package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CachedDatasource<T> extends Datasource<T> {

    private final Datasource<T> cache;
    private final Datasource<T> datasource;

    public CachedDatasource(EntityModel<T> model, Datasource<T> cache, Datasource<T> datasource) {
        super(model);
        this.cache = cache;
        this.datasource = datasource;
    }

    @Override
    public Pair<Datasource<T>, Datasource<T>> raw() {
        return Pair.of(cache, datasource);
    }

    @Override
    public Optional<T> get(Object key) {
        Optional<T> cached = cache.get(key);
        if (!cached.isPresent()) {
            Optional<T> persisted = datasource.get(key);
            persisted.ifPresent(cache::create);
            return persisted;
        }
        return cached;
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<K, T> hits = cache.get(keys);
        List<K> misses = keys.stream().filter(key -> hits.get(key) == null).collect(Collectors.toList());
        if (!misses.isEmpty()) {
            Map<K, T> persisted = datasource.get(misses);
            persisted.forEach((key, value) -> {
                if (value != null) {
                    cache.create(value);
                    hits.put(key, value);
                }
            });
        }
        return hits;
    }

    @Override
    public void create(T entity) {
        datasource.create(entity);
        cache.create(entity);
    }

    @Override
    public void create(Collection<T> entities) {
        datasource.create(entities);
        cache.create(entities);
    }

    @Override
    public void update(T entity) {
        datasource.update(entity);
        cache.update(entity);
    }

    @Override
    public void update(Collection<T> entities) {
        datasource.update(entities);
        cache.update(entities);
    }

    @Override
    public boolean delete(Object key) {
        cache.delete(key);
        return datasource.delete(key);
    }

    @Override
    public long delete(Collection<?> keys) {
        cache.delete(keys);
        return datasource.delete(keys);
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return datasource.first(predicates, pagination).map(entity -> {
            cache.update(entity);
            return entity;
        });
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        List<T> entities = datasource.list(predicates, pagination);
        cache.update(entities);
        return entities;
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        long patched;
        if (cache instanceof KeyValueDatasource) {
            List<Object> keys = datasource.list(predicates, pagination).stream().map(EntityModel::keyOf).collect(Collectors.toList());
            patched = datasource.patch(values, predicates, pagination);
            cache.update(datasource.get(keys).values());
        } else {
            patched = datasource.patch(values, predicates, pagination);
            cache.patch(values, predicates, pagination);
        }
        return patched;
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        List<Object> keys = datasource.list(predicates, pagination).stream().map(EntityModel::keyOf).collect(Collectors.toList());
        if (!keys.isEmpty()) {
            cache.delete(keys);
            datasource.delete(keys);
        }
        return keys.size();
    }

    @Override
    public long count(Predicates<T> predicates) {
        return datasource.count(predicates);
    }

    public static CachedDatasource.Configuration configure() {
        return new CachedDatasource.Configuration();
    }

    @TypeName("cached")
    public static class Configuration implements Datasource.Configuration {

        private Datasource.Configuration cache;
        private Datasource.Configuration datasource;

        @Deserialize
        public Datasource.Configuration cache() {
            return cache;
        }

        @Serialize
        public io.semla.datasource.CachedDatasource.Configuration withCache(Datasource.Configuration cache) {
            this.cache = cache;
            return this;
        }

        @Deserialize
        public Datasource.Configuration datasource() {
            return datasource;
        }

        @Serialize
        public io.semla.datasource.CachedDatasource.Configuration withDatasource(Datasource.Configuration datasource) {
            this.datasource = datasource;
            return this;
        }

        @Override
        public <T> CachedDatasource<T> create(EntityModel<T> model) {
            return new CachedDatasource<>(model, cache.create(model), datasource.create(model));
        }
    }
}
