package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;

import java.time.Duration;

@SuppressWarnings("unchecked")
public abstract class PaginatedQuery<T, SelfType extends PaginatedQuery<T, ?>> extends FilteredQuery<T, SelfType> {

    protected final Pagination<T> pagination;

    public PaginatedQuery(PersistenceContext context, EntityModel<T> model) {
        this(context, Predicates.of(model), Pagination.of(model));
    }

    protected PaginatedQuery(PersistenceContext context, Predicates<T> predicates, Pagination<T> pagination) {
        super(context, predicates);
        this.pagination = pagination;
    }

    public Predicates<T> predicates() {
        return predicates;
    }

    public SelfType orderedBy(String fieldName) {
        pagination.orderedBy(model().member(fieldName));
        return (SelfType) this;
    }

    public SelfType orderedBy(String fieldName, Pagination.Sort sort) {
        pagination.orderedBy(model().member(fieldName), sort);
        return (SelfType) this;
    }

    public SelfType startAt(int start) {
        pagination.startAt(start);
        return (SelfType) this;
    }

    public SelfType limitTo(int limit) {
        pagination.limitTo(limit);
        return (SelfType) this;
    }

    public SelfType cached() {
        context.cachingStrategy().withCache(true);
        return (SelfType) this;
    }

    public SelfType cachedFor(Duration ttl) {
        context.cachingStrategy().withCache(true).withTtl(ttl);
        return (SelfType) this;
    }

    public SelfType invalidateCache() {
        context.cachingStrategy().invalidateCache(true);
        return (SelfType) this;
    }
}
