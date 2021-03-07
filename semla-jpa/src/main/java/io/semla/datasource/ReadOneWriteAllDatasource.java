package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;

import java.util.*;
import java.util.stream.Collectors;

public class ReadOneWriteAllDatasource<T> extends Datasource<T> {

    private final List<Datasource<T>> datasources;
    private final Random random = new Random();

    public ReadOneWriteAllDatasource(EntityModel<T> model, List<Datasource<T>> datasources) {
        super(model);
        this.datasources = datasources;
    }

    @Override
    public List<Datasource<T>> raw() {
        return datasources;
    }

    @Override
    public Optional<T> get(Object key) {
        return datasource().get(key);
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        return datasource().get(keys);
    }

    @Override
    public void create(T entity) {
        datasources.forEach(datasource -> datasource.create(entity));
    }

    @Override
    public void create(Collection<T> entities) {
        datasources.forEach(datasource -> datasource.create(entities));
    }

    @Override
    public void update(T entity) {
        datasources.forEach(datasource -> datasource.update(entity));
    }

    @Override
    public void update(Collection<T> entities) {
        datasources.forEach(datasource -> datasource.update(entities));
    }

    @Override
    public boolean delete(Object key) {
        return datasources.stream().map(datasource -> datasource.delete(key)).reduce(Boolean::logicalOr).orElse(false);
    }

    @Override
    public long delete(Collection<?> keys) {
        return datasources.stream().map(datasource -> datasource.delete(keys)).reduce(Math::max).orElse(0L);
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return datasource().first(predicates, pagination);
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        return datasource().list(predicates, pagination);
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return datasources.parallelStream()
                .map(datasource -> datasource.patch(values, predicates, pagination))
                .reduce(Long::max)
                .orElse(0L);
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        return datasources.stream()
                .map(datasource -> datasource.delete(predicates, pagination))
                .reduce(Long::max)
                .orElse(0L);
    }

    @Override
    public long count(Predicates<T> predicates) {
        return datasource().count(predicates);
    }

    private Datasource<T> datasource() {
        return datasources.get(random.nextInt(datasources.size()));
    }

    public static ReadOneWriteAllDatasource.Configuration configure() {
        return new ReadOneWriteAllDatasource.Configuration();
    }

    @TypeName("read-one-write-all")
    public static class Configuration implements Datasource.Configuration {

        private final List<Datasource.Configuration> datasources = new ArrayList<>();

        @Serialize
        public List<Datasource.Configuration> datasources() {
            return datasources;
        }

        @Deserialize
        public Configuration withDatasources(Datasource.Configuration... datasources) {
            this.datasources.addAll(Arrays.asList(datasources));
            return this;
        }

        @Override
        public <T> ReadOneWriteAllDatasource<T> create(EntityModel<T> model) {
            return new ReadOneWriteAllDatasource<>(model, datasources.stream().map(conf -> conf.create(model)).collect(Collectors.toList()));
        }

        @Override
        public void close() {
            datasources.forEach(Datasource.Configuration::close);
        }
    }
}
