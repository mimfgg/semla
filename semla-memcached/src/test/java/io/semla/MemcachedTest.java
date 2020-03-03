package io.semla;

import ch.qos.logback.classic.Level;
import io.semla.cache.Cache;
import io.semla.config.MemcachedDatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.MemcachedDatasource;
import io.semla.logging.Logging;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;

import static io.semla.config.DatasourceConfiguration.wrapped;
import static io.semla.config.MemcachedDatasourceConfiguration.DEFAULT_PORT;
import static io.semla.util.Unchecked.unchecked;

public class MemcachedTest extends KeyValueDatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("memcached:latest").withExposedPorts(DEFAULT_PORT);

    @BeforeClass
    public static void init() {
        MemcachedDatasourceConfiguration memcached = MemcachedDatasource.configure()
            .withHosts(container.getContainerIpAddress() + ":" + container.getMappedPort(DEFAULT_PORT))
            .withKeyspace("test")
            .autoclose();
        EntitySteps.setDefaultCache(binder -> binder.bind(Cache.class).to(memcached.asCache()));
        EntitySteps.setDefaultDatasource(wrapped(model -> memcached.withKeyspace(getNext("keyspace"))));
        EntitySteps.addCleanup(() -> unchecked(() -> memcached.client().flush().get()));
    }
}
