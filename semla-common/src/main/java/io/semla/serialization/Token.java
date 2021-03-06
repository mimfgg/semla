package io.semla.serialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static io.semla.reflect.Types.isAssignableTo;


public enum Token {

    NULL, OBJECT, OBJECT_END, PROPERTY, NUMBER, BOOLEAN, STRING, ARRAY, ARRAY_END, END, SKIP;

    private static final Map<Class<?>, Token> tokenByType = new LinkedHashMap<>();

    public static Token fromType(Class<?> type) {
        return tokenByType.computeIfAbsent(type, c -> {
            if (type.equals(String.class) || type.equals(UUID.class)) {
                return STRING;
            } else if (isAssignableTo(type, Number.class)) {
                return NUMBER;
            } else if (isAssignableTo(type, Boolean.class)) {
                return BOOLEAN;
            } else if (type.isArray() || isAssignableTo(type, Collection.class)) {
                return ARRAY;
            }
            return OBJECT;
        });
    }

    public static void register(Class<?> type, Token token) {
        tokenByType.put(type, token);
    }
}
