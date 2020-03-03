package io.semla.datasource;

import io.semla.config.ReadOneWriteAllDatasourceConfiguration;
import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;

import java.util.*;

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

    public static ReadOneWriteAllDatasourceConfiguration configure() {
        return new ReadOneWriteAllDatasourceConfiguration();
    }
}
