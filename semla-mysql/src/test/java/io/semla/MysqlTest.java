package io.semla;

import io.semla.cache.Cache;
import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.datasource.MysqlDatasource;
import io.semla.util.Maps;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

import java.util.TimeZone;

import static io.semla.datasource.Datasource.Configuration.generic;


public class MysqlTest extends DatasourceSuite {

    @ClassRule
    public static JdbcDatabaseContainer<?> container = new MySQLContainer<>("mysql:8.0.18")
        .withTmpFs(Maps.of("/var/lib/mysql", "rw"));

    @BeforeClass
    public static void init() {
        MysqlDatasource.Configuration mysql = MysqlDatasource.configure()
            .withJdbcUrl(container.getJdbcUrl() + "?serverTimezone=" + TimeZone.getDefault().getID())
            .withUsername(container.getUsername())
            .withPassword(container.getPassword())
            .withAutoCreateTable(true);
        EntitySteps.setDefaultCache(binder ->
            binder.bind(Cache.class).to(mysql.asCache(entityModel -> mysql.create(entityModel, getNext(entityModel.tablename()))))
        );
        EntitySteps.setDefaultDatasource(generic(entityModel -> mysql.create(entityModel, getNext(entityModel.tablename()))));
    }
}
