package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Multiple reactive SQL clients on the classpath, no explicit db-kind:
 * the datasource cannot be created because the default db-kind is ambiguous.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/51268">#51268</a>.
 */
public class ReactiveSqlClientMultipleTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // Remove the default reactive PG client
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client"),
                    ArtifactKey.of("io.quarkus", "quarkus-reactive-pg-client-deployment")))
            // Force two reactive SQL clients so the resolver sees ambiguity
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-mysql-client-deployment", null),
                    Dependency.of("io.quarkus", "quarkus-reactive-oracle-client-deployment", null)))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "datasource '<default>' cannot be found",
                            "Datasource '<default>' is not configured"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
