package io.semla.datasource;

import io.semla.config.SoftKeyValueDatasourceConfiguration;
import io.semla.model.EntityModel;
import io.semla.util.SoftHashMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SoftKeyValueDatasource<T> extends KeyValueDatasource<T> {

    private final Map<Object, T> entities = Collections.synchronizedMap(new SoftHashMap<>());
    private final AtomicInteger primaryKeyCounter = new AtomicInteger();

    public SoftKeyValueDatasource(EntityModel<T> model, String keyspace) {
        super(model, keyspace);
    }

    @Override
    protected Integer getNextAutoIncrementedPK() {
        return primaryKeyCounter.getAndIncrement();
    }

    @Override
    public Map<Object, T> raw() {
        return entities;
    }

    @Override
    public Optional<T> get(Object key) {
        return Optional.ofNullable(entities.get(key));
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<K, T> entitiesByKey = new LinkedHashMap<>();
        keys.forEach(key -> entitiesByKey.put(key, entities.get(key)));
        return entitiesByKey;
    }

    @Override
    public void create(T entity) {
        generateKeyIfDefault(entity);
        entities.put(model().key().member().getOn(entity), EntityModel.copy(entity));
    }

    @Override
    public void create(Collection<T> entities) {
        entities.forEach(this::create);
    }

    @Override
    public boolean delete(Object key) {
        return entities.remove(key) != null;
    }

    @Override
    public long delete(Collection<?> keys) {
        return keys.stream().map(this::delete).map(r -> r ? 1L : 0).reduce(Long::sum).orElse(0L);
    }

    public static SoftKeyValueDatasourceConfiguration configure() {
        return new SoftKeyValueDatasourceConfiguration();
    }
}
