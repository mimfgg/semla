package io.semla.config;

import io.semla.datasource.InMemoryDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.TypeName;

@TypeName("in-memory")
public class InMemoryDatasourceConfiguration implements DatasourceConfiguration {

    @Override
    public <T> InMemoryDatasource<T> create(EntityModel<T> model) {
        return new InMemoryDatasource<>(model);
    }
}
