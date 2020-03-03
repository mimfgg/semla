package io.semla.config;

import io.semla.datasource.MongoDBDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDBDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MongoDBDatasourceConfiguration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        MongoDBDatasourceConfiguration config = MongoDBDatasource.configure()
            .withHost("localhost")
            .withPort(1234)
            .withDatabase("test");
        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(1234);
        assertThat(config.database()).isEqualTo("test");
        MongoDBDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
        config.close();
    }

    @Test
    public void parse() {
        DatasourceConfiguration configuration = Yaml.read("" +
                "type: mongodb\n" +
                "host: localhost\n" +
                "database: test"
            , DatasourceConfiguration.class);
        assertThat(configuration).isInstanceOf(MongoDBDatasourceConfiguration.class);
    }
}