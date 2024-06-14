package io.quarkus.hibernate.orm.runtime.recording;

import java.util.Collection;

import org.hibernate.dialect.Dialect;
import org.hibernate.service.internal.ProvidedService;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;
import io.quarkus.hibernate.orm.runtime.IntegrationSettings;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;

public final class BuildTimeRecordedState {
    private final QuarkusPersistenceUnitDescriptor descriptor;
    private final Dialect dialect;
    private final PrevalidatedQuarkusMetadata metadata;
    private final BuildTimeSettings settings;
    private final Collection<ProvidedService<?>> providedServices;
    private final IntegrationSettings integrationSettings;
    private final MultiTenancyStrategy multiTenancyStrategy;

    private final boolean isReactive;
    private final boolean fromPersistenceXml;

    public BuildTimeRecordedState(QuarkusPersistenceUnitDescriptor descriptor,
            Dialect dialect, PrevalidatedQuarkusMetadata metadata,
            BuildTimeSettings settings,
            Collection<ProvidedService<?>> providedServices,
            IntegrationSettings integrationSettings,
            MultiTenancyStrategy strategy,
            boolean isReactive, boolean fromPersistenceXml) {
        this.descriptor = descriptor;
        this.dialect = dialect;
        this.metadata = metadata;
        this.settings = settings;
        this.providedServices = providedServices;
        this.integrationSettings = integrationSettings;
        this.multiTenancyStrategy = strategy;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
    }

    public QuarkusPersistenceUnitDescriptor getDescriptor() {
        return descriptor;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public PrevalidatedQuarkusMetadata getMetadata() {
        return metadata;
    }

    public BuildTimeSettings getSettings() {
        return settings;
    }

    public Collection<ProvidedService<?>> getProvidedServices() {
        return providedServices;
    }

    public IntegrationSettings getIntegrationSettings() {
        return integrationSettings;
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
