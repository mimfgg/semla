package io.semla.config;

import io.semla.datasource.CachedDatasource;
import io.semla.datasource.Datasource;
import io.semla.datasource.InMemoryDatasource;
import io.semla.datasource.SoftKeyValueDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(SoftKeyValueDatasourceConfiguration.class, InMemoryDatasourceConfiguration.class, CachedDatasourceConfiguration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        CachedDatasourceConfiguration config = CachedDatasource.configure()
            .withCache(SoftKeyValueDatasource.configure())
            .withDatasource(InMemoryDatasource.configure());
        assertThat(config.cache()).isNotNull();
        assertThat(config.datasource()).isNotNull();
        CachedDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("" +
                "type: cached\n" +
                "cache:\n" +
                "  type: soft-key-value\n" +
                "datasource:\n" +
                "  type: in-memory\n"
            , DatasourceConfiguration.class)
            .create(model);
        assertThat(datasource).isNotNull();
        assertThat(datasource).isInstanceOf(CachedDatasource.class);
    }
}