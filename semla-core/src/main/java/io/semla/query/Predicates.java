package io.semla.query;

import io.semla.exception.InvalidQueryException;
import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.serialization.json.Json;
import io.semla.util.Lists;
import io.semla.util.Splitter;
import io.semla.util.Strings;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.model.EntityModel.isEntity;
import static io.semla.reflect.Types.isAssignableTo;


@SuppressWarnings("unchecked")
public class Predicates<T> extends LinkedHashMap<Member<T>, Map<Predicate, Object>> {

    private final EntityModel<T> model;

    private Predicates(EntityModel<T> model) {
        this.model = model;
    }

    public Predicates<T> add(Predicates<T> predicates) {
        putAll(predicates);
        return this;
    }

    public Predicates<T> where(String name, Predicate predicate, Object value) {
        Member<T> member = model.member(name);
        computeIfAbsent(member, k -> new LinkedHashMap<>()).put(predicate, adjustedValue(value, member));
        return this;
    }

    public Object adjustedValue(Object value, Member<T> member) {
        if (value != null) {
            if (value instanceof String && ((String) value).startsWith("[") && ((String) value).endsWith("]")) {
                value = adjustedValue(Splitter.on(',').omitEmptyStrings().trim().split(((String) value).substring(1, ((String) value).length() - 1)).toList(), member);
            }
            if (value instanceof Collection) {
                if (((Collection) value).isEmpty()) {
                    throw new IllegalStateException("can't check collection predicate against empty values");
                }
                value = new ArrayList<>(((Collection<?>) value));
                T first = ((List<T>) value).get(0);
                if (!isAssignableTo(first.getClass(), member.getType())) {
                    if (first instanceof String) {
                        value = ((List<String>) value).stream().map(e -> Strings.parse(e, member.getType())).collect(Collectors.toList());
                    } else if (model.key().member().isAssignableTo(first.getClass())) {
                        value = ((List) value).stream().map(e -> EntityModel.of(member.getType()).newInstanceFromKey(e)).collect(Collectors.toList());
                    } else {
                        throw new IllegalArgumentException(member + " cannot be checked against value '" + value + "' of type " + first.getClass());
                    }
                }
            } else if (!isAssignableTo(value.getClass(), member.getType())) {
                if (value instanceof String) {
                    value = Strings.parse((String) value, member.getType());
                } else if ((isEntity(member.getType()) && !isAssignableTo(value.getClass(), member.getType()))) {
                    value = Strings.parse(String.valueOf(value), member.getType());
                } else if (model.key().member().isAssignableTo(Long.class) && Types.isAssignableTo(value.getClass(), Integer.class)) {
                    value = ((Integer) value).longValue();
                } else {
                    throw new IllegalArgumentException(member + " cannot be checked against value '" + value + "' of type " + value.getClass());
                }
            } else if (EntityModel.isEntity(value)) {
                value = EntityModel.referenceTo(value);
            }
        }
        return value;
    }

    public Handler<Predicates<T>> where(String name) {
        return where(this, name);
    }

    public Handler<Predicates<T>> and(String name) {
        return where(this, name);
    }

    public <CallBackType> Handler<CallBackType> where(CallBackType callBack, String name) {
        return and(callBack, name);
    }

    public <CallBackType> Handler<CallBackType> and(CallBackType callBack, String name) {
        model.assertHasMember(name);
        return new Handler<>(callBack, name);
    }

    public Predicates<T> parse(String predicatesAsString) {
        if (predicatesAsString != null && predicatesAsString.length() > 0) {
            boolean inQuotedContent = false;
            int parsingIndex = 0;
            String fieldName = null;
            Predicate predicate = null;
            String value;
            StringBuilder sb = new StringBuilder();
            predicatesAsString = predicatesAsString.replaceAll("where (.*)", "$1");
            for (int i = 0; i < predicatesAsString.length(); i++) {
                char c = predicatesAsString.charAt(i);
                switch (c) {
                    case ' ':
                        if (inQuotedContent) {
                            sb.append(c);
                        } else if (sb.charAt(sb.length() - 1) != ' ') {
                            if (parsingIndex == 0) {
                                fieldName = sb.toString();
                                if (!fieldName.equals("and")) {
                                    parsingIndex++;
                                }
                            } else if (parsingIndex == 1) {
                                predicate = Predicate.valueOf(sb.toString());
                                parsingIndex++;
                            } else if (parsingIndex == 2) {
                                value = sb.toString();
                                where(fieldName, predicate, value);
                                parsingIndex = 0;
                            }
                            sb = new StringBuilder();
                        }
                        break;
                    case '"':
                        inQuotedContent ^= true;
                        break;
                    case '[':
                    case ']':
                        inQuotedContent ^= true;
                    default:
                        sb.append(c);
                        break;

                }
            }
            if (sb.length() > 0 && parsingIndex == 2) {
                value = sb.toString();
                where(fieldName, predicate, value);
            }
        }
        return this;
    }

    public boolean matches(T entity) {
        for (Map.Entry<Member<T>, Map<Predicate, Object>> entry : entrySet()) {
            Member<T> member = entry.getKey();
            for (Map.Entry<Predicate, Object> operators : entry.getValue().entrySet()) {
                if (!operators.getKey().test(member.getOn(entity), operators.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    public Stream<T> filter(Collection<T> entities) {
        return entities.stream().filter(this::matches);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        forEach((field, operators) ->
            operators.forEach((predicate, value) -> {
                if (builder.length() > 0) {
                    builder.append(" and ");
                }
                builder.append(field.getName()).append(" ").append(predicate).append(" ");
                if (value instanceof String) {
                    builder.append("\"").append(value).append("\"");
                } else {
                    builder.append(Strings.toString(value));
                }
            }));
        return builder.toString();
    }

    public EntityModel<T> model() {
        return model;
    }

    public void enforceIndices() {
        Set<String> indexHits = keySet().stream()
            .map(member -> model.indices().stream()
                .map(index -> index.columns().stream()
                    .filter(column -> column.member().getName().equals(member.getName()))
                    .map(Column::member))
                .flatMap(Function.identity())
            )
            .flatMap(Function.identity())
            .map(Member::getName)
            .collect(Collectors.toSet());

        if (keySet().size() != indexHits.size()) {
            List<String> nonIndexed = keySet().stream().map(Member::getName).filter(name -> !indexHits.contains(name)).collect(Collectors.toList());
            throw new InvalidQueryException(String.join(", ", nonIndexed) + " " + (nonIndexed.size() == 1 ? "is" : "are") + " not indexed " +
                "and this entityManager requires all queried properties to be indexed!");
        }
    }

    public Object toKey() {
        if (size() == 1) {
            Map.Entry<Member<T>, Map<Predicate, Object>> memberPredicate = entrySet().iterator().next();
            if (memberPredicate.getKey().equals(model.key().member())) {
                Map<Predicate, Object> predicates = memberPredicate.getValue();
                if (predicates.size() == 1) {
                    Map.Entry<Predicate, Object> predicate = predicates.entrySet().iterator().next();
                    if (predicate.getKey().equals(Predicate.is) || predicate.getKey().equals(Predicate.in)) {
                        return predicate.getValue();
                    }
                }
            }
        }
        throw new IllegalStateException("unexpected member predicates in: " + Json.write(this));
    }

    public boolean isKeyOnly() {
        if (size() == 1) {
            Map.Entry<Member<T>, Map<Predicate, Object>> memberPredicate = entrySet().iterator().next();
            if (memberPredicate.getKey().equals(model.key().member())) {
                Map<Predicate, Object> predicates = memberPredicate.getValue();
                if (predicates.size() == 1) {
                    Map.Entry<Predicate, Object> predicate = predicates.entrySet().iterator().next();
                    return predicate.getKey().equals(Predicate.is) || predicate.getKey().equals(Predicate.in);
                }
            }
        }
        return false;
    }

    public static <T> Predicates<T> of(Class<T> clazz) {
        return of(EntityModel.of(clazz));
    }

    public static <T> Predicates<T> of(EntityModel<T> model) {
        return new Predicates<>(model);
    }

    public class Handler<CallBackType> {

        private final CallBackType callBack;
        private final String fieldName;

        public Handler(CallBackType callBack, String fieldName) {
            this.callBack = callBack;
            this.fieldName = fieldName;
        }

        public CallBackType is(Object value) {
            where(fieldName, Predicate.is, value);
            return callBack;
        }

        public CallBackType not(Object value) {
            where(fieldName, Predicate.not, value);
            return callBack;
        }

        public CallBackType in(Object value, Object... values) {
            if (value instanceof Collection && values.length == 0) {
                // fix for single collection being given as an argument
                return in((Collection<?>) value);
            }
            return in(Lists.of(value, values));
        }

        public CallBackType in(Collection<?> values) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("values cannot be empty");
            }
            where(fieldName, Predicate.in, Lists.from(values));
            return callBack;
        }

        public CallBackType notIn(Object value, Object... values) {
            if (value instanceof Collection && values.length == 0) {
                // fix for single collection being given as an argument
                return notIn((Collection<?>) value);
            }
            return notIn(Lists.of(value, values));
        }

        public CallBackType notIn(Collection<?> values) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("values cannot be empty");
            }
            where(fieldName, Predicate.notIn, Lists.from(values));
            return callBack;
        }

        public CallBackType greaterOrEquals(Number number) {
            where(fieldName, Predicate.greaterOrEquals, number);
            return callBack;
        }

        public CallBackType greaterThan(Number number) {
            where(fieldName, Predicate.greaterThan, number);
            return callBack;
        }

        public CallBackType lessOrEquals(Number number) {
            where(fieldName, Predicate.lessOrEquals, number);
            return callBack;
        }

        public CallBackType lessThan(Number number) {
            where(fieldName, Predicate.lessThan, number);
            return callBack;
        }

        public CallBackType like(String pattern) {
            where(fieldName, Predicate.like, pattern);
            return callBack;
        }

        public CallBackType notLike(String pattern) {
            where(fieldName, Predicate.notLike, pattern);
            return callBack;
        }

        public CallBackType contains(String pattern) {
            where(fieldName, Predicate.contains, pattern);
            return callBack;
        }

        public CallBackType doesNotContain(String pattern) {
            where(fieldName, Predicate.doesNotContain, pattern);
            return callBack;
        }

        public CallBackType containedIn(String pattern) {
            where(fieldName, Predicate.containedIn, pattern);
            return callBack;
        }

        public CallBackType notContainedIn(String pattern) {
            where(fieldName, Predicate.notContainedIn, pattern);
            return callBack;
        }
    }

}
