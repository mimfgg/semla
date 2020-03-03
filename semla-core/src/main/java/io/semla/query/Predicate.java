package io.semla.query;

import io.semla.serialization.json.Json;
import io.semla.util.Arrays;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;


public enum Predicate {

    is(Predicate::equals),
    not((value, control) -> !Predicate.equals(value, control)),
    in((Object value, List<Object> controls) -> controls.stream().anyMatch(control -> equals(value, control))),
    notIn((Object value, List<Object> controls) -> controls.stream().noneMatch(control -> equals(value, control))),
    greaterOrEquals((Comparable<Object> value, Comparable<Object> control) -> asDecimal(value).compareTo(asDecimal(control)) >= 0),
    greaterThan((Comparable<Object> value, Comparable<Object> control) -> asDecimal(value).compareTo(asDecimal(control)) > 0),
    lessOrEquals((Comparable<Object> value, Comparable<Object> control) -> asDecimal(value).compareTo(asDecimal(control)) <= 0),
    lessThan((Comparable<Object> value, Comparable<Object> control) -> asDecimal(value).compareTo(asDecimal(control)) < 0),
    like((String value, String control) -> value.matches(control.replaceAll("%", ".*"))),
    notLike((String value, String control) -> !value.matches(control.replaceAll("%", ".*"))),
    contains((BiFunction<String, String, Boolean>) String::contains),
    doesNotContain((String value, String control) -> !value.contains(control)),
    containedIn((String value, String control) -> control.contains(value)),
    notContainedIn((String value, String control) -> !control.contains(value));

    private final BiFunction<?, ?, Boolean> predicate;

    Predicate(BiFunction<?, ?, Boolean> predicate) {
        this.predicate = predicate;
    }

    @SuppressWarnings("unchecked")
    public boolean test(Object value, Object control) {
        return ((BiFunction<Object, Object, Boolean>) predicate).apply(value, control);
    }

    private static boolean equals(Object value, Object control) {
        if (value instanceof Number || value instanceof Date) {
            return asDecimal(value).compareTo(asDecimal(control)) == 0;
        } else if (value != null && control != null && value.getClass().isArray() && control.getClass().isArray()) {
            return java.util.Arrays.equals(Arrays.box(value), Arrays.box(control));
        } else {
            return Objects.equals(Json.write(value), Json.write(control));
        }
    }

    private static BigDecimal asDecimal(Object object) {
        if (object instanceof Date) {
            return new BigDecimal(((Date) object).getTime());
        } else {
            return new BigDecimal(String.valueOf(object));
        }
    }
}