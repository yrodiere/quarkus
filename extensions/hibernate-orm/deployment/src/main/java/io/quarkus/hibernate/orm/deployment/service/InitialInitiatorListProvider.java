package io.quarkus.hibernate.orm.deployment.service;

import java.util.List;

import org.hibernate.boot.registry.StandardServiceInitiator;

import io.quarkus.hibernate.orm.deployment.boot.RecordableBootstrap;

/**
 * The initial list of StandardServiceInitiator instances is a constant
 * for Hibernate ORM "classic", but the list needs to be different for
 * Hibernate Reactive.
 * Also, the list elements occasionally hold state so rather than having
 * two constants we need a shared contract for producing the list.
 * This is such contract:
 *
 * @see RecordableBootstrap#RecordableBootstrap(org.hibernate.boot.registry.BootstrapServiceRegistry,
 *      InitialInitiatorListProvider)
 */
public interface InitialInitiatorListProvider {

    List<StandardServiceInitiator<?>> initialInitiatorList();

}
