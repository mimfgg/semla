package io.semla.config;

import io.semla.datasource.MasterSlaveDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@TypeName("master-slave")
public class MasterSlaveDatasourceConfiguration implements DatasourceConfiguration {

    private DatasourceConfiguration master;
    private List<DatasourceConfiguration> slaves = new ArrayList<>();

    @Serialize
    public DatasourceConfiguration master() {
        return master;
    }

    @Deserialize
    public MasterSlaveDatasourceConfiguration withMaster(DatasourceConfiguration master) {
        this.master = master;
        return this;
    }

    @Serialize
    public List<DatasourceConfiguration> slaves() {
        return slaves;
    }

    @Deserialize
    public MasterSlaveDatasourceConfiguration withSlaves(DatasourceConfiguration... slaves) {
        this.slaves = Arrays.asList(slaves);
        return this;
    }

    @Override
    public <T> MasterSlaveDatasource<T> create(EntityModel<T> model) {
        return new MasterSlaveDatasource<>(model,
            master.create(model),
            slaves.stream().map(conf -> conf.create(model)).collect(Collectors.toList())
        );
    }

    @Override
    public void close() {
        master.close();
        slaves.forEach(DatasourceConfiguration::close);
    }
}
