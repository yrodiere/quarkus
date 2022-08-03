package io.quarkus.hibernate.envers;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.EnversSettings;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;

@Recorder
public class HibernateEnversRecorder {

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
        return new HibernateEnversIntegrationStaticInitListener(buildTimeConfig);
    }

    private static final class HibernateEnversIntegrationStaticInitListener
            implements HibernateOrmIntegrationStaticInitListener {
        private final HibernateEnversBuildTimeConfig buildTimeConfig;

        private HibernateEnversIntegrationStaticInitListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, EnversSettings.STORE_DATA_AT_DELETE, buildTimeConfig.storeDataAtDelete);
            addConfig(propertyCollector, EnversSettings.AUDIT_TABLE_SUFFIX, buildTimeConfig.auditTableSuffix);
            addConfig(propertyCollector, EnversSettings.AUDIT_TABLE_PREFIX, buildTimeConfig.auditTablePrefix);
            addConfig(propertyCollector, EnversSettings.REVISION_FIELD_NAME, buildTimeConfig.revisionFieldName);
            addConfig(propertyCollector, EnversSettings.REVISION_TYPE_FIELD_NAME, buildTimeConfig.revisionTypeFieldName);
            addConfig(propertyCollector, EnversSettings.REVISION_ON_COLLECTION_CHANGE,
                    buildTimeConfig.revisionOnCollectionChange);
            addConfig(propertyCollector, EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD,
                    buildTimeConfig.doNotAuditOptimisticLockingField);
            addConfig(propertyCollector, EnversSettings.DEFAULT_SCHEMA, buildTimeConfig.defaultSchema);
            addConfig(propertyCollector, EnversSettings.DEFAULT_CATALOG, buildTimeConfig.defaultCatalog);
            addConfig(propertyCollector, EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION,
                    buildTimeConfig.trackEntitiesChangedInRevision);
            addConfig(propertyCollector, EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID,
                    buildTimeConfig.useRevisionEntityWithNativeId);
            addConfig(propertyCollector, EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, buildTimeConfig.globalWithModifiedFlag);
            addConfig(propertyCollector, EnversSettings.MODIFIED_FLAG_SUFFIX, buildTimeConfig.modifiedFlagSuffix);
            addConfigIfPresent(propertyCollector, EnversSettings.REVISION_LISTENER, buildTimeConfig.revisionListener);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY, buildTimeConfig.auditStrategy);
            addConfigIfPresent(propertyCollector, EnversSettings.ORIGINAL_ID_PROP_NAME, buildTimeConfig.originalIdPropName);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME,
                    buildTimeConfig.auditStrategyValidityEndRevFieldName);
            addConfig(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP,
                    buildTimeConfig.auditStrategyValidityStoreRevendTimestamp);
            addConfigIfPresent(propertyCollector, EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME,
                    buildTimeConfig.auditStrategyValidityRevendTimestampFieldName);
            addConfigIfPresent(propertyCollector, EnversSettings.EMBEDDABLE_SET_ORDINAL_FIELD_NAME,
                    buildTimeConfig.embeddableSetOrdinalFieldName);
            addConfig(propertyCollector, EnversSettings.ALLOW_IDENTIFIER_REUSE, buildTimeConfig.allowIdentifierReuse);
            addConfigIfPresent(propertyCollector, EnversSettings.MODIFIED_COLUMN_NAMING_STRATEGY,
                    buildTimeConfig.modifiedColumnNamingStrategy);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
            propertyCollector.accept(configPath, value);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, Optional<T> value) {
            if (value.isPresent()) {
                propertyCollector.accept(configPath, value.get());
            } else {
                propertyCollector.accept(configPath, "");
            }
        }

        public static <T> void addConfigIfPresent(BiConsumer<String, Object> propertyCollector, String configPath,
                Optional<T> value) {
            value.ifPresent(t -> propertyCollector.accept(configPath, t));
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }

    public HibernateOrmIntegrationStaticInitListener createStaticInitInactiveListener() {
        return new HibernateEnversIntegrationStaticInitInactiveListener();
    }

    private static final class HibernateEnversIntegrationStaticInitInactiveListener
            implements HibernateOrmIntegrationStaticInitListener {
        private HibernateEnversIntegrationStaticInitInactiveListener() {
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            propertyCollector.accept(EnversService.INTEGRATION_ENABLED, "false");
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitListener(
            HibernateEnversRuntimeConfig runtimeConfig, String persistenceUnitName) {
        HibernateEnversRuntimeConfigPersistenceUnit puConfig = runtimeConfig.getAllPersistenceUnitConfigsAsMap()
                .get(persistenceUnitName);
        return new HibernateEnversIntegrationRuntimeInitListener(puConfig);
    }

    private static final class HibernateEnversIntegrationRuntimeInitListener
            implements HibernateOrmIntegrationRuntimeInitListener {
        private final HibernateEnversRuntimeConfigPersistenceUnit runtimeConfig;

        private HibernateEnversIntegrationRuntimeInitListener(HibernateEnversRuntimeConfigPersistenceUnit runtimeConfig) {
            this.runtimeConfig = runtimeConfig;
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            if (runtimeConfig != null) {
                if (runtimeConfig.active.isPresent() && !runtimeConfig.active.get()) {
                    propertyCollector.accept(EnversService.INTEGRATION_ENABLED, "false");
                    // Do not process other properties: Hibernate Envers is inactive anyway.
                    return;
                }

                // In the future, add support for other runtime configuration properties here.
            }
        }
    }

    public HibernateOrmIntegrationRuntimeInitListener createRuntimeInitInactiveListener() {
        return new HibernateEnversIntegrationRuntimeInitInactiveListener();
    }

    private static final class HibernateEnversIntegrationRuntimeInitInactiveListener
            implements HibernateOrmIntegrationRuntimeInitListener {
        private HibernateEnversIntegrationRuntimeInitInactiveListener() {
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            // Not strictly necessary since this should be set during static init,
            // but let's be on the safe side.
            propertyCollector.accept(EnversService.INTEGRATION_ENABLED, "false");
        }
    }

    public void checkNoExplicitActiveTrue(HibernateEnversRuntimeConfig runtimeConfig) {
        for (var entry : runtimeConfig.getAllPersistenceUnitConfigsAsMap().entrySet()) {
            var config = entry.getValue();
            if (config.active.isPresent() && config.active.get()) {
                var puName = entry.getKey();
                String enabledPropertyKey = HibernateEnversRuntimeConfig.extensionPropertyKey("enabled");
                String activePropertyKey = HibernateEnversRuntimeConfig.persistenceUnitPropertyKey(puName, "active");
                throw new ConfigurationException(
                        "Hibernate Envers activated explicitly for persistence unit '" + puName
                                + "', but the Hibernate Envers extension was disabled at build time."
                                + " If you want Hibernate Envers to be active for this persistence unit, you must set '"
                                + enabledPropertyKey
                                + "' to 'true' at build time."
                                + " If you don't want Hibernate Envers to be active for this persistence unit, you must leave '"
                                + activePropertyKey
                                + "' unset or set it to 'false'.",
                        Set.of(enabledPropertyKey, activePropertyKey));
            }
        }
    }
}
