package io.semla.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public final class Lists {

    private Lists() {}

    @SafeVarargs
    public static <E> List<E> of(E first, E... values) {
        ArrayList<E> list = new ArrayList<>();
        list.add(first);
        if (values.length > 0) {
            list.addAll(java.util.Arrays.asList(values));
        }
        return list;
    }

    public static <E> List<E> from(Collection<E> values) {
        return new ArrayList<>(values);
    }

    public static <E> List<List<E>> chunk(Collection<E> values, int maxChunckSize) {
        return IntStream.range(0, (int) Math.ceil((values.size() * 1.0) / maxChunckSize))
            .mapToObj(i -> values.stream().skip(maxChunckSize * i).limit(maxChunckSize))
            .map(first -> first.collect(Collectors.toList())).collect(Collectors.toList());
    }

    public static <E> List<E> fromArray(Object value) {
        return (List<E>) io.semla.util.Arrays.toStream(value).collect(Collectors.toList());
    }

    public static <E> List<E> empty() {
        return new ArrayList<>();
    }

    public static <K, V> Map<K, V> toMap(Collection<V> values, Function<V, K> key) {
        return values.stream().collect(Maps.collect(key, Function.identity()));
    }


}


