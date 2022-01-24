package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MysqlDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(MysqlDatasource.Configuration.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create() {
        MysqlDatasource.Configuration config = MysqlDatasource.configure();
        assertThat(config.autoCreateTable()).isFalse();
        config.create(EntityModel.of(Player.class));
    }

    @Test
    public void parse() {
        Datasource.Configuration configuration = Yaml.read("""
                type: mysql
                jdbcUrl: "jdbc:mysql://test:3306/test"
                """
            , Datasource.Configuration.class);
        assertThat(configuration).isInstanceOf(MysqlDatasource.Configuration.class);
    }
}
