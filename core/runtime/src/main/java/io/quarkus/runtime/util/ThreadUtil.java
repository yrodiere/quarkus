package io.quarkus.runtime.util;

import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;

public final class ThreadUtil {
    private static final Field INHERITED_ACCESS_CONTROL_CONTEXT_FIELD;

    static {
        Field field;
        try {
            field = Thread.class.getDeclaredField("inheritedAccessControlContext");
            field.setAccessible(true);
        } catch (NoSuchFieldException | RuntimeException e) {
            field = null;
        }
        INHERITED_ACCESS_CONTROL_CONTEXT_FIELD = field;
    }

    private ThreadUtil() {
    }

    /**
     * Resets the Context ClassLoader of a given thread,
     * along with any internal Thread fields that may reference a different classloader indirectly.
     * <p>
     * Should be used in dev/test mode to reset threads from thread pools on application restarts.
     *
     * @param threadToReset The thread to reset.
     * @param classLoader The classloader to use as the new Context ClassLoader.
     * @param aggressive Whether to also reset indirect references to classloaders with ugly hacks.
     *        Should only be used in dev mode.
     */
    public static void resetClassLoaderReferences(Thread threadToReset, ClassLoader classLoader,
            boolean aggressive) {
        threadToReset.setContextClassLoader(classLoader);

        if (aggressive && INHERITED_ACCESS_CONTROL_CONTEXT_FIELD != null) {
            // The inheritedAccessControlContext may leak classloaders too,
            // so we need to reset it.
            // Unfortunately we're not supposed to do that,
            // so the only way is through this ugly hack.
            try {
                INHERITED_ACCESS_CONTROL_CONTEXT_FIELD.set(threadToReset,
                        // This should be fine as long as the SecurityManager is not used,
                        // and we don't support the SecurityManager at the moment,
                        // especially not in tests/devmode where aggressive resets are used.
                        new AccessControlContext(new ProtectionDomain[0]));
            } catch (IllegalAccessException e) {
                // Ignore: this is best-effort.
            }
        }
    }

}
