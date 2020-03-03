package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.reflect.Member;

import java.util.Map;

public class Patch<T> extends PaginatedQuery<T, Patch<T>> {

    private final Values<T> values;

    public Patch(PersistenceContext context, EntityModel<T> model) {
        super(context, model);
        this.values = new Values<>(model);
    }

    protected Patch(PersistenceContext context, Predicates<T> predicates, Pagination<T> pagination, Values<T> values) {
        super(context, predicates, pagination);
        this.values = values;
    }

    public Patch<T> set(Map<String, Object> values) {
        values.forEach(this::set);
        return this;
    }

    public Patch<T> set(String name, Object value) {
        Member<T> member = model().member(name);
        if (!model().getColumn(member).updatable()) {
            throw new IllegalArgumentException(member + " is not updatable");
        }
        this.values.put(member, member.unwrap(value));
        return this;
    }

    public long patch() {
        return context.patch(values, predicates, pagination);
    }
}
