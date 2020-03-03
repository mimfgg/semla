package io.semla.config;

import io.semla.datasource.MysqlDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MysqlDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MysqlDatasourceConfiguration.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create() {
        MysqlDatasourceConfiguration config = MysqlDatasource.configure();
        assertThat(config.autoCreateTable()).isFalse();
        config.create(EntityModel.of(Player.class));
    }

    @Test
    public void parse() {
        DatasourceConfiguration configuration = Yaml.read("" +
                "type: mysql\n" +
                "jdbcUrl: \"jdbc:mysql://test:3306/test\"\n"
            , DatasourceConfiguration.class);
        assertThat(configuration).isInstanceOf(MysqlDatasourceConfiguration.class);
    }
}