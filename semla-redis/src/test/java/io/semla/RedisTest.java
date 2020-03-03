package io.semla;

import io.semla.cache.Cache;
import io.semla.config.RedisDatasourceConfiguration;
import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.RedisDatasource;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

import static io.semla.config.DatasourceConfiguration.wrapped;
import static io.semla.config.RedisDatasourceConfiguration.DEFAULT_PORT;

public class RedisTest extends KeyValueDatasourceSuite {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("redis:latest").withExposedPorts(DEFAULT_PORT);

    @BeforeClass
    public static void init() {
        RedisDatasourceConfiguration redis = RedisDatasource.configure()
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
}
