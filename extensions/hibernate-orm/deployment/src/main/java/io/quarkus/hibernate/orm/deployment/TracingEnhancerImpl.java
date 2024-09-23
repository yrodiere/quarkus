package io.quarkus.hibernate.orm.deployment;

import static net.bytebuddy.matcher.ElementMatchers.isDefaultFinalizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImplConstants;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.jboss.logging.Logger;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

public class TracingEnhancerImpl extends EnhancerImpl {
    private static final Logger LOGGER = Logger.getLogger(EnhancerImpl.class);

    public static Object getSuperPrivateField(Object self, String fieldName) {
        return getPrivateField(self, self.getClass().getSuperclass(), fieldName);
    }

    public static Object getPrivateField(Object self, Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(self);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeSuperPrivateMethod(Object self, String methodName,
            List<Class<?>> paramTypes,
            List<?> args) {
        return invokePrivateMethod(self, self.getClass().getSuperclass(), methodName, paramTypes, args);
    }

    public static Object invokePrivateMethod(Object self, Class<?> clazz, String methodName,
            List<Class<?>> paramTypes,
            List<?> args) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes.toArray(Class[]::new));
            method.setAccessible(true);
            return method.invoke(self, args.toArray(Object[]::new));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Provider extends BytecodeProviderImpl {
        public Provider(ClassFileVersion targetCompatibleJVM) {
            super(targetCompatibleJVM);
        }

        @Override
        public Enhancer getEnhancer(EnhancementContext enhancementContext, EnhancerClassLocator classLocator) {
            return new TracingEnhancerImpl(enhancementContext,
                    (ByteBuddyState) getSuperPrivateField(this, "byteBuddyState"),
                    classLocator);
        }
    }

    private final ByteBuddyState byteBuddyState;
    private final EnhancerClassLocator typePool;
    private final EnhancerImplConstants constants;

    public TracingEnhancerImpl(EnhancementContext enhancementContext, ByteBuddyState byteBuddyState,
            EnhancerClassLocator classLocator) {
        super(enhancementContext, byteBuddyState, classLocator);
        this.byteBuddyState = (ByteBuddyState) getSuperPrivateField(this, "byteBuddyState");
        this.typePool = (EnhancerClassLocator) getSuperPrivateField(this, "typePool");
        this.constants = (EnhancerImplConstants) getSuperPrivateField(this, "constants");
    }

    @Override
    public byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
        //Classpool#describe does not accept '/' in the description name as it expects a class name. See HHH-12545
        final String safeClassName = className.replace('/', '.');
        LOGGER.tracef("For '%s': enhance() -> before typePool.registerClassNameAndBytes()", safeClassName);
        typePool.registerClassNameAndBytes(safeClassName, originalBytes);
        LOGGER.tracef("For '%s': enhance() -> after typePool.registerClassNameAndBytes()", safeClassName);
        try {
            LOGGER.tracef("For '%s': enhance() -> before typePool.describe()", safeClassName);
            final TypeDescription typeDescription = typePool.describe(safeClassName).resolve();
            LOGGER.tracef("For '%s': enhance() -> after typePool.describe()", safeClassName);

            LOGGER.tracef("For '%s': enhance() -> before rewrite()", safeClassName);
            return byteBuddyState.rewrite(typePool, safeClassName, byteBuddy -> doEnhance(
                    () -> byteBuddy.ignore(isDefaultFinalizer())
                            .redefine(typeDescription, typePool.asClassFileLocator())
                            .annotateType(
                                    (List<? extends Annotation>) getPrivateField(constants,
                                            EnhancerImplConstants.class, "HIBERNATE_VERSION_ANNOTATION")),
                    typeDescription));
        } catch (EnhancementException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EnhancementException("Failed to enhance class " + className, e);
        } finally {
            LOGGER.tracef("For '%s': enhance() -> after rewrite()", safeClassName);
            LOGGER.tracef("For '%s': enhance() -> before deregister()", safeClassName);
            typePool.deregisterClassNameAndBytes(safeClassName);
            LOGGER.tracef("For '%s': enhance() -> after deregister()", safeClassName);
        }
    }

    private DynamicType.Builder<?> doEnhance(Supplier<DynamicType.Builder<?>> builderSupplier, TypeDescription managedCtClass) {
        return (DynamicType.Builder<?>) invokeSuperPrivateMethod(this, "doEnhance",
                List.of(Supplier.class, TypeDescription.class),
                List.of(builderSupplier, managedCtClass));
    }
}