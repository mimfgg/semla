package io.semla.query;

import io.semla.exception.SemlaException;
import io.semla.model.EntityModel;
import io.semla.reflect.Member;
import io.semla.util.Splitter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Pagination<T> {

    public enum Sort {
        ASC, DESC
    }

    private final EntityModel<T> model;
    private final Map<Member<T>, Sort> sorts = new LinkedHashMap<>();
    private int start;
    private int limit = Integer.MAX_VALUE;

    private Pagination(EntityModel<T> model) {
        this.model = model;
    }

    public Pagination<T> copy() {
        Pagination<T> copy = new Pagination<>(model);
        copy.sorts.putAll(sorts);
        copy.start = start;
        copy.limit = limit;
        return copy;
    }

    public boolean isPaginated() {
        return start != 0 || limit < Integer.MAX_VALUE;
    }

    public Pagination<T> startAt(int start) {
        this.start = start;
        return this;
    }

    public int start() {
        return start;
    }

    public Pagination<T> limitTo(int limit) {
        this.limit = limit;
        return this;
    }

    public int limit() {
        return limit;
    }

    public boolean isSorted() {
        return !sorts.isEmpty();
    }

    public Map<Member<T>, Sort> sort() {
        return sorts;
    }

    public Pagination<T> orderedBy(String fieldName) {
        return orderedBy(fieldName, null);
    }

    public Pagination<T> orderedBy(String fieldName, Sort sort) {
        return orderedBy(model.member(fieldName), sort);
    }

    public Pagination<T> orderedBy(Member<T> field) {
        this.sorts.put(field, null);
        return this;
    }

    public Pagination<T> orderedBy(Member<T> field, Sort sort) {
        this.sorts.put(field, sort);
        return this;
    }

    public <F> int compare(T entity1, T entity2) {
        Comparator<T> comparator = (o1, o2) -> 0;
        for (Map.Entry<Member<T>, Sort> sort : sorts.entrySet()) {
            comparator = comparator.thenComparing((a, b) -> {
                int order = sort.getValue() == null || sort.getValue().equals(Sort.ASC) ? 1 : -1;
                Comparable<F> value1 = valueOfKeyIfReference(sort, a);
                F value2 = valueOfKeyIfReference(sort, b);
                if (value1 == null && value2 == null) {
                    return 0;
                } else if (value1 == null) {
                    return -1 * order;
                } else if (value2 == null) {
                    return order;
                }
                return (value1 == value2) ? 0 : value1.compareTo(value2) * order;
            });
        }
        return comparator.compare(entity1, entity2);
    }

    public Stream<T> paginate(Stream<T> filtered) {
        if (this.isSorted()) {
            filtered = filtered.sorted(this::compare);
        }
        if (this.start() > 0) {
            filtered = filtered.skip(this.start());
        }
        if (this.limit() < Integer.MAX_VALUE) {
            filtered = filtered.limit(this.limit());
        }
        return filtered;
    }

    @SuppressWarnings("unchecked")
    private <F> F valueOfKeyIfReference(Map.Entry<Member<T>, Sort> sort, T a) {
        Object value = sort.getKey().getOn(a);
        if (EntityModel.isReference(value)) {
            return EntityModel.of(value).key().member().getOn(value);
        }
        return (F) value;
    }

    public Pagination<T> parse(String paginationAsString) {
        if (paginationAsString != null && paginationAsString.length() > 0) {
            Splitter.on(',').trim().split(paginationAsString.replaceFirst("^ordered by ", ""))
                .forEach(token -> {
                    List<String> sorts = Splitter.on(' ').omitEmptyStrings().trim().split(token).toList();
                    for (int i = 0; i < sorts.size(); i++) {
                        switch (sorts.get(i)) {
                            case "start":
                                if (sorts.get(i + 1).equals("at")) {
                                    startAt(Integer.parseInt(sorts.get(i + 2)));
                                    i += 2;
                                } else {
                                    throw new SemlaException("was expecting 'at' after 'start' in '" + paginationAsString + "'");
                                }
                                break;
                            case "limit":
                                if (sorts.get(i + 1).equals("to")) {
                                    limitTo(Integer.parseInt(sorts.get(i + 2)));
                                    i += 2;
                                } else {
                                    throw new SemlaException("was expecting 'to' after 'limit' in '" + paginationAsString + "'");
                                }
                                break;
                            default:
                                if (sorts.size() > i + 1 && sorts.get(i + 1).matches("desc|asc")) {
                                    orderedBy(model.member(sorts.get(i)), Sort.valueOf(sorts.get(++i).toUpperCase()));
                                } else {
                                    orderedBy(model.member(sorts.get(i)));
                                }


                        }
                    }
                });
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!sorts.isEmpty()) {
            builder.append("ordered by ");
            sorts.forEach((field, sort) -> {
                builder.append(field.getName());
                if (sort != null) {
                    builder.append(' ').append(sort.name().toLowerCase());
                }
                builder.append(", ");
            });
            builder.delete(builder.length() - 2, builder.length());
        }
        if (start > 0) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("start at ").append(start);
        }
        if (limit < Integer.MAX_VALUE) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("limit to ").append(limit);
        }
        return builder.toString().trim();
    }

    public static <T> Pagination<T> of(Class<T> clazz) {
        return of(EntityModel.of(clazz));
    }

    public static <T> Pagination<T> of(EntityModel<T> model) {
        return new Pagination<>(model);
    }

}
