package io.quarkus.it.hibernate.reactive.postgresql.jdbcmetadata;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HibernateReactiveJdbcMetadataRetrievalTest {

    @Test
    public void test() {
        when().get("/jdbc-metadata-retrieval").then()
                .body(is("thePersistedName"))
                .statusCode(200);
    }

}
