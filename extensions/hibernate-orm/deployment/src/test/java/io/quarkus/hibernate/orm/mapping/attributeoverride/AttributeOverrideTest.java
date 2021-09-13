package io.quarkus.hibernate.orm.mapping.attributeoverride;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class AttributeOverrideTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MappedSuperclassType.class)
                    .addClass(DerivedEntityType.class)
                    .addClass(SchemaUtil.class)
                    .addClass(SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.log.sql", "true");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void attributeOverrideTakenIntoAccount() {
        assertThat(SchemaUtil.getColumnNames(entityManagerFactory, DerivedEntityType.class))
                .contains("custom_name")
                .doesNotContain("name");
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(entityManager,
                DerivedEntityType.class, DerivedEntityType::new,
                DerivedEntityType::getId, DerivedEntityType::setName, DerivedEntityType::getName);
    }

    @MappedSuperclass
    public static class MappedSuperclassType {

        private String name;

        public MappedSuperclassType() {
        }

        public MappedSuperclassType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity(name = "derivedentity")
    @AttributeOverride(name = "name", column = @Column(name = "custom_name"))
    public static class DerivedEntityType extends MappedSuperclassType {

        @Id
        @GeneratedValue
        private long id;

        public DerivedEntityType() {
        }

        public DerivedEntityType(String name) {
            super(name);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }
    }
}
