package io.semla;

import io.semla.cache.Cache;
import io.semla.config.HsqlDatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.HsqlDatasource;
import org.junit.BeforeClass;

import static io.semla.config.DatasourceConfiguration.generic;

public class JdbiTest extends DatasourceSuite {

    @BeforeClass
    public static void init() {
        HsqlDatasourceConfiguration hsql = HsqlDatasource.configure()
            .withJdbcUrl("jdbc:hsqldb:mem:test")
            .withAutoCreateTable(true)
            .autoclose();
        EntitySteps.setDefaultCache(binder ->
            binder.bind(Cache.class).to(hsql.asCache(entityModel -> hsql.create(entityModel, getNext(entityModel.tablename()))))
        );
        EntitySteps.setDefaultDatasource(generic(entityModel -> hsql.create(entityModel, getNext(entityModel.tablename()))));
    }
}
