package io.semla.datasource;

import io.semla.config.MemcachedDatasourceConfiguration;
import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.serialization.json.Json;
import net.spy.memcached.CachedData;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.semla.util.Unchecked.unchecked;

public class MemcachedDatasource<T> extends EphemeralKeyValueDatasource<T> {

    public static final Duration DEFAULT_TTL = Duration.ofHours(3);
    private MemcachedClient memcached;
    private Transcoder<T> transcoder;

    public MemcachedDatasource(EntityModel<T> model, MemcachedClient memcached, String keyspace) {
        super(model, keyspace);
        this.memcached = memcached;
        transcoder = new Transcoder<T>() {
            @Override
            public boolean asyncDecode(CachedData data) {
                return false;
            }

            @Override
            public CachedData encode(T entity) {
                return new CachedData(0, Json.write(EntityModel.copy(entity)).getBytes(), getMaxSize());
            }

            @Override
            public T decode(CachedData data) {
                return Json.read(new String(data.getData()), model().getType());
            }

            @Override
            public int getMaxSize() {
                return CachedData.MAX_SIZE;
            }
        };
    }

    @Override
    public MemcachedClient raw() {
        return memcached;
    }

    @Override
    public Optional<T> get(Object key) {
        return Optional.ofNullable(memcached.get(prefix(key), transcoder));
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<String, T> found = memcached.getBulk(keys.stream().map(this::prefix).collect(Collectors.toList()), transcoder);
        return keys.stream().collect(LinkedHashMap::new, (map, key) -> map.put(key, found.get(prefix(key))), LinkedHashMap::putAll);
    }

    @Override
    public void create(T entity) {
        set(entity, DEFAULT_TTL);
    }

    @Override
    public void set(T entity, Duration ttl) {
        generateKeyIfDefault(entity);
        unchecked(() -> memcached.set(prefixedKeyOf(entity), Math.max((int) ttl.getSeconds(), 1), entity, transcoder).get());
    }

    @Override
    protected Integer getNextAutoIncrementedPK() {
        return (int) memcached.incr(prefix("PK_counter"), 1, 1);
    }

    @Override
    public void create(Collection<T> entities) {
        set(entities, DEFAULT_TTL);
    }

    @Override
    public void set(Collection<T> entities, Duration ttl) {
        entities.forEach(entity -> set(entity, ttl));
    }

    @Override
    public boolean delete(Object key) {
        return unchecked(() -> memcached.delete(prefix(key)).get());
    }

    @Override
    public long delete(Collection<?> keys) {
        return keys.stream().map(this::delete).map(r -> r ? 1L : 0).reduce(Long::sum).orElse(0L);
    }

    public static MemcachedDatasourceConfiguration configure() {
        return new MemcachedDatasourceConfiguration();
    }
}
