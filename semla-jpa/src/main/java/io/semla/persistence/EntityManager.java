package io.semla.persistence;

import io.semla.datasource.Datasource;
import io.semla.query.*;

import javax.persistence.PostLoad;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultRemovesOrDeleteOf;
import static io.semla.query.Includes.of;

public class EntityManager<T> extends AbstractEntityManager<T> {

    public EntityManager(Datasource<T> datasource, EntityManagerFactory entityManagerFactory) {
        super(datasource, entityManagerFactory);
    }

    public Get<T> cached() {
        return new Get<>(newContext(), model()).cached();
    }

    public Get<T> cachedFor(Duration ttl) {
        return new Get<>(newContext(), model()).cachedFor(ttl);
    }

    public Get<T> invalidateCache() {
        return new Get<>(newContext(), model()).invalidateCache();
    }

    public Get<T>.Evict evictCache() {
        return new Get<>(newContext(), model()).evictCache();
    }

    public Create<T> newInstance() {
        return new Create<>(newContext(), model());
    }

    public Predicates<T>.Handler<Select<T>> where(String fieldName) {
        return new Select<>(newContext(), model()).where(fieldName);
    }

    public Select<T> where(Predicates<T> predicates) {
        return new Select<>(newContext(), model()).where(predicates);
    }

    public Select<T> orderedBy(String fieldName) {
        return new Select<>(newContext(), model()).orderedBy(fieldName);
    }

    public Select<T> orderedBy(String fieldName, Pagination.Sort sort) {
        return new Select<>(newContext(), model()).orderedBy(fieldName, sort);
    }

    public Select<T> limitTo(int limit) {
        return new Select<>(newContext(), model()).limitTo(limit);
    }

    public Select<T> startAt(int start) {
        return new Select<>(newContext(), model()).startAt(start);
    }

    public Patch<T> set(Map<String, Object> values) {
        return new Patch<>(newContext(), model()).set(values);
    }

    public Patch<T> set(String fieldName, Object value) {
        return new Patch<>(newContext(), model()).set(fieldName, value);
    }

    public T create(T entity, UnaryOperator<Includes<T>> include) {
        return create(newContext(), entity, include.apply(of(model())));
    }

    public <CollectionType extends Collection<T>> CollectionType create(CollectionType entities, UnaryOperator<Includes<T>> include) {
        return create(newContext(), entities, include.apply(of(model())));
    }

    public Optional<T> get(Object key, UnaryOperator<Includes<T>> include) {
        return get(newContext(), key, include.apply(of(model())));
    }

    public <K> Map<K, T> get(Collection<K> keys, UnaryOperator<Includes<T>> include) {
        return get(newContext(), keys, include.apply(of(model())));
    }

    public T update(T entity, UnaryOperator<Includes<T>> include) {
        return update(newContext(), entity, include.apply(of(model())));
    }

    public <CollectionType extends Collection<T>> CollectionType update(CollectionType entities, UnaryOperator<Includes<T>> include) {
        return update(newContext(), entities, include.apply(of(model())));
    }

    public boolean delete(Object key, UnaryOperator<Includes<T>> include) {
        return delete(newContext(), key, include.apply(of(model())));
    }

    public long delete(Collection<?> keys, UnaryOperator<Includes<T>> include) {
        return delete(newContext(), keys, include.apply(defaultRemovesOrDeleteOf(model())));
    }

    public Optional<T> first() {
        return first(UnaryOperator.identity());
    }

    public Optional<T> first(UnaryOperator<Includes<T>> include) {
        return new Select<>(newContext(), model()).first(include);
    }

    protected Optional<T> first(PersistenceContext context, Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        enforceIndicesIfNeeded(predicates);
        return execute(() -> Query.first(predicates, pagination, includes), () ->
            datasource.first(predicates, pagination)
                .map(context.entityContext()::remapOrCache)
                .map(entity -> includes.fetchOn(entity, context))
                .map(entity -> {
                    entityManagerFactory.injector().inject(entity);
                    return invokeListener(entity, PostLoad.class);
                }));
    }

    public List<T> list() {
        return list(UnaryOperator.identity());
    }

    public List<T> list(UnaryOperator<Includes<T>> include) {
        return new Select<>(newContext(), model()).list(include);
    }

    protected List<T> list(PersistenceContext context, Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        enforceIndicesIfNeeded(predicates);
        return execute(() -> Query.list(predicates, pagination, includes), () -> {
                List<T> list = includes.fetchOn(context.entityContext().remapOrCache(datasource.list(predicates, pagination)), context);
                list.forEach(entity -> invokeListener(entityManagerFactory.injector().inject(entity), PostLoad.class));
                return list;
            }
        );
    }

    protected long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        enforceIndicesIfNeeded(predicates);
        return execute(() -> Query.patch(values, predicates, pagination), () -> datasource.patch(values, predicates, pagination));
    }

    protected long delete(PersistenceContext context, Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        enforceIndicesIfNeeded(predicates);
        return execute(() -> Query.delete(predicates, pagination, includes), () -> {
                model().relations().forEach(relation -> addDetachIfMissing(includes, relation));
                if (!includes.relations().isEmpty()) {
                    List<T> entities = list(context, predicates, pagination, Includes.of(model()));
                    if (!entities.isEmpty()) {
                        includes.deleteOn(entities, context);
                    }
                }
                return datasource.delete(predicates, pagination);
            }
        );
    }

    protected long count(Predicates<T> predicates) {
        enforceIndicesIfNeeded(predicates);
        return execute(() -> Query.count(predicates), () -> datasource.count(predicates));
    }

    protected void enforceIndicesIfNeeded(Predicates<T> predicates) {
        if (strictIndices) {
            predicates.enforceIndices();
        }
    }
}
