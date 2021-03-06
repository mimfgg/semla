package io.semla.reflect;

import io.semla.util.Arrays;
import io.semla.util.Maps;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.util.Unchecked.unchecked;

public class Annotations {

    private Annotations() {}

    public static <A extends Annotation> A defaultOf(Class<A> annotation) {
        return proxyOf(annotation, new LinkedHashMap<>());
    }

    public static <A extends Annotation> A proxyOf(Class<A> annotation, Map<String, Object> values) {
        return Proxy.of(annotation, (proxy, method, args) -> {
            switch (method.getName()) {
                case "annotationType":
                    return annotation;
                case "toString":
                    return toString(annotation, values);
                case "equals":
                    return toString(annotation, values).equals(String.valueOf(args[0]));
                default:
                    return values.computeIfAbsent(method.getName(), name -> method.getDefaultValue());
            }
        });
    }

    private static <A extends Annotation> String toString(Class<A> annotation, Map<String, Object> values) {
        return "@" + annotation.getName()
            + "(" + values.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")) + ")";
    }

    public static boolean isAnnotation(Object value) {
        Class<?> clazz = value.getClass();
        return clazz.isAnnotation() || (Types.isAssignableTo(clazz, java.lang.reflect.Proxy.class) && clazz.getInterfaces()[0].isAnnotation());
    }

    public static Map<String, Object> valuesOf(Object value) {
        return Stream.of(value.getClass().getDeclaredMethods())
            .filter(method -> !Arrays.in(method.getName(), "equals", "toString", "hashCode", "annotationType")) // no method inherited from Annotation
            .collect(Maps.collect(Method::getName, method -> unchecked(() -> method.invoke(value))));
    }
}
