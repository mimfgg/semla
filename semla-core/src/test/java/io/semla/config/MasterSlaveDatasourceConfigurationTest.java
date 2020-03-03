package io.semla.config;

import io.semla.datasource.Datasource;
import io.semla.datasource.InMemoryDatasource;
import io.semla.datasource.MasterSlaveDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MasterSlaveDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MasterSlaveDatasourceConfiguration.class, InMemoryDatasourceConfiguration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        MasterSlaveDatasourceConfiguration config = MasterSlaveDatasource.configure()
            .withMaster(InMemoryDatasource.configure())
            .withSlaves(InMemoryDatasource.configure(), InMemoryDatasource.configure());
        assertThat(config.master()).isNotNull();
        assertThat(config.slaves()).isNotNull().isNotEmpty();
        MasterSlaveDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("" +
                "type: master-slave\n" +
                "master:\n" +
                "  type: in-memory\n" +
                "slaves:\n" +
                "  - type: in-memory\n" +
                "  - type: in-memory\n"
            , DatasourceConfiguration.class)
            .create(model);
        assertThat(datasource).isNotNull();
        assertThat(datasource).isInstanceOf(MasterSlaveDatasource.class);
    }
}