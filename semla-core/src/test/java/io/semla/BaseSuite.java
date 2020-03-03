package io.semla;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseSuite {

    private static Map<String, AtomicInteger> counters = new LinkedHashMap<>();

    protected static String getNext(String resource) {
        // this will prevent collisions during tests
        return resource + counters.computeIfAbsent(resource, s -> new AtomicInteger(0)).incrementAndGet();
    }
}
