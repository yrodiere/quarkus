package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.hibernate.orm.deployment.boot.xml.QuarkusXmlMapping;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;

/**
 * Not to be confused with PersistenceXmlDescriptorBuildItem, which holds
 * items of the same type.
 * This build item represents a later phase, and might include the implicit
 * configuration definitions that are automatically defined by Quarkus.
 */
public final class PersistenceUnitDescriptorBuildItem extends MultiBuildItem {

    private final QuarkusPersistenceUnitDescriptor descriptor;

    private final RecordedConfig config;
    private final String multiTenancySchemaDataSource;
    private final List<QuarkusXmlMapping> xmlMappings;
    private final boolean isReactive;
    private final boolean fromPersistenceXml;

    public PersistenceUnitDescriptorBuildItem(QuarkusPersistenceUnitDescriptor descriptor,
            RecordedConfig config,
            String multiTenancySchemaDataSource,
            List<QuarkusXmlMapping> xmlMappings,
            boolean isReactive, boolean fromPersistenceXml, Capabilities capabilities) {
        this.descriptor = descriptor;
        this.config = config;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
        this.xmlMappings = xmlMappings;
        this.isReactive = isReactive;
        this.fromPersistenceXml = fromPersistenceXml;
    }

    public QuarkusPersistenceUnitDescriptor getDescriptor() {
        return descriptor;
    }

    public Collection<String> getManagedClassNames() {
        return descriptor.getManagedClassNames();
    }

    public String getExplicitSqlImportScriptResourceName() {
        return descriptor.getProperties().getProperty("jakarta.persistence.sql-load-script-source");
    }

    public String getPersistenceUnitName() {
        return descriptor.getName();
    }

    public String getConfigurationName() {
        return descriptor.getConfigurationName();
    }

    public RecordedConfig getConfig() {
        return config;
    }

    public String getMultiTenancySchemaDataSource() {
        return multiTenancySchemaDataSource;
    }

    public boolean hasXmlMappings() {
        return !xmlMappings.isEmpty();
    }

    public List<QuarkusXmlMapping> getXmlMappings() {
        return xmlMappings;
    }

    public boolean isReactive() {
        return isReactive;
    }

    public boolean isFromPersistenceXml() {
        return fromPersistenceXml;
    }
}
