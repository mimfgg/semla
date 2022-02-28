package io.semla;

import io.semla.cache.Cache;
import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.datasource.MongoDBDatasource;
import io.semla.util.Maps;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;

import static io.semla.datasource.Datasource.Configuration.wrapped;
import static io.semla.datasource.MongoDBDatasource.Configuration.DEFAULT_PORT;

public class MongoDBTest extends DatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("mongo:4.2.2-bionic")
        .withExposedPorts(DEFAULT_PORT)
        .withTmpFs(Maps.of("/data/db/", "rw"));

    @BeforeClass
    public static void init() {
        MongoDBDatasource.Configuration mongodb = MongoDBDatasource.configure()
            .withHost(container.getContainerIpAddress())
            .withPort(container.getMappedPort(DEFAULT_PORT))
            .autoclose();
        EntitySteps.setDefaultCache(binder -> binder.bind(Cache.class).to(mongodb.withDatabase(getNext("cacheEntries")).asCache()));
        EntitySteps.setDefaultDatasource(wrapped(entityModel -> mongodb.withDatabase(getNext(entityModel.tablename()))));
    }
}
