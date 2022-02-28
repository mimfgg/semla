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
import redis.clients.jedis.Jedis;

import static io.semla.datasource.Datasource.Configuration.wrapped;
import static io.semla.datasource.RedisDatasource.Configuration.DEFAULT_PORT;

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
    RedisTest.RedisDatasourceTest.class
})
public class RedisTest extends KeyValueDatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("redis:latest").withExposedPorts(DEFAULT_PORT);

    @BeforeClass
    public static void init() {
        RedisDatasource.Configuration redis = RedisDatasource.configure()
            .withHost(container.getContainerIpAddress())
            .withPort(container.getMappedPort(DEFAULT_PORT))
            .autoclose();
        EntitySteps.setDefaultCache(binder -> binder.bind(Cache.class).to(redis.asCache()));
        EntitySteps.setDefaultDatasource(wrapped(entityModel -> redis.withKeyspace(getNext(entityModel.tablename()))));
        EntitySteps.addCleanup(() -> {
            try (Jedis jedis = redis.client().getResource()) {
                jedis.flushAll();
            }
        });
    }

    public static class RedisDatasourceTest extends EphemeralDatasourceTest {}

}
