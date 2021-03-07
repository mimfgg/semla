package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReadOneWriteAllDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(ReadOneWriteAllDatasource.Configuration.class, InMemoryDatasource.Configuration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        ReadOneWriteAllDatasource.Configuration config = ReadOneWriteAllDatasource.configure()
            .withDatasources(InMemoryDatasource.configure(), InMemoryDatasource.configure());
        assertThat(config.datasources()).isNotNull().isNotEmpty();
        ReadOneWriteAllDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("" +
                "type: read-one-write-all\n" +
                "datasources:\n" +
                "  - type: in-memory\n" +
                "  - type: in-memory\n",
            Datasource.Configuration.class).create(model);
        assertThat(datasource).isNotNull();
        assertThat(datasource).isInstanceOf(ReadOneWriteAllDatasource.class);
    }
}
