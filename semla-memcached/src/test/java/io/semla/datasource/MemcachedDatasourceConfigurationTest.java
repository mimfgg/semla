package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemcachedDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MemcachedDatasource.Configuration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        MemcachedDatasource.Configuration config = MemcachedDatasource.configure()
            .withHosts("localhost").withKeyspace("production");
        assertThat(config.keyspace()).isEqualTo("production");
        assertThat(config.hosts()).isNotNull().isNotEmpty().contains("localhost");
        MemcachedDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
        config.close();
    }

    @Test
    public void parse() {
        Datasource.Configuration configuration = Yaml.read("" +
                "type: memcached\n" +
                "hosts: [\"localhost:11211\"]\n" +
                "keyspace: test"
            , Datasource.Configuration.class);
        assertThat(configuration).isInstanceOf(MemcachedDatasource.Configuration.class);
    }
}
