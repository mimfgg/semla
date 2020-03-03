package io.semla.reflect;

import io.semla.util.Maps;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static Named named(String name) {
        return proxyOf(Named.class, Maps.of("value", name));
    }
}
