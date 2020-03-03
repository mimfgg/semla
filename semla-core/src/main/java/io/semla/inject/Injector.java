package io.semla.inject;

import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


public interface Injector {

    default <E> E getInstance(Class<E> clazz, Annotation... annotations) {
        return getInstance((Type) clazz, annotations);
    }

    default <E> E getInstance(TypeLiteral<E> typeLiteral, Annotation... annotations) {
        return getInstance(typeLiteral.getType(), annotations);
    }

    <E> E getInstance(Type type, Annotation... annotations);

    <E> E inject(E instance);
}
