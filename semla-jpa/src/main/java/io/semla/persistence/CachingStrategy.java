package io.semla.persistence;

import io.semla.cache.Cache;
import io.semla.reflect.Types;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.function.Supplier;

public class CachingStrategy {

    public static Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private boolean invalidateCache;
    private boolean cache;
    private boolean evictCache;
    private Duration ttl = DEFAULT_TTL;

    public CachingStrategy withCache(boolean cache) {
        this.cache = cache;
        return this;
    }

    public CachingStrategy withTtl(Duration ttl) {
        this.ttl = ttl;
        return this;
    }

    public CachingStrategy invalidateCache(boolean invalidateCache) {
        this.invalidateCache = invalidateCache;
        return this;
    }

    public void evictCache() {
        this.evictCache = true;
    }

    public boolean applies() {
        return cache || invalidateCache || evictCache;
    }

    public <E> E applyTo(Cache cache, Supplier<String> keySupplier, Type type, Supplier<E> loader) {
        String key = keySupplier.get();
        if (evictCache || invalidateCache) {
            cache.evict(key);
            if (evictCache) {
                return Types.safeNull(type, null);
            }
        }
        return cache.get(key, type, loader, ttl);
    }

    public <E> E ifApplicable(Supplier<Cache> cacheSupplier, Supplier<String> keySupplier, Type type, Supplier<E> loader) {
        if (applies()) {
            return applyTo(cacheSupplier.get(), keySupplier, type, loader);
        }
        return loader.get();
    }
}
