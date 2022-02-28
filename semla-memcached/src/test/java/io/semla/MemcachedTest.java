package io.semla;

import io.semla.cache.Cache;
import io.semla.cache.CacheTest;
import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.datasource.*;
import io.semla.persistence.KeyValueCachedEntityManagerTest;
import io.semla.persistence.KeyValueCachedTypedEntityManagerTest;
import io.semla.relation.KeyValueRelationsTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runners.Suite;
import org.testcontainers.containers.GenericContainer;

import static io.semla.datasource.Datasource.Configuration.wrapped;
import static io.semla.datasource.MemcachedDatasource.Configuration.DEFAULT_PORT;
import static io.semla.util.Unchecked.unchecked;

@Suite.SuiteClasses({
    CacheTest.class,
    CachedKeyValueDatasourceTest.class,
    KeyValueDatasourceTest.class,
    MasterSlaveKeyValueDatasourceTest.class,
    ReadOneWriteAllKeyValueDatasourceTest.class,
    ShardedKeyValueDatasourceTest.class,
    KeyValueCachedEntityManagerTest.class,
    KeyValueCachedTypedEntityManagerTest.class,
    KeyValueRelationsTest.class,
    MemcachedTest.MemcachedDatasourceTest.class
})
public class MemcachedTest extends KeyValueDatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("memcached:latest").withExposedPorts(DEFAULT_PORT);

    @BeforeClass
    public static void init() {
        MemcachedDatasource.Configuration memcached = MemcachedDatasource.configure()
            .withHosts(container.getContainerIpAddress() + ":" + container.getMappedPort(DEFAULT_PORT))
            .withKeyspace("test")
            .autoclose();
        EntitySteps.setDefaultCache(binder -> binder.bind(Cache.class).to(memcached.asCache()));
        EntitySteps.setDefaultDatasource(wrapped(model -> memcached.withKeyspace(getNext("keyspace"))));
        EntitySteps.addCleanup(() -> unchecked(() -> memcached.client().flush().get()));
    }

    public static class MemcachedDatasourceTest extends EphemeralDatasourceTest {}

}
