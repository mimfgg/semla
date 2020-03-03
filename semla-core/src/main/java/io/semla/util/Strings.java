package io.semla.util;

import io.semla.model.EntityModel;
import io.semla.reflect.Getter;
import io.semla.reflect.Methods;

import java.lang.reflect.Member;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.model.EntityModel.isEntity;
import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.isAssignableToOneOf;


@SuppressWarnings("unchecked")
public class Strings {

    private Strings() {}

    private static final Map<Class<?>, BiFunction<Object, Set<Object>, String>> stringifiers = new LinkedHashMap<>();
    private static final Map<Class<?>, Function<String, ?>> parsers = new LinkedHashMap<>();

    public static int getClosingBracketIndex(String value, int start, char opening, char closing) {
        int bracketDepth = 0;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == opening) {
                bracketDepth++;
            } else if (c == closing) {
                if (--bracketDepth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String prefixIfNotNullOrEmpty(String prefix, String value) {
        if (value != null && value.length() > 0) {
            return prefix + value;
        }
        return value;
    }

    public static String defaultIfEmptyOrNull(String value, String defaultValue) {
        return value != null && !value.equals("") ? value : defaultValue;
    }

    public static String decapitalize(String property) {
        switch (property.length()) {
            case 0:
                return property;
            case 1:
                return property.toLowerCase();
            default:
                return property.substring(0, 1).toLowerCase() + property.substring(1);
        }
    }

    public static String capitalize(String property) {
        switch (property.length()) {
            case 0:
                return property;
            case 1:
                return property.toUpperCase();
            default:
                return property.substring(0, 1).toUpperCase() + property.substring(1);
        }
    }

    public static String toString(Object object) {
        return toString(object, new HashSet<>());
    }

    private static <T> String toString(Object object, Set<Object> printed) {
        if (object == null) {
            return "null";
        }
        return stringifiers.computeIfAbsent(object.getClass(), clazz -> {
            if (clazz.isArray()) {
                return (o, p) -> toString(Lists.fromArray(o), p);
            } else if (isAssignableTo(clazz, Collection.class)) {
                return (o, p) -> {
                    StringBuilder builder = new StringBuilder();
                    Collection<?> collection = (Collection<?>) o;
                    builder.append('[');
                    if (!collection.isEmpty()) {
                        collection.forEach(element -> builder.append(toString(element, p)).append(", "));
                        builder.delete(builder.length() - 2, builder.length());
                    }
                    builder.append(']');
                    return builder.toString();
                };
            } else if (isAssignableTo(clazz, Map.class)) {
                return (o, p) -> {
                    StringBuilder builder = new StringBuilder();
                    Map<?, ?> map = (Map<?, ?>) o;
                    builder.append('{');
                    if (!map.isEmpty()) {
                        map.forEach((key, value) -> builder.append(toString(key, p)).append(": ").append(toString(value, p)).append(", "));
                        builder.delete(builder.length() - 2, builder.length());
                    }
                    builder.append('}');
                    return builder.toString();
                };
            } else if (clazz.equals(Optional.class)) {
                return (o, p) -> toString(((Optional<?>) o).orElse(null), p);
            } else if (isEntity(clazz)) {
                return (o, p) -> {
                    EntityModel<T> model = EntityModel.of((T) o);
                    StringBuilder builder = new StringBuilder();
                    if (!p.add(o) || EntityModel.isReference(o)) {
                        builder.append(toString(model.key().member().getOn((T) o), p));
                    } else {
                        builder.append("{");
                        if (!model.members().isEmpty()) {
                            model.members().forEach(getter ->
                                builder.append(getter.getName()).append(": ").append(toString(((Getter<Object>) getter).getOn(o), p)).append(", ")
                            );
                            builder.delete(builder.length() - 2, builder.length());
                        }
                        builder.append("}");
                    }
                    return builder.toString();
                };
            } else if (clazz.equals(Date.class)) {
                return (o, p) -> DateTimeFormatter.ISO_INSTANT.format(((Date) o).toInstant());
            } else if (isAssignableTo(clazz, Calendar.class)) {
                return (o, p) -> DateTimeFormatter.ISO_INSTANT.format(((Calendar) o).toInstant());
            } else if (clazz.equals(Timestamp.class)) {
                return (o, p) -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(((Timestamp) o).toLocalDateTime());
            } else if (isAssignableToOneOf(clazz, String.class)) {
                return (o, p) -> (String) o;
            } else if (isAssignableToOneOf(clazz, java.sql.Date.class, Time.class, Temporal.class, UUID.class)) {
                return (o, p) -> String.valueOf(o);
            } else if (isAssignableTo(clazz, Member.class)) {
                return (o, p) -> ((Member) o).getName();
            }
            return (o, p) -> String.valueOf(o);
        }).apply(object, printed);
    }

    public static <T> T parse(String value, Class<T> clazz) {
        return (T) parsers.computeIfAbsent(clazz, c -> {
            if (clazz.equals(String.class)) {
                return UnaryOperator.identity();
            } else if (isAssignableTo(clazz, Byte.class)) {
                return Byte::valueOf;
            } else if (isAssignableTo(clazz, Boolean.class)) {
                return Boolean::valueOf;
            } else if (clazz.equals(Number.class)) {
                return s -> {
                    if (s.matches("\\d+")) {
                        return Integer.valueOf(s);
                    }
                    return Double.valueOf(s);
                };
            } else if (isAssignableTo(clazz, Short.class)) {
                return Short::valueOf;
            } else if (isAssignableTo(clazz, Integer.class)) {
                return Integer::valueOf;
            } else if (isAssignableTo(clazz, Float.class)) {
                return Float::valueOf;
            } else if (isAssignableTo(clazz, Long.class)) {
                return Long::valueOf;
            } else if (isAssignableTo(clazz, Double.class)) {
                return Double::valueOf;
            } else if (isAssignableTo(clazz, Character.class)) {
                return s -> {
                    if (s == null) {
                        return null;
                    }
                    if (s.length() == 1) {
                        return (T) (Character) s.charAt(0);
                    }
                    throw new IllegalArgumentException("cannot parse \"" + s + "\" into a char");
                };
            } else if (clazz.equals(BigInteger.class)) {
                return BigInteger::new;
            } else if (clazz.equals(BigDecimal.class)) {
                return BigDecimal::new;
            } else if (isEntity(clazz)) {
                return s -> EntityModel.of(clazz).newInstanceFromKey(parse(s, EntityModel.of(clazz).key().member().getType()));
            } else if (isAssignableTo(clazz, Calendar.class)) {
                return s -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(DateTimeFormatter.ISO_INSTANT.parse(s, Instant::from).toEpochMilli());
                    return calendar;
                };
            } else if (clazz.equals(Date.class)) {
                return s -> new Date(DateTimeFormatter.ISO_INSTANT.parse(s, Instant::from).toEpochMilli());
            } else if (clazz.equals(java.sql.Date.class)) {
                return java.sql.Date::valueOf;
            } else if (clazz.equals(java.sql.Time.class)) {
                return java.sql.Time::valueOf;
            } else if (clazz.equals(Instant.class)) {
                return s -> DateTimeFormatter.ISO_INSTANT.parse(s, Instant::from);
            } else if (clazz.equals(Timestamp.class)) {
                return s -> Timestamp.valueOf(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(s, LocalDateTime::from));
            } else if (clazz.equals(LocalDateTime.class)) {
                return s -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(s, LocalDateTime::from);
            } else if (clazz.isEnum()) {
                return s -> Methods.invoke(clazz, "valueOf", s);
            } else if (clazz.equals(UUID.class)) {
                return UUID::fromString;
            } else if (clazz.isArray()) {
                // naive parsing
                return s -> Arrays.toArray(Splitter.on(',').trim().omitEmptyStrings().split(s.substring(1, s.length() - 1)).stream()
                        .map(token -> parse(token, clazz.getComponentType())).collect(Collectors.toList()),
                    clazz.getComponentType());
            }
            return UnaryOperator.identity();
        }).apply(value);
    }

    public static <T> ParserHandler<T> parseType(Class<T> clazz) {
        return new ParserHandler<>(clazz);
    }

    public static <T> WriterHandler<T> writeType(Class<T> clazz) {
        return new WriterHandler<>(clazz);
    }

    public static String toSnakeCase(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ' ') {
                output.append('_');
            } else {
                if (Character.isUpperCase(c)) {
                    if (i > 0) {
                        output.append('_');
                    }
                    output.append(Character.toLowerCase(c));
                } else {
                    output.append(c);
                }
            }
        }
        return output.toString();
    }

    public static String toCamelCaseCase(String input) {
        StringBuilder output = new StringBuilder();
        boolean wasSpaceOrUnderscore = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ' ' || c == '_') {
                wasSpaceOrUnderscore = true;
                continue;
            } else {
                if (wasSpaceOrUnderscore) {
                    wasSpaceOrUnderscore = false;
                    if (Character.isLowerCase(c)) {
                        output.append((Character.toUpperCase(c)));
                        continue;
                    }
                }
                output.append(c);
            }
        }
        return output.toString();
    }

    public static String emptyIfNull(String value) {
        return value != null ? value : "";
    }

    public static boolean notNullOrEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static String until(String text, char exclusive) {
        int index = text.indexOf('(');
        if (index > -1) {
            return text.substring(0, index);
        }
        return "";
    }

    public static boolean equalsOneOf(String text, String... values) {
        return Stream.of(values).anyMatch(text::equals);
    }

    public static boolean firstNonWhitespaceCharacterIs(String text, char... c) {
        for (int i = 0; i < text.length(); i++) {
            char charAt = text.charAt(i);
            if (charAt != ' ') {
                return Lists.fromArray(c).contains(charAt);
            }
        }
        return false;
    }

    public static class ParserHandler<T> {

        private final Class<T> clazz;

        ParserHandler(Class<T> clazz) {
            this.clazz = clazz;
        }

        public ParserHandler<T> with(Function<String, T> parser) {
            parsers.put(clazz, parser);
            return this;
        }
    }

    public static class WriterHandler<T> {

        private final Class<T> clazz;

        WriterHandler(Class<T> clazz) {
            this.clazz = clazz;
        }

        public WriterHandler<T> with(Function<T, String> stringifier) {
            stringifiers.put(clazz, (o, p) -> stringifier.apply((T) o));
            return this;
        }
    }
}
