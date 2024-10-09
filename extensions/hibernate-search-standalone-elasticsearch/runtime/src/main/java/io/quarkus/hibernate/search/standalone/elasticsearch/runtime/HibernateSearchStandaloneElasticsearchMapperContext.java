package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;

import io.quarkus.hibernate.search.backend.elasticsearch.runtime.MapperContext;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.runtime.annotations.RecordableConstructor;

public final class HibernateSearchStandaloneElasticsearchMapperContext implements MapperContext {

    private final Set<String> backendNamesForIndexedEntities;
    private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;

    @RecordableConstructor
    public HibernateSearchStandaloneElasticsearchMapperContext(Set<String> backendNamesForIndexedEntities,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
        this.backendNamesForIndexedEntities = backendNamesForIndexedEntities;
        this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }

    @Override
    public Set<String> getBackendNamesForIndexedEntities() {
        return backendNamesForIndexedEntities;
    }

    @Override
    public Map<String, Set<String>> getBackendAndIndexNamesForSearchExtensions() {
        return backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String backendPropertyKey(String backendName, String indexName, String propertyKeyRadical) {
        return HibernateSearchStandaloneRuntimeConfig.backendPropertyKey(backendName, indexName, propertyKeyRadical);
    }

    @Override
    public <T> Optional<BeanReference<T>> singleExtensionBeanReferenceFor(Optional<String> override, Class<T> beanType,
            String backendName, String indexName) {
        return HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(override, beanType, backendName, indexName);
    }

    @Override
    public <T> Optional<List<BeanReference<T>>> multiExtensionBeanReferencesFor(Optional<List<String>> override,
            Class<T> beanType, String backendName, String indexName) {
        return HibernateSearchBeanUtil.multiExtensionBeanReferencesFor(override, beanType, backendName, indexName);
    }
}
