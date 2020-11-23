package io.quarkus.it.hibernate.reactive.postgresql.jdbcmetadata;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("/jdbc-metadata-retrieval")
@ApplicationScoped
public class JdbcMetadataRetrievalResource {

    @Inject
    Mutiny.Session mutinySession;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Uni<String> test() {
        EntityWithSequenceIdentityId entity = new EntityWithSequenceIdentityId();
        entity.setName("thePersistedName");
        return mutinySession
                .persist(entity)
                .chain(() -> mutinySession.flush())
                .invoke(() -> mutinySession.clear())
                .chain(() -> mutinySession.find(EntityWithSequenceIdentityId.class, entity.getId()))
                .map(EntityWithSequenceIdentityId::getName);
    }
}
