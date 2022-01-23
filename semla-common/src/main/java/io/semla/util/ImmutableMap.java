package io.semla.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImmutableMap<K, V> implements Map<K, V> {

    private final Map<K, V> map = new LinkedHashMap<>();

    private ImmutableMap(Map<K, V> map) {
        this.map.putAll(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        return ImmutableSet.copyOf(map.keySet());
    }

    @Override
    public Collection<V> values() {
        return ImmutableList.copyOf(map.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return ImmutableSet.copyOf(map.entrySet());
    }


    public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
        return new ImmutableMap<>(map);
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static class Builder<K, V> {

        private final ImmutableMap<K, V> immutableMap = new ImmutableMap<>();

        private Builder() {
        }

        public Builder<K, V> put(K key, V value) {
            immutableMap.map.put(key, value);
            return this;
        }

        public ImmutableMap<K, V> build() {
            return immutableMap;
        }
    }

    public static <K, V> Map<K, V> of(K k1, V v1) {
        return ImmutableMap.<K, V>builder().put(k1, v1).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        return ImmutableMap.<K, V>builder().put(k1, v1).put(k2, v2).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return ImmutableMap.<K, V>builder().put(k1, v1).put(k2, v2).put(k3, v3).build();
    }

    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return ImmutableMap.<K, V>builder().put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build();
    }
}
