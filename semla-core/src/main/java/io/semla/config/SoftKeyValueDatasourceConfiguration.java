package io.semla.config;

import io.semla.datasource.SoftKeyValueDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.TypeName;

@TypeName("soft-key-value")
public class SoftKeyValueDatasourceConfiguration extends KeyspacedDatasourceConfiguration<SoftKeyValueDatasourceConfiguration> {

    @Override
    public <T> SoftKeyValueDatasource<T> create(EntityModel<T> entityModel) {
        return new SoftKeyValueDatasource<>(entityModel, keyspace());
    }
}
