package io.quarkus.runner.bootstrap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.runtime.util.ThreadUtil;

public class ForkJoinClassLoading {

    private static final Logger log = Logger.getLogger(ForkJoinClassLoading.class.getName());

    /**
     * A yucky hack, basically attempt to make sure every thread in the common pool has
     * the correct CL.
     *
     * It's not perfect, but as this only affects test and dev mode and not production it is better
     * than nothing.
     *
     * Really we should just not use the common pool at all.
     *
     * @param classLoader The classloader to use as the new Context ClassLoader for ForkJoinPool threads.
     * @param mode The bootstrap mode of the application being booted / shut down.
     */
    public static void resetClassLoaderReferences(ClassLoader classLoader, QuarkusBootstrap.Mode mode) {
        boolean aggressive = mode != QuarkusBootstrap.Mode.PROD;

        CountDownLatch allDone = new CountDownLatch(ForkJoinPool.getCommonPoolParallelism());
        CountDownLatch taskRelease = new CountDownLatch(1);
        for (int i = 0; i < ForkJoinPool.getCommonPoolParallelism(); ++i) {
            ForkJoinPool.commonPool().execute(new Runnable() {
                @Override
                public void run() {
                    ThreadUtil.resetClassLoaderReferences(Thread.currentThread(), classLoader, aggressive);
                    allDone.countDown();
                    try {
                        taskRelease.await();
                    } catch (InterruptedException e) {
                        log.error("Failed to set fork join ClassLoader", e);
                    }
                }
            });
        }
        try {
            if (!allDone.await(1, TimeUnit.SECONDS)) {
                log.error(
                        "Timed out trying to set fork join ClassLoader, this should never happen unless something has tied up a fork join thread before the app launched");
            }
        } catch (InterruptedException e) {
            log.error("Failed to set fork join ClassLoader", e);
        } finally {
            taskRelease.countDown();
        }
    }
}
