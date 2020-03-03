package io.semla.config;

import io.semla.datasource.CachedDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;

@TypeName("cached")
public class CachedDatasourceConfiguration implements DatasourceConfiguration {

    private DatasourceConfiguration cache;
    private DatasourceConfiguration datasource;

    @Deserialize
    public DatasourceConfiguration cache() {
        return cache;
    }

    @Serialize
    public CachedDatasourceConfiguration withCache(DatasourceConfiguration cache) {
        this.cache = cache;
        return this;
    }

    @Deserialize
    public DatasourceConfiguration datasource() {
        return datasource;
    }

    @Serialize
    public CachedDatasourceConfiguration withDatasource(DatasourceConfiguration datasource) {
        this.datasource = datasource;
        return this;
    }

    @Override
    public <T> CachedDatasource<T> create(EntityModel<T> model) {
        return new CachedDatasource<>(model, cache.create(model), datasource.create(model));
    }
}
