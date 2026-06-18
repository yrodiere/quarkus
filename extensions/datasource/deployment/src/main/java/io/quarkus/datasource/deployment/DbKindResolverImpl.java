package io.quarkus.datasource.deployment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

public abstract class DbKindResolverImpl implements DataSourceDbKindResolverBuildItem.DbKindResolver {
    private final DataSourcesBuildTimeConfig config;

    private DbKindResolverImpl(DataSourcesBuildTimeConfig config) {
        this.config = config;
    }

    @Override
    public Optional<String> getOptional(String dataSourceName) {
        return config.dataSources().get(dataSourceName).dbKind();
    }

    public static class NoDefault extends DbKindResolverImpl {
        public NoDefault(DataSourcesBuildTimeConfig config) {
            super(config);
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            // FIXME setting ...=other and configuring the "client" does not make sense in reactive...
            //   We should probably just expose the resolved default in a build item using a sealed type,
            //   and implement the reason generation differently for Agroal/Reactive?
            return new Reason(String.format(Locale.ROOT, """
                    No default db-kind is available. \
                    Please add a %1$s extension (e.g. %2$s, etc.), \
                    or set '%3$s=other' and configure the %1$s explicitly. \
                    Refer to https://quarkus.io/guides/datasource for guidance.
                    """,
                    switch (paradigm) {
                        case BLOCKING -> "JDBC driver";
                        case REACTIVE -> "Reactive SQL Client";
                    },
                    switch (paradigm) {
                        case BLOCKING -> "quarkus-jdbc-postgresql, quarkus-jdbc-mysql";
                        case REACTIVE -> "quarkus-reactive-pg-client, quarkus-reactive-mysql-client";
                    },
                    DataSourceUtil.dataSourcePropertyKey(dataSourceName, "db-kind")));
        }
    }

    public static class SingleDefault extends DbKindResolverImpl {
        private final String theDefault;

        public SingleDefault(DataSourcesBuildTimeConfig config, String theDefault) {
            super(config);
            this.theDefault = theDefault;
        }

        @Override
        public Optional<String> getOptional(String dataSourceName) {
            var result = super.getOptional(dataSourceName);
            if (result.isEmpty()) {
                result = Optional.of(theDefault);
            }
            return result;
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            throw new IllegalStateException("This method should not be called, because the db-kind _is_ available");
        }
    }

    public static class MultipleDefaults extends DbKindResolverImpl {
        private final List<String> defaultDbKindStrings;

        public MultipleDefaults(DataSourcesBuildTimeConfig config, List<String> defaultDbKindStrings) {
            super(config);
            this.defaultDbKindStrings = defaultDbKindStrings;
        }

        @Override
        public Optional<String> getOptional(String dataSourceName) {
            // If db-kind is explicitly configured, use it; otherwise we can't pick a default
            return super.getOptional(dataSourceName);
        }

        @Override
        public Reason unavailableReason(String dataSourceName, ProgrammingParadigm paradigm) {
            return new Reason(String.format(Locale.ROOT, """
                    Multiple db-kinds are available: %2$s. \
                    Please select an extension by setting '%3$s' to one of these values, \
                    or set '%3$s=other' and configure the %1$s explicitly. \
                    Refer to https://quarkus.io/guides/datasource for guidance.
                    """,
                    switch (paradigm) {
                        case BLOCKING -> "JDBC driver";
                        case REACTIVE -> "Reactive SQL Client";
                    },
                    defaultDbKindStrings,
                    DataSourceUtil.dataSourcePropertyKey(dataSourceName, "db-kind")));
        }
    }
}
