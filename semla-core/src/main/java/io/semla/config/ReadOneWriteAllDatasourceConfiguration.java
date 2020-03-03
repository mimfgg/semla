package io.semla.config;

import io.semla.datasource.ReadOneWriteAllDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@TypeName("read-one-write-all")
public class ReadOneWriteAllDatasourceConfiguration implements DatasourceConfiguration {

    private List<DatasourceConfiguration> datasources = new ArrayList<>();

    @Serialize
    public List<DatasourceConfiguration> datasources() {
        return datasources;
    }

    @Deserialize
    public ReadOneWriteAllDatasourceConfiguration withDatasources(DatasourceConfiguration... datasources) {
        this.datasources.addAll(Arrays.asList(datasources));
        return this;
    }

    @Override
    public <T> ReadOneWriteAllDatasource<T> create(EntityModel<T> model) {
        return new ReadOneWriteAllDatasource<>(model, datasources.stream().map(conf -> conf.create(model)).collect(Collectors.toList()));
    }

    @Override
    public void close() {
        datasources.forEach(DatasourceConfiguration::close);
    }
}
