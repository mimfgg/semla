package io.semla.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.synchronizedMap;

@Slf4j
public class Singleton<T> implements Supplier<T> {

    private static final Map<String, Singleton<?>> GLOBAL = synchronizedMap(new HashMap<>());

    private Supplier<T> supplier;
    private volatile T instance;

    private Singleton(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    private Singleton(T instance) {
        this.instance = instance;
    }

    public T get() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = supplier.get();
                }
            }
        }
        return instance;
    }

    public Singleton<T> reset() {
        instance = null;
        return this;
    }

    public static <T> Singleton<T> lazy(Supplier<T> supplier) {
        return new Singleton<>(supplier);
    }

    public static <T> Singleton<T> of(T value) {
        return new Singleton<>(value);
    }

    /**
     * get or create a new singleton by name
     */
    @SuppressWarnings("unchecked")
    public static <T> Singleton<T> named(String name, Supplier<T> supplier) {
        return (Singleton<T>) GLOBAL.computeIfAbsent(name, __ -> {
            log.trace("creating new value for {}", name);
            return Singleton.lazy(supplier);
        });
    }

    public static void resetAll(Singleton<?> singleton, Singleton<?>... singletons) {
        Stream.concat(Stream.of(singleton), Stream.of(singletons)).forEach(Singleton::reset);
    }
}
