package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(RedisDatasource.Configuration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        RedisDatasource.Configuration config = RedisDatasource.configure()
            .withHost("localhost")
            .withPort(1234)
            .withKeyspace("production")
            .withMinIdle(1)
            .withMaxIdle(5)
            .withMaxTotal(10)
            .withMaxWaitMillis(1000);
        assertThat(config.host()).isEqualTo("localhost");
        assertThat(config.port()).isEqualTo(1234);
        assertThat(config.keyspace()).isEqualTo("production");
        assertThat(config.minIdle()).isEqualTo(1);
        assertThat(config.maxIdle()).isEqualTo(5);
        assertThat(config.maxTotal()).isEqualTo(10);
        assertThat(config.maxWaitMillis()).isEqualTo(1000);
        assertThat(config.config()).isNotNull();
        RedisDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
        config.close();
    }

    @Test
    public void parse() {
        Datasource.Configuration configuration = Yaml.read("type: redis", Datasource.Configuration.class);
        assertThat(configuration).isInstanceOf(RedisDatasource.Configuration.class);
    }
}
