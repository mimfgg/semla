package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.util.Strings;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.semla.model.EntityModel.isEntity;
import static io.semla.reflect.Types.isAssignableTo;

public class Values<T> extends LinkedHashMap<Member<T>, Object> {

    private final EntityModel<T> model;

    public Values(EntityModel<T> model) {
        this.model = model;
    }

    public EntityModel<T> model() {
        return model;
    }

    public Object put(String fieldName, Object value) {
        return put(model.member(fieldName), value);
    }

    @Override
    public Object put(Member<T> member, Object value) {
        if (value != null) {
            if (isEntity(member.getType()) && !isAssignableTo(value.getClass(), member.getType())) {
                value = Strings.parse(String.valueOf(value), member.getType());
            } else if (value instanceof String && !member.getType().equals(String.class)) {
                value = Strings.parse((String) value, member.getType());
            }
            Types.assertIsAssignableTo(value, member.getType());
        }
        return super.put(member, value);
    }

    public Values<T> with(Map<String, Object> values) {
        values.forEach(this::with);
        return this;
    }

    public Values<T> with(String fieldName, Object value) {
        put(fieldName, value);
        return this;
    }

    public T apply(T entity) {
        this.forEach(((member, value) -> member.setOn(entity, value)));
        return entity;
    }

    public static <T> Values<T> of(Class<T> clazz) {
        return new Values<>(EntityModel.of(clazz));
    }
}
