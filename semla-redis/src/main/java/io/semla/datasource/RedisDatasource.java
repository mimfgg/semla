package io.semla.datasource;

import io.semla.config.RedisDatasourceConfiguration;
import io.semla.model.EntityModel;
import io.semla.serialization.json.Json;
import io.semla.util.Maps;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
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

    public static RedisDatasourceConfiguration configure() {
        return new RedisDatasourceConfiguration();
    }
}
