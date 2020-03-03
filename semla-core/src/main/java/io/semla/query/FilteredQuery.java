package io.semla.query;

import io.semla.persistence.PersistenceContext;

@SuppressWarnings("unchecked")
public abstract class FilteredQuery<T, SelfType extends FilteredQuery<T, ?>> extends ContextualQuery<T> {

    protected final Predicates<T> predicates;

    protected FilteredQuery(PersistenceContext context, Predicates<T> predicates) {
        super(context, predicates.model());
        this.predicates = predicates;
    }

    public SelfType where(Predicates<T> predicates) {
        this.predicates.add(predicates);
        return (SelfType) this;
    }

    public Predicates<T>.Handler<SelfType> where(String fieldName) {
        return predicates.where((SelfType) this, fieldName);
    }

    public Predicates<T>.Handler<SelfType> and(String fieldName) {
        return where(fieldName);
    }
}
