package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;

import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultPersistsOrMergesOf;
import static io.semla.query.Includes.of;

public class Create<T> extends ContextualQuery<T> {

    protected final T entity;

    public Create(PersistenceContext context, T entity) {
        super(context, EntityModel.of(entity));
        this.entity = entity;
    }

    public Create<T> with(String name, Object value) {
        model().member(name).setOn(entity, value);
        return this;
    }

    public T create() {
        return create(defaultPersistsOrMergesOf(model())::addTo);
    }

    public T create(UnaryOperator<Includes<T>> includes) {
        return context.create(entity, includes.apply(of(model())));
    }
}
