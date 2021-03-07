package io.semla.cache;

import io.semla.datasource.Datasource;
import io.semla.datasource.EphemeralKeyValueDatasource;
import io.semla.persistence.CacheEntry;
import io.semla.serialization.json.Json;
import net.jodah.typetools.TypeResolver;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface Cache {

    Duration DEFAULT_TTL = Duration.ofMinutes(5);

    <E> Optional<E> get(String key, Type type);

    default <E> E get(String key, Supplier<E> loader) {
        return get(key, TypeResolver.resolveRawArgument(Supplier.class, loader.getClass()), loader);
    }

    default <E> E get(String key, Type type, Supplier<E> loader) {
        return get(key, type, loader, DEFAULT_TTL);
    }

    default <E> E get(String key, Supplier<E> loader, Duration ttl) {
        return get(key, TypeResolver.resolveRawArgument(Supplier.class, loader.getClass()), loader, ttl);
    }

    default <E> E get(String key, Type type, Supplier<E> loader, Duration ttl) {
        Optional<E> cachedValue = get(key, type);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        E value = loader.get();
        put(key, value, ttl);
        return value;
    }

    Cache evict(String key);

    default Cache put(String key, Object entry) {
        return put(key, entry, DEFAULT_TTL);
    }

    Cache put(String key, Object entry, Duration ttl);

    static Cache of(Datasource<CacheEntry> datasource) {
        return new Cache() {
            @Override
            public <E> Optional<E> get(String key, Type type) {
                return datasource.get(key).map(entry -> {
                    if (entry.isExpired()) {
                        datasource.delete(key);
                        return null;
                    }
                    return Json.read(entry.value, type);
                });
            }

            @Override
            public Cache evict(String key) {
                datasource.delete(key);
                return this;
            }

            @Override
            public Cache put(String key, Object entry, Duration ttl) {
                if (datasource instanceof EphemeralKeyValueDatasource) {
                    ((EphemeralKeyValueDatasource<CacheEntry>) datasource).set(CacheEntry.of(key, Json.write(entry), ttl), ttl);
                } else {
                    datasource.create(CacheEntry.of(key, Json.write(entry), ttl));
                }
                return this;
            }
        };
    }
}
