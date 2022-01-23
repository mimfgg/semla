package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.model.Player;
import io.semla.reflect.Types;
import io.semla.serialization.yaml.Yaml;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class HsqlDatasourceConfigurationTest {

    static {
        Types.registerSubTypes(HsqlDatasource.Configuration.class);
    }

    @Test
    public void create() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        HsqlDatasource.Configuration config = HsqlDatasource.configure()
            .withDriverClassName("org.hsqldb.jdbcDriver")
            .withUsername("SA")
            .withPassword("")
            .withJdbcUrl("jdbc:hsqldb:mem:test")
            .withConnectionTestQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS")
            .withMaximumPoolSize(1)
            .withIdleTimeout(Duration.ofMinutes(1));
        assertThat(config.driverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
        assertThat(config.username()).isEqualTo("SA");
        assertThat(config.password()).isEqualTo("");
        assertThat(config.jdbcUrl()).isEqualTo("jdbc:hsqldb:mem:test");
        assertThat(config.connectionTestQuery()).isEqualTo("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        assertThat(config.maximumPoolSize()).isEqualTo(1);
        assertThat(config.idleTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.hikariConfig()).isNotNull();
        HsqlDatasource<Player> datasource = config.create(model);
        assertThat(datasource).isNotNull();
        config.close();
    }

    @Test
    public void parse() {
        EntityModel<Player> model = EntityModel.of(Player.class);
        Datasource<Player> datasource = Yaml.read("""
                type: hsql
                jdbcUrl: jdbc:hsqldb:mem:test
                """,
            Datasource.Configuration.class).create(model);
        assertThat(datasource).isInstanceOf(HsqlDatasource.class);
    }
}
