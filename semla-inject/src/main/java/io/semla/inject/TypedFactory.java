package io.semla.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.rawTypeArgumentOf;

public abstract class TypedFactory<T> implements Factory<T> {

    @Override
    public final boolean appliesTo(Type type, Annotation[] annotations) {
        return isAssignableTo(type, rawTypeArgumentOf(this.getClass().getGenericSuperclass())) && annotations.length == 0;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName();
    }
}
