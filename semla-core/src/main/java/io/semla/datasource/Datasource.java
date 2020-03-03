package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class Datasource<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EntityModel<T> model;

    public Datasource(EntityModel<T> model) {
        this.model = model;
    }

    public abstract Object raw();

    public EntityModel<T> model() {
        return model;
    }

    protected EntityExistsException alreadyExists(Object key) {
        return new EntityExistsException("entity '" + model().singularName() + "' with key '" + key + "' already exist!");
    }

    protected EntityNotFoundException notFound(Object key) {
        return new EntityNotFoundException("entity '" + model().singularName() + "' with key '" + key + "' doesn't exist!");
    }

    public abstract Optional<T> get(Object key);

    public abstract <K> Map<K, T> get(Collection<K> keys);

    public abstract void create(T entity);

    public abstract void create(Collection<T> entities);

    public abstract void update(T entity);

    public abstract void update(Collection<T> entities);

    public abstract boolean delete(Object key);

    public abstract long delete(Collection<?> keys);

    public final Optional<T> first() {
        return first(Predicates.of(model().getType()));
    }

    public final Optional<T> first(Predicates<T> predicates) {
        return first(predicates, Pagination.of(model().getType()));
    }

    public final Optional<T> first(Pagination<T> pagination) {
        return first(Predicates.of(model().getType()), pagination);
    }

    public abstract Optional<T> first(Predicates<T> predicates, Pagination<T> pagination);

    public final List<T> list() {
        return list(Predicates.of(model().getType()));
    }

    public final List<T> list(Predicates<T> predicates) {
        return list(predicates, Pagination.of(model().getType()));
    }

    public final List<T> list(Pagination<T> pagination) {
        return list(Predicates.of(model().getType()), pagination);
    }

    public abstract List<T> list(Predicates<T> predicates, Pagination<T> pagination);

    public final long patch(Values<T> values, Predicates<T> predicates) {
        return patch(values, predicates, Pagination.of(model().getType()));
    }

    public abstract long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination);

    public final long deleteAll() {
        return delete(Predicates.of(model().getType()));
    }

    public long delete(Predicates<T> predicates) {
        return delete(predicates, Pagination.of(model().getType()));
    }

    public abstract long delete(Predicates<T> predicates, Pagination<T> pagination);

    public final long count() {
        return count(Predicates.of(model().getType()));
    }

    public abstract long count(Predicates<T> predicates);
}
