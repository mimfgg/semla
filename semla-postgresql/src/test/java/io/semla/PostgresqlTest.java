package io.semla;

import io.semla.cache.Cache;
import io.semla.config.PostgresqlDatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.PostgresqlDatasource;
import io.semla.util.Maps;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static io.semla.config.DatasourceConfiguration.generic;

public class PostgresqlTest extends DatasourceSuite {

    @ClassRule
    public static JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:12.1")
        .withTmpFs(Maps.of("/var/lib/postgresql/data", "rw"));

    @BeforeClass
    public static void init() {
        PostgresqlDatasourceConfiguration pgsql = PostgresqlDatasource.configure()
            .withDriverClassName("org.postgresql.Driver")
            .withJdbcUrl(container.getJdbcUrl())
            .withUsername(container.getUsername())
            .withPassword(container.getPassword())
            .withAutoCreateTable(true);
        EntitySteps.setDefaultCache(binder ->
            binder.bind(Cache.class).to(pgsql.asCache(entityModel -> pgsql.create(entityModel, getNext(entityModel.tablename()))))
        );
        EntitySteps.setDefaultDatasource(generic(entityModel -> pgsql.create(entityModel, getNext(entityModel.tablename()))));
    }
}
