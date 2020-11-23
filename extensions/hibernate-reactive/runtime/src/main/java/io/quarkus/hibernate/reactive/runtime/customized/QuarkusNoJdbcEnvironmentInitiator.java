package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.reactive.provider.service.NoJdbcEnvironmentInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusNoJdbcEnvironmentInitiator extends NoJdbcEnvironmentInitiator {

    private final Dialect dialect;

    public QuarkusNoJdbcEnvironmentInitiator(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Class<JdbcEnvironment> getServiceInitiated() {
        return JdbcEnvironment.class;
    }

    @Override
    public JdbcEnvironment initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        if (canUseJdbcMetadata(configurationValues, registry)) {
            // Delegate to JdbcEnvironmentInitiator,
            // so that it can optionally retrieve database/driver metadata from the JDBC driver.
            return JdbcEnvironmentInitiator.INSTANCE.initiateService(configurationValues, registry);
        } else {
            return new JdbcEnvironmentImpl(registry, dialect);
        }
    }

    // TODO We wouldn't need this code duplication if we could simply disable
    //  dialect resolution in the parent class. One idea would be to move dialect resolution
    //  to DialectFactory instead of implementing it in NoJdbcEnvironmentInitiator;
    //  then we could just override the DialectFactory in Quarkus.
    private boolean canUseJdbcMetadata(Map configurationValues, ServiceRegistryImplementor registry) {
        boolean useJdbcMetadata = ConfigurationHelper.getBoolean("hibernate.temp.use_jdbc_metadata_defaults",
                configurationValues, true);
        if (!useJdbcMetadata) {
            return false;
        }
        ConnectionProvider connectionProvider = registry.getService(ConnectionProvider.class);
        return connectionProvider instanceof DriverManagerConnectionProviderImpl;
    }

}
