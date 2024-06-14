package io.quarkus.hibernate.orm.deployment.boot;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelector;

import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.service.InitialInitiatorListProvider;
import io.quarkus.hibernate.orm.deployment.service.StandardHibernateORMInitiatorListProvider;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusIntegratorServiceImpl;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusStrategySelectorBuilder;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;

final class RecordableBootstrapFactory {

    private static final InitialInitiatorListProvider reactiveInitiatorListProvider = initReactiveListProviderMaybe();
    private static final InitialInitiatorListProvider classicInitiatorListProvider = new StandardHibernateORMInitiatorListProvider();

    public static RecordableBootstrap createRecordableBootstrapBuilder(PersistenceUnitDescriptorBuildItem pu) {
        final BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry();
        final RecordableBootstrap ssrBuilder = new RecordableBootstrap(bsr, getInitiatorListProvider(pu));
        return ssrBuilder;
    }

    private static InitialInitiatorListProvider getInitiatorListProvider(PersistenceUnitDescriptorBuildItem pu) {
        if (pu.isReactive()) {
            if (reactiveInitiatorListProvider == null) {
                throw new IllegalStateException(
                        "InitiatorList requires for Hibernate Reactive but Hibernate Reactive extension is not around?");
            }
            return reactiveInitiatorListProvider;
        } else {
            return classicInitiatorListProvider;
        }
    }

    private static BootstrapServiceRegistry buildBootstrapServiceRegistry() {
        final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;
        // N.B. support for custom IntegratorProvider injected via Properties (as
        // instance) removed

        final QuarkusIntegratorServiceImpl integratorService = new QuarkusIntegratorServiceImpl(providedClassLoaderService);
        final StrategySelector strategySelector = QuarkusStrategySelectorBuilder.buildSelector(providedClassLoaderService);
        return new BootstrapServiceRegistryImpl(true, providedClassLoaderService, strategySelector, integratorService);
    }

    private static InitialInitiatorListProvider initReactiveListProviderMaybe() {
        try {
            //Use reflection as we don't want the Hibernate ORM extension to depend on the Hibernate Reactive extension:
            final Class<?> forName = Class
                    .forName("io.quarkus.hibernate.reactive.deployment.boot.registry.ReactiveHibernateInitiatorListProvider");
            final Object o = forName.getDeclaredConstructor().newInstance();
            return (InitialInitiatorListProvider) o;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
            //Be silent as this is a static initializer: most likely the Hibernate Reactive extension is
            //not on the classpath, which implies we won't need this.
            return null;
        }
    }

}
