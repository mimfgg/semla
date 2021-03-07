package io.semla.reflect;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.When;
import io.semla.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

@SuppressWarnings("unchecked")
public interface Setter<T> extends Property<T> {

    T setOn(T host, Object value);

    When deserializeWhen();

    String deserializeFrom();

    static <T> Setter<T> from(Method method) {
        Optional<Deserialize> deserialize = Optional.ofNullable(method.getAnnotation(Deserialize.class));
        String deserializeFrom = deserialize.map(Deserialize::from).filter(Strings::notNullOrEmpty).orElseGet(() -> stripPrefix(method));
        Field field = Fields.getField(method.getDeclaringClass(), deserializeFrom);
        When deserializeWhen = deserialize.map(Deserialize::value).orElseGet(() -> field != null ? When.ALWAYS : When.NEVER);
        MethodAccess methodAccess = MethodAccess.get(method.getDeclaringClass());
        int index = methodAccess.getIndex(method.getName(), 1);
        return new Setter<T>() {
            @Override
            public T setOn(T host, Object value) {
                methodAccess.invoke(host, index, (Object) Types.unwrap(getType(), value));
                return host;
            }

            @Override
            public Class<T> getDeclaringClass() {
                return (Class<T>) method.getDeclaringClass();
            }

            @Override
            public Type getGenericType() {
                return method.getGenericParameterTypes()[0];
            }

            @Override
            public String getName() {
                return deserializeFrom;
            }

            @Override
            public String toGenericString() {
                return method.toGenericString();
            }

            @Override
            public When deserializeWhen() {
                return deserializeWhen;
            }

            @Override
            public String deserializeFrom() {
                return deserializeFrom;
            }

            @Override
            public <A extends Annotation> Optional<A> annotation(Class<A> annotationClass) {
                return Optional.ofNullable(method.getAnnotation(annotationClass));
            }

            @Override
            public String toString() {
                return getDeclaringClass().getCanonicalName() + "." + getName();
            }
        };
    }

    static boolean isSetter(Method method) {
        return method.getName().matches("^(set|with)[A-Z].*") && method.getParameterCount() == 1;
    }

    static String stripPrefix(Method method) {
        return Strings.decapitalize(method.getName().replaceFirst("^(?:set|with)([A-Z].*)", "$1"));
    }
}
