package io.semla.inject;

import io.semla.util.Maps;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.inject.Named;

import static io.semla.reflect.Annotations.proxyOf;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Value {

    public static Named named(String name) {
        return proxyOf(Named.class, Maps.of("value", name));
    }
}
