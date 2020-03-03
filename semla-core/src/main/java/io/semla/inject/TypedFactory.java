package io.semla.inject;

import io.semla.reflect.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public abstract class TypedFactory<T> implements Factory<T> {

    @Override
    public final boolean appliesTo(Type type, Annotation[] annotations) {
        return Types.isAssignableTo(type, Types.typeArgumentOf(this.getClass().getGenericSuperclass())) && annotations.length == 0;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName();
    }
}
