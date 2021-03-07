package io.semla.inject;

import io.semla.reflect.TypeReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


public interface Injector {

    default <E> E getInstance(Class<E> clazz, Annotation... annotations) {
        return getInstance((Type) clazz, annotations);
    }

    default <E> E getInstance(TypeReference<E> typeReference, Annotation... annotations) {
        return getInstance(typeReference.getType(), annotations);
    }

    <E> E getInstance(Type type, Annotation... annotations);

    <E> E inject(E instance);
}
