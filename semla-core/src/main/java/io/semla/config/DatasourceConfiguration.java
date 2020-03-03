package io.semla.config;

import io.semla.cache.Cache;
import io.semla.datasource.Datasource;
import io.semla.model.EntityModel;
import io.semla.persistence.CacheEntry;
import io.semla.serialization.annotations.TypeInfo;

import java.util.function.Function;

@FunctionalInterface
@TypeInfo
public interface DatasourceConfiguration {

    <T> Datasource<T> create(EntityModel<T> entityModel);

    default Cache asCache() {
        return Cache.of(create(EntityModel.of(CacheEntry.class)));
    }

    default Cache asCache(Function<EntityModel<CacheEntry>, Datasource<CacheEntry>> constructor) {
        return Cache.of(constructor.apply(EntityModel.of(CacheEntry.class)));
    }

    @SuppressWarnings("unchecked")
    default <DatasourceConfigurationType extends DatasourceConfiguration> DatasourceConfigurationType autoclose() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        return (DatasourceConfigurationType) this;
    }

    default void close() {}

    static DatasourceConfiguration wrapped(Function<EntityModel<?>, DatasourceConfiguration> override) {
        return new DatasourceConfiguration() {
            @Override
            public <T> Datasource<T> create(EntityModel<T> entityModel) {
                return override.apply(entityModel).create(entityModel);
            }
        };
    }

    static DatasourceConfiguration generic(Function<EntityModel<?>, Datasource<?>> configuration) {
        return new DatasourceConfiguration() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Datasource<T> create(EntityModel<T> entityModel) {
                return (Datasource<T>) configuration.apply(entityModel);
            }
        };
    }

}
