package io.semla.reflect;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.semla.model.Model;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.When;
import io.semla.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;


@SuppressWarnings("unchecked")
public interface Getter<T> extends Property<T> {

    <E> E getOn(T instance);

    When serializeWhen();

    String serializeAs();

    default boolean isDefaultOn(T instance) {
        return Model.of(instance).isDefault(this, instance);
    }

    static <T> Getter<T> from(Method method) {
        Optional<Serialize> serialize = Optional.ofNullable(method.getAnnotation(Serialize.class));
        String serializeAs = serialize.map(Serialize::as).filter(Strings::notNullOrEmpty).orElseGet(() -> stripPrefix(method));
        Field field = Fields.getField(method.getDeclaringClass(), serializeAs);
        When serializeWhen = serialize.map(Serialize::value).orElseGet(() -> {
            if (field != null || method.getDeclaringClass().isAnnotation()) {
                return When.ALWAYS;
            }
            return When.NEVER;
        });
        MethodAccess methodAccess = MethodAccess.get(method.getDeclaringClass());
        int index = methodAccess.getIndex(method.getName());
        return new Getter<T>() {
            @Override
            public <E> E getOn(T instance) {
                return (E) methodAccess.invoke(instance, index);
            }

            @Override
            public Class<T> getDeclaringClass() {
                return (Class<T>) method.getDeclaringClass();
            }

            @Override
            public Type getGenericType() {
                return method.getGenericReturnType();
            }

            @Override
            public String getName() {
                return serializeAs;
            }

            @Override
            public String toGenericString() {
                return method.toGenericString();
            }

            @Override
            public When serializeWhen() {
                return serializeWhen;
            }

            @Override
            public String serializeAs() {
                return serializeAs;
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

    static boolean isGetter(Method method) {
        return method.getName().matches("^(get|is)[A-Z].*") && method.getParameterCount() == 0;
    }

    static String stripPrefix(Method method) {
        return Strings.decapitalize(method.getName().replaceFirst("^(?:get|is)([A-Z].*)", "$1"));
    }
}
