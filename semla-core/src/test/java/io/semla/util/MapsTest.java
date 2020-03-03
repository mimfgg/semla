package io.semla.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MapsTest {

    @Test
    public void copyOf() {
        assertThat(Maps.copyOf(Maps.of("key", "value"))).size().isEqualTo(1);
    }

    @Test
    public void builder() {
        assertThat(Maps.<String, String>builder().put("key", "value").build()).size().isEqualTo(1);
    }

    @Test
    public void of() {
        assertThat(Maps.of("key1", "value1")).size().isEqualTo(1);
        assertThat(Maps.of("key1", "value1", "key2", "value2")).size().isEqualTo(2);
        assertThat(Maps.of("key1", "value1", "key2", "value2", "key3", "value3")).size().isEqualTo(3);
        assertThat(Maps.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4")).size().isEqualTo(4);
    }

    @Test
    public void map() {
        assertThat(Maps.map(Maps.of("key", "value"), k -> "prefixed." + k, v -> "prefixed." + v))
            .isEqualTo(Maps.of("prefixed.key", "prefixed.value"));
    }

    @Test
    public void collect() {
        assertThat(Maps.of("key", "value").entrySet().stream().collect(Maps.collect())).size().isEqualTo(1);
        assertThat(Maps.of("key", "value", "key2", "value2", "key3", "value3", "key4", "value4")
            .entrySet().parallelStream().collect(Maps.collect(e -> "prefixed." + e.getKey(), e -> "prefixed." + e.getValue())))
            .isEqualTo(Maps.of("prefixed.key", "prefixed.value",
                "prefixed.key2", "prefixed.value2",
                "prefixed.key3", "prefixed.value3",
                "prefixed.key4", "prefixed.value4"));
    }

}