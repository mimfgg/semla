package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.json.Json;
import io.semla.util.Maps;
import io.semla.util.Singleton;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RedisDatasource<T> extends EphemeralKeyValueDatasource<T> {

    private final JedisPool jedisPool;

    public RedisDatasource(EntityModel<T> model, JedisPool jedisPool, String keyspace) {
        super(model, keyspace);
        this.jedisPool = jedisPool;
    }

    @Override
    public JedisPool raw() {
        return jedisPool;
    }

    @Override
    public Optional<T> get(Object key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return Optional.ofNullable(jedis.get(prefix(key))).map(json -> Json.read(json, model().getType()));
        }
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        if (!keys.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                Map<String, T> found = jedis.mget(keys.stream().map(this::prefix).toArray(String[]::new)).stream()
                    .filter(Objects::nonNull)
                    .map(json -> Json.read(json, model().getType()))
                    .collect(Collectors.toMap(this::prefixedKeyOf, Function.identity()));
                return keys.stream().collect(Maps.collect(Function.identity(), key -> found.get(prefix(key))));
            }
        } else {
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void create(T entity) {
        try (Jedis jedis = jedisPool.getResource()) {
            generateKeyIfDefault(entity);
            jedis.set(prefixedKeyOf(entity), Json.write(EntityModel.copy(entity)));
        }
    }

    @Override
    public void set(T entity, Duration ttl) {
        try (Jedis jedis = jedisPool.getResource()) {
            generateKeyIfDefault(entity);
            jedis.set(prefixedKeyOf(entity), Json.write(EntityModel.copy(entity)), SetParams.setParams().px(ttl.toMillis()));
        }
    }

    @Override
    protected Integer getNextAutoIncrementedPK() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(prefix("PK_counter")).intValue();
        }
    }

    @Override
    public void create(Collection<T> entities) {
        if (!entities.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                Pipeline pipelined = jedis.pipelined();
                entities.forEach(entity -> pipelined.set(prefixedKeyOf(entity), Json.write(EntityModel.copy(entity))));
                pipelined.sync();
            }
        }
    }

    @Override
    public void set(Collection<T> entities, Duration ttl) {
        if (!entities.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                Pipeline pipelined = jedis.pipelined();
                entities.forEach(entity ->
                    pipelined.set(prefixedKeyOf(entity), Json.write(EntityModel.copy(entity)), SetParams.setParams().px(ttl.toMillis()))
                );
                pipelined.sync();
            }
        }
    }

    @Override
    public boolean delete(Object key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(prefix(key)) > 0;
        }
    }

    @Override
    public long delete(Collection<?> keys) {
        if (!keys.isEmpty()) {
            try (Jedis jedis = jedisPool.getResource()) {
                Pipeline pipelined = jedis.pipelined();
                keys.forEach(key -> pipelined.del(prefix(key)));
                return pipelined.syncAndReturnAll().stream().map(r -> (Long) r).reduce(Long::sum).orElse(0L);
            }
        }
        return 0L;
    }

    public static RedisDatasource.Configuration configure() {
        return new RedisDatasource.Configuration();
    }

    @TypeName("redis")
    public static class Configuration extends KeyValueDatasource.Configuration<RedisDatasource.Configuration> {

        public static final int DEFAULT_PORT = 6379;
        private final JedisPoolConfig config = new JedisPoolConfig();
        private String host = "localhost";
        private Integer port = DEFAULT_PORT;
        private final Singleton<JedisPool> client = Singleton.lazy(() -> new JedisPool(config, host, port));


        public JedisPool client() {
            return client.get();
        }

        @Serialize
        public int minIdle() {
            return config.getMinIdle();
        }

        @Deserialize
        public Configuration withMinIdle(int minIdle) {
            config.setMinIdle(minIdle);
            return this;
        }

        @Serialize
        public int maxIdle() {
            return config.getMaxIdle();
        }

        @Deserialize
        public Configuration withMaxIdle(int maxIdle) {
            config.setMaxIdle(maxIdle);
            return this;
        }

        @Serialize
        public long maxWaitMillis() {
            return config.getMaxWaitMillis();
        }

        @Deserialize
        public Configuration withMaxWaitMillis(long maxWaitMillis) {
            config.setMaxWaitMillis(maxWaitMillis);
            return this;
        }

        @Serialize
        public int maxTotal() {
            return config.getMaxTotal();
        }

        @Deserialize
        public Configuration withMaxTotal(int maxTotal) {
            config.setMaxTotal(maxTotal);
            return this;
        }

        @Serialize
        public String host() {
            return host;
        }

        @Deserialize
        public Configuration withHost(String host) {
            this.host = host;
            return this;
        }

        @Serialize
        public Integer port() {
            return port;
        }

        public JedisPoolConfig config() {
            return config;
        }

        @Deserialize
        public Configuration withPort(Integer port) {
            this.port = port;
            return this;
        }

        @Override
        public <T> RedisDatasource<T> create(EntityModel<T> entityModel) {
            return new RedisDatasource<>(entityModel, client(), keyspace());
        }

        @Override
        public void close() {
            client().close();
            client.reset();
        }
    }

}
