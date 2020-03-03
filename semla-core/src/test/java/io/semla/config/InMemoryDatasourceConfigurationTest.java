package io.semla.config;

import io.semla.datasource.Datasource;
import io.semla.datasource.InMemoryDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(InMemoryDatasourceConfiguration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        InMemoryDatasource<Player> datasource = InMemoryDatasource.configure()
            .create(model);
        assertThat(datasource).isNotNull();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("type: in-memory", DatasourceConfiguration.class)
            .create(model);
        assertThat(datasource).isNotNull();
        assertThat(datasource).isInstanceOf(InMemoryDatasource.class);
    }
}