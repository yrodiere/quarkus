package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.dialect.Dialect;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.internal.ProvidedService;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;

public final class StaticInitRecordedState {

    public static StaticInitRecordedState create(BuildTimeRecordedState buildTimeState,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies preGeneratedProxies) {
        return new StaticInitRecordedState(
                buildTimeState.getDialect(),
                buildTimeState.getMetadata(),
                buildTimeState.getSettings(),
                createIntegrators(additionalIntegrators),
                buildTimeState.getProvidedServices(),
                buildTimeState.getIntegrationSettings(),
                ProxyDefinitions.createFromMetadata(buildTimeState.getMetadata(), preGeneratedProxies),
                buildTimeState.getMultiTenancyStrategy(),
                buildTimeState.isReactive(), buildTimeState.isFromPersistenceXml());
    }

    private static Collection<Integrator> createIntegrators(Collection<Class<? extends Integrator>> integratorClasses) {
        LinkedHashSet<Integrator> integrators = new LinkedHashSet<>();
        integrators.add(new BeanValidationIntegrator());
        integrators.add(new CollectionCacheInvalidator());

        for (Class<? extends Integrator> integratorClass : integratorClasses) {
            try {
                integrators.add(integratorClass.getConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to instantiate integrator " + integratorClass, e);
            }
        }

        return integrators;
    }

    private final Dialect dialect;
    private final PrevalidatedQuarkusMetadata metadata;
    private final BuildTimeSettings settings;
    private final Collection<Integrator> integrators;
    private final Collection<ProvidedService<?>> providedServices;
    private final IntegrationSettings integrationSettings;
    private final ProxyDefinitions proxyClassDefinitions;
    private final MultiTenancyStrategy multiTenancyStrategy;

    private final boolean isReactive;
    private final boolean fromPersistenceXml;

    private StaticInitRecordedState(Dialect dialect, PrevalidatedQuarkusMetadata metadata,
            BuildTimeSettings settings, Collection<Integrator> integrators,
            Collection<ProvidedService<?>> providedServices, IntegrationSettings integrationSettings,
            ProxyDefinitions classDefinitions, MultiTenancyStrategy strategy,
            boolean isReactive, boolean fromPersistenceXml) {
        this.dialect = dialect;
        this.metadata = metadata;
        this.settings = settings;
        this.integrators = integrators;
        this.providedServices = providedServices;
        this.integrationSettings = integrationSettings;
        this.proxyClassDefinitions = classDefinitions;
        this.multiTenancyStrategy = strategy;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public PrevalidatedQuarkusMetadata getMetadata() {
        return metadata;
    }

    public BuildTimeSettings getBuildTimeSettings() {
        return settings;
    }

    public Collection<Integrator> getIntegrators() {
        return integrators;
    }

    public Collection<ProvidedService<?>> getProvidedServices() {
        return providedServices;
    }

    public IntegrationSettings getIntegrationSettings() {
        return integrationSettings;
    }

    public ProxyDefinitions getProxyClassDefinitions() {
        return proxyClassDefinitions;
    }

    public MultiTenancyStrategy getMultiTenancyStrategy() {
        return multiTenancyStrategy;
    }

    public boolean isReactive() {
        return isReactive;
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }
}
