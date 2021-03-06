package io.semla.reflect;

import java.lang.reflect.Type;

import static io.semla.reflect.Types.rawTypeOf;
import static io.semla.reflect.Types.typeArgumentOf;

public abstract class TypeReference<E> {

    public Type getType() {
        return typeArgumentOf(this.getClass().getGenericSuperclass());
    }

    public Class<E> getRawType() {
        return rawTypeOf(getType());
    }
}
