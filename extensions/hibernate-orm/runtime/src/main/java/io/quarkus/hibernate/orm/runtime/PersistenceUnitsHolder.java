package io.quarkus.hibernate.orm.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.integrator.spi.Integrator;

import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.recording.BuildTimeRecordedState;
import io.quarkus.hibernate.orm.runtime.recording.StaticInitRecordedState;

public final class PersistenceUnitsHolder {

    private static final String NO_NAME_TOKEN = "__no_name";

    // Populated by Quarkus's runtime phase from Quarkus deployment info
    private static volatile PersistenceUnits persistenceUnits;

    /**
     * Initialize JPA for use in Quarkus. In a native image. This must be called
     * from within a static init method.
     */
    static void jpaStaticInitialization(Map<String, BuildTimeRecordedState> buildTimeRecordedStates,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies preGeneratedProxies) {
        final List<QuarkusPersistenceUnitDescriptor> puDescriptors = buildTimeRecordedStates.values().stream()
                .map(BuildTimeRecordedState::getDescriptor)
                .collect(Collectors.toList());
        final Map<String, StaticInitRecordedState> metadata = constructMetadataAdvance(buildTimeRecordedStates,
                additionalIntegrators,
                preGeneratedProxies);

        persistenceUnits = new PersistenceUnits(puDescriptors, metadata);
    }

    public static List<QuarkusPersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        checkJPAInitialization();
        return persistenceUnits.units;
    }

    public static StaticInitRecordedState popRecordedState(String persistenceUnitName) {
        checkJPAInitialization();
        return persistenceUnits.recordedStates.remove(persistenceUnitName);
    }

    private static Map<String, StaticInitRecordedState> constructMetadataAdvance(
            final Map<String, BuildTimeRecordedState> buildTimeStates,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyClassDefinitions) {
        Map<String, StaticInitRecordedState> recordedStates = new HashMap<>();

        for (var entry : buildTimeStates.entrySet()) {
            var puName = entry.getKey();
            var buildTimeState = entry.getValue();
            var staticInitState = StaticInitRecordedState.create(buildTimeState, additionalIntegrators, proxyClassDefinitions);
            Object previous = recordedStates.put(puName, staticInitState);
            if (previous != null) {
                throw new IllegalStateException("Duplicate persistence unit name: " + puName);
            }
        }

        return recordedStates;
    }

    private static void checkJPAInitialization() {
        if (persistenceUnits == null) {
            throw new RuntimeException("JPA not initialized yet by Quarkus: this is likely a bug.");
        }
    }

    private static class PersistenceUnits {

        private final List<QuarkusPersistenceUnitDescriptor> units;

        private final Map<String, StaticInitRecordedState> recordedStates;

        public PersistenceUnits(final List<QuarkusPersistenceUnitDescriptor> units,
                final Map<String, StaticInitRecordedState> recordedStates) {
            this.units = units;
            this.recordedStates = recordedStates;
        }
    }

}
