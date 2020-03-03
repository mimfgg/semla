package io.semla.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

public class Maps {

    private Maps() {}

    public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
        return new LinkedHashMap<>(map);
    }

    public static <K, V> Builder<K, V> builder() {
        return new Maps.Builder<>();
    }

    public static class Builder<K, V> {

        private final LinkedHashMap<K, V> map = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return map;
        }
    }

    public static <K, V> Map<K, V> of(K k1, V v1) {
        return Maps.<K, V>builder().put(k1, v1).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return Maps.<K, V>builder().put(k1, v1).put(k2, v2).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return Maps.<K, V>builder().put(k1, v1).put(k2, v2).put(k3, v3).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return Maps.<K, V>builder().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build();
    }

    public static <T, V, K, U> Map<K, U> map(Map<T, V> map,
                                             Function<? super T, ? extends K> keyMapper,
                                             Function<? super V, ? extends U> valueMapper) {
        return map.entrySet().stream().collect(collect(e -> keyMapper.apply(e.getKey()), e -> valueMapper.apply(e.getValue())));
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> collect() {
        return collect(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <E, K, V> Collector<E, ?, Map<K, V>> collect(Function<? super E, ? extends K> keyMapper,
                                                               Function<? super E, ? extends V> valueMapper) {
        return Collector.of(LinkedHashMap::new,
            (map, e) -> map.put(keyMapper.apply(e), valueMapper.apply(e)),
            (map1, map2) -> {
                map1.putAll(map2);
                return map1;
            });
    }
}
