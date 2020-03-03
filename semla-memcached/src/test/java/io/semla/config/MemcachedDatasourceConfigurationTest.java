package io.semla.config;

import io.semla.datasource.MemcachedDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemcachedDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MemcachedDatasourceConfiguration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        MemcachedDatasourceConfiguration config = MemcachedDatasource.configure().withKeyspace("production");
        assertThat(config.keyspace()).isEqualTo("production");
        assertThat(config.hosts()).isNotNull().isNotEmpty().contains("localhost");
        MemcachedDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
        config.close();
    }

    @Test
    public void parse() {
        DatasourceConfiguration configuration = Yaml.read("" +
                "type: memcached\n" +
                "hosts: [\"localhost:11211\"]\n" +
                "keyspace: test"
            , DatasourceConfiguration.class);
        assertThat(configuration).isInstanceOf(MemcachedDatasourceConfiguration.class);
    }
}