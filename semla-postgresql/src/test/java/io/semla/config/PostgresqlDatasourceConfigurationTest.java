package io.semla.config;

import io.semla.datasource.PostgresqlDatasource;
import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresqlDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(PostgresqlDatasourceConfiguration.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create() {
        PostgresqlDatasource.configure().create(EntityModel.of(Player.class));
    }

    @Test
    public void parse() {
        DatasourceConfiguration configuration = Yaml.read("" +
                "type: postgresql\n" +
                "jdbcUrl: \"jdbc:postgresql://[::1]:5740/accounting\"\n"
            , DatasourceConfiguration.class);
        assertThat(configuration).isInstanceOf(PostgresqlDatasourceConfiguration.class);
    }
}