package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.json.Json;
import io.semla.util.Lists;
import io.semla.util.Singleton;
import io.semla.util.Splitter;
import net.spy.memcached.*;
import net.spy.memcached.transcoders.Transcoder;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static io.semla.util.Unchecked.unchecked;

public class MemcachedDatasource<T> extends EphemeralKeyValueDatasource<T> {

    public static final Duration DEFAULT_TTL = Duration.ofHours(3);
    private final MemcachedClient memcached;
    private final Transcoder<T> transcoder;

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

    public static MemcachedDatasource.Configuration configure() {
        return new MemcachedDatasource.Configuration();
    }

    @TypeName("memcached")
    public static class Configuration extends KeyValueDatasource.Configuration<MemcachedDatasource.Configuration> {

        public static final int DEFAULT_PORT = 11211;

        private final List<String> hosts = new ArrayList<>();

        private final Singleton<MemcachedClient> client = Singleton.lazy(() -> {
            ConnectionFactory cf = new ConnectionFactoryBuilder()
                .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                .setDaemon(true)
                .setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT)
                .setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
                .setFailureMode(FailureMode.Cancel)
                .build();
            return unchecked(() -> new MemcachedClient(cf,
                    hosts.stream().map(host -> {
                        if (host.matches(".*:[0-9]{1,5}")) {
                            return Splitter.on(':').split(host).map(splitted -> new InetSocketAddress(splitted.get(0), Integer.parseInt(splitted.get(1))));
                        } else {
                            return new InetSocketAddress(host, DEFAULT_PORT);
                        }
                    }).collect(Collectors.toList())
                )
            );
        });

        @Serialize
        public List<String> hosts() {
            return hosts;
        }

        @Deserialize
        public Configuration withHosts(String... hosts) {
            this.hosts.addAll(Lists.fromArray(hosts));
            return this;
        }

        @Override
        public <T> MemcachedDatasource<T> create(EntityModel<T> entityModel) {
            return new MemcachedDatasource<>(entityModel, client.get(), keyspace());
        }

        @Override
        public void close() {
            client().shutdown();
            client.reset();
        }

        public MemcachedClient client() {
            return client.get();
        }
    }
}
