package io.semla;

import io.semla.cache.Cache;
import io.semla.config.MongoDBDatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.MongoDBDatasource;
import io.semla.util.Maps;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;

import static io.semla.config.DatasourceConfiguration.wrapped;
import static io.semla.config.MongoDBDatasourceConfiguration.DEFAULT_PORT;

public class MongoDBTest extends DatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("mongo:4.2.2-bionic")
        .withExposedPorts(DEFAULT_PORT)
        .withTmpFs(Maps.of("/data/db/", "rw"));

    @BeforeClass
    public static void init() {
        MongoDBDatasourceConfiguration mongodb = MongoDBDatasource.configure()
            .withHost(container.getContainerIpAddress())
            .withPort(container.getMappedPort(DEFAULT_PORT))
            .autoclose();
        EntitySteps.setDefaultCache(binder -> binder.bind(Cache.class).to(mongodb.withDatabase(getNext("cacheEntries")).asCache()));
        EntitySteps.setDefaultDatasource(wrapped(entityModel -> mongodb.withDatabase(getNext(entityModel.tablename()))));
    }
}
