package io.semla.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class Singleton<T> implements Provider<T>, Supplier<T> {

    private static Logger LOGGER = LoggerFactory.getLogger(Singleton.class);
    private static Map<String, Singleton<?>> GLOBAL = new LinkedHashMap<>();

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
        return (Singleton<T>) GLOBAL.computeIfAbsent(name, ignore -> {
            LOGGER.trace("creating new value for {}", name);
            return Singleton.lazy(supplier);
        });
    }

    public static void resetAll(Singleton<?> singleton, Singleton<?>... singletons) {
        Stream.concat(Stream.of(singleton), Stream.of(singletons)).forEach(Singleton::reset);
    }
}
