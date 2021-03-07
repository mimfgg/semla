package io.semla.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

public interface Property<T> {

    Class<T> getDeclaringClass();

    Type getGenericType();

    default <E> Class<E> getType() {
        return Types.rawTypeOf(getGenericType());
    }

    String getName();

    String toGenericString();

    <A extends Annotation> Optional<A> annotation(Class<A> annotationClass);

    default boolean isAssignableTo(Class<?> clazz) {
        return Types.isAssignableTo(getType(), clazz);
    }

    default boolean isAssignableToOneOf(Class<?>... toClasses) {
        return Types.isAssignableToOneOf(getType(), toClasses);
    }

    default boolean isAnnotatedWithOneOf(Class<? extends Annotation>[] annotations) {
        return Stream.of(annotations).anyMatch(annotation -> annotation(annotation).isPresent());
    }

}
