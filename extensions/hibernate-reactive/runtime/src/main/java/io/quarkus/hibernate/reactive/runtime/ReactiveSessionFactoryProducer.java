package io.quarkus.hibernate.reactive.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;

public class ReactiveSessionFactoryProducer {

    @Inject
    @PersistenceUnit
    EntityManagerFactory emf;

    @Produces
    @ApplicationScoped
    @DefaultBean
    @Unremovable
    @Typed(Mutiny.SessionFactory.class)
    public Mutiny.SessionFactory mutinySessionFactory() {
        return emf.unwrap(Mutiny.SessionFactory.class);
    }

}
