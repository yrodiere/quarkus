package io.quarkus.hibernate.orm.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.hibernate.bytecode.enhance.internal.bytebuddy.CoreTypePool;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.ModelTypePool;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.QuarkusClassVisitor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusClassFileLocator;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusEnhancementContext;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.ClassFileLocator;

/**
 * Used to transform bytecode by registering to
 * io.quarkus.deployment.ProcessorContext#addByteCodeTransformer(java.util.function.Function).
 * This function adapts the Quarkus bytecode transformer API - which uses ASM - to use the Entity Enhancement API of
 * Hibernate ORM, which exposes a simple byte array.
 *
 * N.B. For enhancement the hardcoded tool of choice is the Byte Buddy based enhancer.
 * This is not configurable, and we enforce the ORM environment to use the "noop" enhancer as we require all
 * entities to be enhanced at build time.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final Logger LOGGER = Logger.getLogger(HibernateEntityEnhancer.class);

    private static final BytecodeProviderImpl PROVIDER = new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl(
            ClassFileVersion.JAVA_V17);

    //Choose this set to include Jakarta annotations, basic Java types such as String and Map, Hibernate annotations, and Panache supertypes:
    private static final CoreTypePool CORE_POOL = new CoreTypePool("jakarta.persistence.", "java.",
            "org.hibernate.annotations.",
            "io.quarkus.hibernate.reactive.panache.", "io.quarkus.hibernate.orm.panache.",
            "org.hibernate.search.mapper.pojo.mapping.definition.annotation.",
            "jakarta.validation.constraints.");

    private final EnhancerHolder enhancerHolder = new EnhancerHolder();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor, enhancerHolder);
    }

    private static class HibernateEnhancingClassVisitor extends QuarkusClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private final EnhancerHolder enhancerHolder;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                EnhancerHolder enhancerHolder) {
            //Careful: the ASM API version needs to match the ASM version of Gizmo, not the one from Byte Buddy.
            //Most often these match - but occasionally they will diverge which is acceptable as Byte Buddy is shading ASM.
            super(Gizmo.ASM_API_VERSION, new QuarkusClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            this.enhancerHolder = enhancerHolder;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = hibernateEnhancement(className, inputBytes);
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, super.getOriginalClassReaderOptions());
        }

        private byte[] hibernateEnhancement(final String className, final byte[] originalBytes) {
            final byte[] enhanced = doEnhance(enhancerHolder, className, originalBytes);
            return enhanced == null ? originalBytes : enhanced;
        }

    }

    public byte[] enhance(String className, byte[] originalBytes) {
        return doEnhance(enhancerHolder, className, originalBytes);
    }

    private static byte[] doEnhance(EnhancerHolder enhancerHolder, String className, byte[] originalBytes) {
        String transformedClassesDir = BootstrapDebug.transformedClassesDir();
        if (transformedClassesDir != null) {
            try {
                Path dumpPath = Path.of(transformedClassesDir, "hibernate-enhancer-input",
                        className.replace(".", File.separator) + ".class");
                Files.createDirectories(dumpPath.getParent());
                Files.write(dumpPath, originalBytes, StandardOpenOption.CREATE);
                LOGGER.infof("Wrote a copy of the Hibernate Enhancer input to %s", dumpPath);
            } catch (IOException e) {
                LOGGER.errorf(e, "Failed to write a copy of the Hibernate Enhancer input for class '%s': %s",
                        className, e.getMessage());
            }
        }
        return enhancerHolder.getEnhancer().enhance(className, originalBytes);
    }

    private static class EnhancerHolder {

        private volatile Enhancer actualEnhancer;

        public Enhancer getEnhancer() {
            //Lazily initialized as it's expensive and might not be necessary: these transformations are cacheable.
            if (actualEnhancer == null) {
                synchronized (this) {
                    if (actualEnhancer == null) {
                        actualEnhancer = PROVIDER.getEnhancer(QuarkusEnhancementContext.INSTANCE, new ThreadsafeLocator());
                    }
                }
            }
            return actualEnhancer;
        }
    }

    private static final class ThreadsafeLocator implements EnhancerClassLocator {

        final ThreadLocal<EnhancerClassLocator> localLocator = ThreadLocal
                .withInitial(() -> ModelTypePool.buildModelTypePool(QuarkusClassFileLocator.INSTANCE,
                        CORE_POOL));
        final AtomicBoolean abstractDone = new AtomicBoolean();
        final AtomicBoolean concreteDone = new AtomicBoolean();
        final AtomicBoolean mappedStarted = new AtomicBoolean();

        @Override
        public void registerClassNameAndBytes(String s, byte[] bytes) {
            // HACK: to reproduce the problem reliably.
            switch (s) {
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyConcreteEntity":
                    waitUntil(() -> mappedStarted.get());
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyMappedSuperclass":
                    waitUntil(() -> abstractDone.get());
                    break;
                default:
                    break;
            }
            // END HACK
            localLocator.get().registerClassNameAndBytes(s, bytes);
            // HACK: to reproduce the problem reliably.
            switch (s) {
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyMappedSuperclass":
                    mappedStarted.set(true);
                    waitUntil(() -> abstractDone.get() && concreteDone.get());
                    break;
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyAbstractEntity":
                    break;
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyConcreteEntity":
                    waitUntil(() -> abstractDone.get());
                    break;
                default:
                    break;
            }
            // END HACK
        }

        @Override
        public void deregisterClassNameAndBytes(String s) {
            // HACK: to reproduce the problem reliably.
            switch (s) {
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyAbstractEntity":
                    abstractDone.set(true);
                    break;
                case "io.quarkus.hibernate.orm.applicationfieldaccess.PublicFieldAccessInheritanceTest$MyConcreteEntity":
                    concreteDone.set(true);
                    break;
                default:
                    break;
            }
            // END HACK
            localLocator.get().deregisterClassNameAndBytes(s);
        }

        private void waitUntil(BooleanSupplier condition) {
            try {
                while (!condition.getAsBoolean()) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ClassFileLocator asClassFileLocator() {
            return localLocator.get().asClassFileLocator();
        }

        @Override
        public Resolution describe(String s) {
            return localLocator.get().describe(s);
        }

        @Override
        public void clear() {
            //not essential as it gets discarded, but could help:
            localLocator.get().clear();
            localLocator.remove();
        }
    }

}
