package io.semla.reflect;

import io.semla.model.EntityModel;
import io.semla.serialization.json.Json;
import io.semla.util.Strings;

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

    default <E> E valueOf(String value) {
        return Json.isJson(value) ? Json.read(value, getType()) : Strings.parse(value, getType());
    }

    @SuppressWarnings("unchecked")
    default <E> E unwrap(Object value) {
        Class<E> type = getType();
        if (value != null && !Types.isAssignableTo(value.getClass(), type)) {
            if (value instanceof String) {
                value = valueOf((String) value);
            } else if (EntityModel.isEntity(type)) {
                value = EntityModel.referenceTo(type, value);
            }
        }
        return (E) value;
    }

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
