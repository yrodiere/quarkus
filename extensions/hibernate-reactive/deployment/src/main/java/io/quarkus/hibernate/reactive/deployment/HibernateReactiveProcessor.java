package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.HibernateConfigUtil.firstPresent;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.loader.BatchFetchStyle;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.hibernate.orm.deployment.Dialects;
import io.quarkus.hibernate.orm.deployment.HibernateConfigUtil;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionFactoryProducer;
import io.quarkus.hibernate.reactive.runtime.ReactiveSessionProducer;
import io.quarkus.reactive.datasource.deployment.VertxPoolBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = HibernateReactiveEnabled.class)
public final class HibernateReactiveProcessor {

    private static final String HIBERNATE_REACTIVE = "Hibernate Reactive";
    private static final Logger LOG = Logger.getLogger(HibernateReactiveProcessor.class);
    static final String[] REFLECTIVE_CONSTRUCTORS_NEEDED = {
            "org.hibernate.reactive.persister.entity.impl.ReactiveSingleTableEntityPersister",
            "org.hibernate.reactive.persister.entity.impl.ReactiveJoinedSubclassEntityPersister",
            "org.hibernate.reactive.persister.entity.impl.ReactiveUnionSubclassEntityPersister",
            "org.hibernate.reactive.persister.collection.impl.ReactiveOneToManyPersister",
            "org.hibernate.reactive.persister.collection.impl.ReactiveBasicCollectionPersister",
    };

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel) {
        if (descriptors.size() == 1) {
            // Only register those beans if their EMF dependency is also available, so use the same guard as the ORM extension
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveSessionFactoryProducer.class));
            additionalBeans.produce(new AdditionalBeanBuildItem(ReactiveSessionProducer.class));
        } else {
            LOG.warnf(
                    "Skipping registration of %s and %s because exactly one persistence unit is required for their registration",
                    ReactiveSessionFactoryProducer.class.getSimpleName(), ReactiveSessionProducer.class.getSimpleName());
        }
    }

    @BuildStep
    void reflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, REFLECTIVE_CONSTRUCTORS_NEEDED));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext,
            HibernateReactiveRecorder recorder,
            JpaModelBuildItem jpaModel) {
        final boolean enableRx = hasEntities(jpaModel);
        recorder.callHibernateReactiveFeatureInit(enableRx);
    }

    @BuildStep
    public void buildReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaModelBuildItem jpaModel,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            List<DefaultDataSourceDbKindBuildItem> defaultDataSourceDbKindBuildItems,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {

        final boolean enableHR = hasEntities(jpaModel);
        if (!enableHR) {
            // we have to bail out early as we might not have a Vertx pool configuration
            LOG.warn("Hibernate Reactive is disabled because no JPA entities were found");
            return;
        }

        // Block any reactive persistence units from using persistence.xml
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            String provider = persistenceXmlDescriptorBuildItem.getDescriptor().getProviderClassName();
            if (provider == null ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.class.getCanonicalName()) ||
                    provider.equals(FastBootHibernateReactivePersistenceProvider.IMPLEMENTATION_NAME)) {
                throw new ConfigurationException(
                        "Cannot use persistence.xml with Hibernate Reactive in Quarkus. Must use application.properties instead.");
            }
        }

        // we only support the default pool for now
        Optional<String> dbKindOptional = DefaultDataSourceDbKindBuildItem.resolve(
                dataSourcesBuildTimeConfig.defaultDataSource.dbKind,
                defaultDataSourceDbKindBuildItems,
                dataSourcesBuildTimeConfig.defaultDataSource.devservices.enabled
                        .orElse(dataSourcesBuildTimeConfig.namedDataSources.isEmpty()),
                curateOutcomeBuildItem);
        if (dbKindOptional.isPresent()) {
            final String dbKind = dbKindOptional.get();
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig = hibernateOrmConfig.defaultPersistenceUnit;
            ParsedPersistenceXmlDescriptor reactivePU = generateReactivePersistenceUnit(
                    hibernateOrmConfig, persistenceUnitConfig, jpaModel,
                    dbKind, applicationArchivesBuildItem, launchMode.getLaunchMode(),
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, dbKindDialectBuildItems);

            //Some constant arguments to the following method:
            // - this is Reactive
            // - we don't support starting Hibernate Reactive from a persistence.xml
            // - we don't support Hibernate Envers with Hibernate Reactive
            persistenceUnitDescriptors.produce(new PersistenceUnitDescriptorBuildItem(reactivePU,
                    jpaModel.getXmlMappings(reactivePU.getName()),
                    persistenceUnitConfig.unsupportedProperties,
                    true, false));
        }

    }

    @BuildStep
    void waitForVertxPool(List<VertxPoolBuildItem> vertxPool,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationRuntimeConfiguredBuildItem> runtimeConfigured) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            // Define a dependency on VertxPoolBuildItem to ensure that any Pool instances are available
            // when HibernateORM starts its persistence units
            runtimeConfigured.produce(new HibernateOrmIntegrationRuntimeConfiguredBuildItem(HIBERNATE_REACTIVE,
                    puDescriptor.getPersistenceUnitName()));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    PersistenceProviderSetUpBuildItem setUpPersistenceProviderAndWaitForVertxPool(HibernateReactiveRecorder recorder,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationBuildItems,
            BuildProducer<RecorderBeanInitializedBuildItem> orderEnforcer) {
        recorder.initializePersistenceProvider(hibernateOrmRuntimeConfig,
                HibernateOrmIntegrationRuntimeConfiguredBuildItem.collectDescriptors(integrationBuildItems));
        return new PersistenceProviderSetUpBuildItem();
    }

    /**
     * This is mostly copied from
     * io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor#handleHibernateORMWithNoPersistenceXml
     * Key differences are:
     * - Always produces a persistence unit descriptor, since we assume there always 1 reactive persistence unit
     * - Any JDBC-only configuration settings are removed
     * - If we ever add any Reactive-only config settings, they can be set here
     */
    private static ParsedPersistenceXmlDescriptor generateReactivePersistenceUnit(
            HibernateOrmConfig hibernateOrmConfig,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            JpaModelBuildItem jpaModel,
            String dbKind,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<DatabaseKindDialectBuildItem> dbKindDialectBuildItems) {
        //we have no persistence.xml so we will create a default one
        String persistenceUnitConfigName = PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

        Optional<String> explicitDialect = persistenceUnitConfig.dialect.dialect;
        String dialect;
        if (explicitDialect.isPresent()) {
            dialect = explicitDialect.get();
        } else {
            dialect = Dialects.guessDialect(persistenceUnitConfigName, dbKind, dbKindDialectBuildItems);
        }

        // we found one
        ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
        desc.setName("default-reactive");
        desc.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
        desc.getProperties().setProperty(AvailableSettings.DIALECT, dialect);
        desc.setExcludeUnlistedClasses(true);
        desc.addClasses(new ArrayList<>(jpaModel.getAllModelClassNames()));

        // The storage engine has to be set as a system property.
        if (persistenceUnitConfig.dialect.storageEngine.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE,
                    persistenceUnitConfig.dialect.storageEngine.get()));
        }
        // Physical Naming Strategy
        persistenceUnitConfig.physicalNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

        // Implicit Naming Strategy
        persistenceUnitConfig.implicitNamingStrategy.ifPresent(
                namingStrategy -> desc.getProperties()
                        .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

        //charset
        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                persistenceUnitConfig.database.charset.name());

        if (persistenceUnitConfig.database.globallyQuotedIdentifiers) {
            desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }

        // Query
        // TODO ideally we should align on ORM and use 16 as a default, but that would break applications
        //   because of https://github.com/hibernate/hibernate-reactive/issues/742
        int batchSize = firstPresent(persistenceUnitConfig.fetch.batchSize, persistenceUnitConfig.batchFetchSize)
                .orElse(-1);
        if (batchSize > 0) {
            desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                    Integer.toString(batchSize));
            desc.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
        }

        if (persistenceUnitConfig.fetch.maxDepth.isPresent()) {
            setMaxFetchDepth(desc, persistenceUnitConfig.fetch.maxDepth);
        } else if (persistenceUnitConfig.maxFetchDepth.isPresent()) {
            setMaxFetchDepth(desc, persistenceUnitConfig.maxFetchDepth);
        }

        desc.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, Integer.toString(
                persistenceUnitConfig.query.queryPlanCacheMaxSize));

        desc.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                persistenceUnitConfig.query.defaultNullOrdering.name().toLowerCase());

        // JDBC
        persistenceUnitConfig.jdbc.statementBatchSize.ifPresent(
                statementBatchSize -> desc.getProperties().setProperty(AvailableSettings.STATEMENT_BATCH_SIZE,
                        String.valueOf(statementBatchSize)));

        // Statistics
        if (hibernateOrmConfig.metricsEnabled
                || (hibernateOrmConfig.statistics.isPresent() && hibernateOrmConfig.statistics.get())) {
            desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
        }

        // sql-load-script
        List<String> importFiles = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript, launchMode);

        if (!importFiles.isEmpty()) {
            for (String importFile : importFiles) {
                Path loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile);

                if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                    // enlist resource if present
                    nativeImageResources.produce(new NativeImageResourceBuildItem(importFile));
                    hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile));
                } else if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                    //raise exception if explicit file is not present (i.e. not the default)
                    String propertyName = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitConfigName, "sql-load-script");
                    throw new ConfigurationException(
                            "Unable to find file referenced in '"
                                    + propertyName + "="
                                    + String.join(",", persistenceUnitConfig.sqlLoadScript.get())
                                    + "'. Remove property or add file to your path.",
                            Collections.singleton(propertyName));
                }
            }

            if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, String.join(",", importFiles));
            }
        } else {
            //Disable implicit loading of the default import script (import.sql)
            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, "");
        }

        // Caching
        if (persistenceUnitConfig.secondLevelCachingEnabled) {
            Properties p = desc.getProperties();
            //Only set these if the user isn't making an explicit choice:
            p.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(persistenceUnitConfig);
            for (Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                desc.getProperties().setProperty(entry.getKey(), entry.getValue());
            }
        } else {
            //Unless the global switch is explicitly set to off, in which case we disable all caching:
            Properties p = desc.getProperties();
            p.put(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
            p.put(USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
            p.put(USE_QUERY_CACHE, Boolean.FALSE);
            p.put(JPA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
        }

        return desc;
    }

    private static void setMaxFetchDepth(ParsedPersistenceXmlDescriptor descriptor, OptionalInt maxFetchDepth) {
        descriptor.getProperties().setProperty(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(maxFetchDepth.getAsInt()));
    }

    private static List<String> getSqlLoadScript(Optional<List<String>> sqlLoadScript, LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (sqlLoadScript.isPresent()) {
            return sqlLoadScript.get().stream()
                    .filter(s -> !HibernateOrmProcessor.NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(s))
                    .collect(Collectors.toList());
        } else if (launchMode == LaunchMode.NORMAL) {
            return Collections.emptyList();
        } else {
            return List.of("import.sql");
        }
    }

    private boolean hasEntities(JpaModelBuildItem jpaModel) {
        return !jpaModel.getEntityClassNames().isEmpty();
    }

}
