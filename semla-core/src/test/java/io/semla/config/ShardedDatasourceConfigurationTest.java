package io.semla.config;

import io.semla.datasource.Datasource;
import io.semla.datasource.InMemoryDatasource;
import io.semla.datasource.ShardedDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShardedDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(
            ShardedDatasourceConfiguration.class,
            InMemoryDatasourceConfiguration.class,
            ShardedDatasource.KeyedShardingStrategy.class
        );
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        ShardedDatasourceConfiguration config = ShardedDatasource.configure()
            .withDatasources(InMemoryDatasource.configure(), InMemoryDatasource.configure());
        assertThat(config.datasources()).isNotNull().isNotEmpty();
        assertThat(config.rebalancing()).isFalse();
        assertThat(config.strategy()).isInstanceOf(ShardedDatasource.KeyedShardingStrategy.class);
        ShardedDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("" +
                "type: sharded\n" +
                "strategy: keyed\n" +
                "datasources:\n" +
                "  - type: in-memory\n" +
                "  - type: in-memory\n"
            , DatasourceConfiguration.class)
            .create(model);
        assertThat(datasource).isNotNull();
        assertThat(datasource).isInstanceOf(ShardedDatasource.class);
    }
}