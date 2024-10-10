package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests a use case where multiple datasources are defined at build time,
 * but only one is used at runtime.
 * <p>
 * This is mostly useful when each datasource has a distinct db-kind, but in theory that shouldn't matter,
 * so we use the h2 db-kind everywhere here to keep test dependencies simpler.
 * <p>
 * See {@link MultipleDataSourcesAsAlternativesWithActiveDS1Test} for the counterpart where PU2 is used at runtime.
 */
public class MultipleDataSourcesAsAlternativesWithActiveDS1Test {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyProducer.class))
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
            .overrideConfigKey("quarkus.datasource.ds-2.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.ds-2.active", "false")
            // This is where we select datasource 1
            .overrideRuntimeConfigKey("quarkus.datasource.ds-1.active", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.ds-1.jdbc.url", "jdbc:h2:mem:testds1");

    @Inject
    @DataSource("ds-1")
    AgroalDataSource explicitDatasourceBean;

    @Inject
    AgroalDataSource customIndirectDatasourceBean;

    @Test
    public void testExplicitDatasourceBeanUsable() {
        doTestDatasource(explicitDatasourceBean);
    }

    @Test
    public void testCustomIndirectDatasourceBeanUsable() {
        doTestDatasource(customIndirectDatasourceBean);
    }

    @Test
    public void testInactiveDatasourceBeanUnusable() {
        assertThatThrownBy(() -> Arc.container().select(AgroalDataSource.class, new DataSource.DataSourceLiteral("ds-2")).get()
                .getConnection())
                .hasMessageContaining("Datasource 'ds-2' was deactivated through configuration properties.");
    }

    private static void doTestDatasource(AgroalDataSource dataSource) {
        assertThatCode(() -> {
            try (var connection = dataSource.getConnection()) {
            }
        })
                .doesNotThrowAnyException();
    }

    private static class MyProducer {
        @Any
        InjectableInstance<AgroalDataSource> dataSources;

        @Produces
        @ApplicationScoped
        public AgroalDataSource dataSource() {
            List<AgroalDataSource> list = listActive(dataSources);
            if (list.size() != 1) {
                throw new IllegalStateException("Number of active datasources should be 1, was " + list.size() + " instead");
            }
            return list.get(0);
        }

        static <T> List<T> listActive(InjectableInstance<T> instance) {
            return instance.handlesStream().filter(h -> h.getBean().isActive()).map(io.quarkus.arc.InstanceHandle::get)
                    .toList();
        }
    }
}
