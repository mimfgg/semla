package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultPersistsOrMergesOf;
import static io.semla.query.Includes.of;

public class Create<T> extends ContextualQuery<T> {

    private final List<Consumer<T>> setters = new ArrayList<>();

    public Create(PersistenceContext context, EntityModel<T> model) {
        super(context, model);
    }

    public Create<T> with(String name, Object value) {
        setters.add(instance -> model().member(name).setOn(instance, value));
        return this;
    }

    public T create() {
        return create(defaultPersistsOrMergesOf(model())::addTo);
    }

    public T create(UnaryOperator<Includes<T>> includes) {
        return context.create(
                model.newInstance(instance -> setters.forEach(setter -> setter.accept(instance))),
                includes.apply(of(model()))
        );
    }
}
