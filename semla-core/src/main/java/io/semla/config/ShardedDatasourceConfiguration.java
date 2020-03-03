package io.semla.config;

import io.semla.datasource.ShardedDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@TypeName("sharded")
public class ShardedDatasourceConfiguration implements DatasourceConfiguration {

    private ShardedDatasource.ShardingStrategy strategy = new ShardedDatasource.KeyedShardingStrategy();
    private boolean rebalacing;
    private List<DatasourceConfiguration> datasources = new ArrayList<>();

    @Serialize
    public List<DatasourceConfiguration> datasources() {
        return datasources;
    }

    @Deserialize
    public ShardedDatasourceConfiguration withDatasources(DatasourceConfiguration... datasources) {
        this.datasources.addAll(Arrays.asList(datasources));
        return this;
    }

    @Serialize
    public ShardedDatasource.ShardingStrategy strategy() {
        return strategy;
    }

    @Deserialize
    public ShardedDatasourceConfiguration withStrategy(ShardedDatasource.ShardingStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    @Serialize
    public boolean rebalancing() {
        return rebalacing;
    }

    @Deserialize
    public ShardedDatasourceConfiguration withRebalancing(boolean rebalacing) {
        this.rebalacing = rebalacing;
        return this;
    }

    @Override
    public <T> ShardedDatasource<T> create(EntityModel<T> entityModel) {
        return new ShardedDatasource<>(
            entityModel,
            strategy,
            rebalacing,
            datasources.stream().map(conf -> conf.create(entityModel)).collect(Collectors.toList())
        );
    }

    @Override
    public void close() {
        datasources.forEach(DatasourceConfiguration::close);
    }
}
