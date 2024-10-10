package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionConfigActiveFalseDefaultDatasourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.active", "false");

    @Inject
    MyBean myBean;

    @Test
    @DisplayName("If the default datasource is deactivated, the application should boot, but Flyway should be deactivated for that datasource")
    public void testBootSucceedsButFlywayDeactivated() {
        assertThatThrownBy(myBean::useFlyway)
                .cause()
                .hasMessageContainingAll("Unable to find datasource '<default>' for Flyway",
                        "Datasource '<default>' was deactivated through configuration properties.",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                + " to 'true' and configure datasource '<default>'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Flyway flyway;

        public void useFlyway() {
            flyway.getConfiguration();
        }
    }
}
