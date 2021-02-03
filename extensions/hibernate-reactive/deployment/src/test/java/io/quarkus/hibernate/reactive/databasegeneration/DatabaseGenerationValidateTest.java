package io.quarkus.hibernate.reactive.databasegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DatabaseGenerationValidateTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyEntity.class)
                    .addAsResource("application-database-generation-validate.properties", "application.properties"));

    @Inject
    Mutiny.Session session;

    @Test
    @ActivateRequestContext
    public void test() {
        MyEntity entity = new MyEntity("default");

        MyEntity retrievedEntity = session.withTransaction(tx -> session.persist(entity))
                .chain(() -> session.withTransaction(tx -> session.clear().find(MyEntity.class, entity.getId())))
                .await().indefinitely();

        assertThat(retrievedEntity)
                .isNotSameAs(entity)
                .returns(entity.getName(), MyEntity::getName);
    }

    @Entity
    public static class MyEntity {

        private long id;

        private String name;

        public MyEntity() {
        }

        public MyEntity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "defaultSeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
