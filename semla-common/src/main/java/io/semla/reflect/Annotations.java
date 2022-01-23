package io.semla.reflect;

import io.semla.serialization.json.Json;
import io.semla.util.Arrays;
import io.semla.util.Maps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.semla.util.Unchecked.unchecked;
import static java.util.stream.Collectors.joining;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Annotations {

    public static <A extends Annotation> A defaultOf(Class<A> annotation) {
        return proxyOf(annotation, new LinkedHashMap<>());
    }

    public static <A extends Annotation> A proxyOf(Class<A> annotation, Map<String, Object> values) {
        return Proxy.of(annotation, (proxy, method, args) -> switch (method.getName()) {
            case "annotationType" -> annotation;
            case "toString" -> toString(annotation, values);
            case "equals" -> toString(annotation, values).equals(String.valueOf(args[0]));
            default -> values.computeIfAbsent(method.getName(), name -> method.getDefaultValue());
        });
    }

    private static <A extends Annotation> String toString(Class<A> annotation, Map<String, Object> values) {
        if (values.size() == 1 && values.containsKey("value")) {
            return "@" + annotation.getName() + "(" + Json.write(values.get("value")) + ")";
        }
        return "@" + annotation.getName() + "("
                + values.entrySet().stream()
                .map(e -> e.getKey() + "=" + Json.write(e.getValue()))
                .collect(joining(", "))
                + ")";
    }

    public static boolean isAnnotation(Object value) {
        Class<?> clazz = value.getClass();
        return clazz.isAnnotation() || (Types.isAssignableTo(clazz, java.lang.reflect.Proxy.class) && clazz.getInterfaces()[0].isAnnotation());
    }

    public static Map<String, Object> valuesOf(Object value) {
        return Methods.of(value)
                .filter(method -> !Arrays.in(method.getName(), "equals", "toString", "hashCode", "annotationType")) // no method inherited from Annotation
                .filter(method -> method.getParameterCount() == 0) // only getters
                .collect(Maps.collect(Method::getName, method -> unchecked(() -> method.invoke(value))));
    }
}
