package io.semla.reflect;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.When;
import io.semla.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.semla.reflect.Modifier.PRIVATE;

@SuppressWarnings("unchecked")
public class Member<T> implements Getter<T>, Setter<T> {

    private final String name;
    private final Function<T, Object> get;
    private final BiConsumer<T, Object> set;
    private final Class<T> declaringClass;
    private final Type genericType;
    private final String genericString;
    private final Function<Class<? extends Annotation>, ? extends Annotation> getAnnotation;
    private final When deserializeWhen;
    private final String deserializeFrom;
    private final When serializeWhen;
    private final String serializeAs;

    private Member(String name,
                   Function<T, Object> get,
                   BiConsumer<T, Object> set,
                   Class<T> declaringClass,
                   Type genericType,
                   String genericString,
                   Function<Class<? extends Annotation>, ? extends Annotation> getAnnotation) {
        this.name = name;
        this.get = get;
        this.set = set;
        this.declaringClass = declaringClass;
        this.genericType = genericType;
        this.genericString = genericString;
        this.getAnnotation = getAnnotation;
        Optional<Deserialize> deserialize = annotation(Deserialize.class);
        deserializeWhen = deserialize.map(Deserialize::value).orElse(When.ALWAYS);
        deserializeFrom = deserialize.map(Deserialize::from).filter(Strings::notNullOrEmpty).orElse(name);
        Optional<Serialize> serialize = annotation(Serialize.class);
        serializeWhen = serialize.map(Serialize::value).orElse(When.ALWAYS);
        serializeAs = serialize.map(Serialize::as).filter(Strings::notNullOrEmpty).orElse(name);
    }

    @Override
    public <E> E getOn(T instance) {
        return (E) get.apply(instance);
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
    public T setOn(T host, Object value) {
        set.accept(host, Types.unwrap(getType(), value));
        return host;
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
    public Class<T> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toGenericString() {
        return genericString;
    }

    @Override
    public String toString() {
        return getDeclaringClass().getCanonicalName() + "." + getName();
    }

    @Override
    public <A extends Annotation> Optional<A> annotation(Class<A> annotationClass) {
        return Optional.ofNullable((A) getAnnotation.apply(annotationClass));
    }

    public static <T> Member<T> from(Field field) {
        Function<T, Object> get;
        BiConsumer<T, Object> set;
        if (Modifier.not(field, PRIVATE)) {
            FieldAccess fieldAccess = FieldAccess.get(field.getDeclaringClass());
            int index = fieldAccess.getIndex(field);
            get = host -> fieldAccess.get(host, index);
            set = (host, value) -> fieldAccess.set(host, index, value);
        } else {
            get = host -> Fields.getValue(host, field);
            set = (host, value) -> Fields.setValue(host, field, value);
        }
        return new Member<>(
                field.getName(),
                get,
                set,
                (Class<T>) field.getDeclaringClass(),
                field.getGenericType(),
                field.toGenericString(),
                field::getAnnotation
        );
    }

    public static <T> Member<T> from(Field field, Method getter, Method setter) {
        Function<T, Object> get;
        BiConsumer<T, Object> set;
        Function<Class<? extends Annotation>, ? extends Annotation> getAnnotation = annotationClass -> {
            if (getter != null && getter.isAnnotationPresent(annotationClass)) {
                return getter.getAnnotation(annotationClass);
            }
            if (setter != null && setter.isAnnotationPresent(annotationClass)) {
                return setter.getAnnotation(annotationClass);
            }
            return field.getAnnotation(annotationClass);
        };
        Member<T> member = Member.from(field);
        MethodAccess methodAccess = MethodAccess.get(member.getDeclaringClass());
        if (getter != null) {
            int getIndex = methodAccess.getIndex(getter.getName());
            get = host -> methodAccess.invoke(host, getIndex);
        } else {
            get = member::getOn;
        }
        if (setter != null) {
            int setIndex = methodAccess.getIndex(setter.getName());
            set = (host, value) -> methodAccess.invoke(host, setIndex, value);
        } else {
            set = member::setOn;
        }
        return new Member<>(
                field.getName(),
                get,
                set,
                (Class<T>) field.getDeclaringClass(),
                field.getGenericType(),
                field.toGenericString(),
                getAnnotation
        );
    }
}
