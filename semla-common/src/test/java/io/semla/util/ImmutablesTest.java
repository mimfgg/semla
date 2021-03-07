package io.semla.util;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ImmutablesTest {

    @Test
    public void immutableList() {
        List<String> list = ImmutableList.of("Test");
        assertThat(list).size().isEqualTo(1);
        assertThat(list).isNotEmpty();
        assertThat(list.contains("Test")).isTrue();
        assertThat(list.containsAll(Lists.of("Test"))).isTrue();
        assertThat(list.iterator().next()).isEqualTo("Test");
        assertThat(list.toArray()).isEqualTo(new Object[]{"Test"});
        assertThat(list.toArray(new String[0])).isEqualTo(new String[]{"Test"});
        assertThat(list.get(0)).isEqualTo("Test");
        assertThat(list.indexOf("Test")).isEqualTo(0);
        assertThat(list.lastIndexOf("Test")).isEqualTo(0);
        assertThat(list.listIterator().hasNext()).isTrue();
        assertThat(list.listIterator(1).hasNext()).isFalse();
        assertThat(list.subList(0, 1)).isInstanceOf(ImmutableList.class);


        Stream.<ThrowableAssert.ThrowingCallable>of(
            () -> list.add("Test2"),
            () -> list.add(0, "Test2"),
            () -> list.set(0, "Test2"),
            () -> list.remove("Test"),
            () -> list.remove(0),
            () -> list.addAll(Lists.of("something else")),
            () -> list.addAll(1, Lists.of("something else")),
            () -> list.retainAll(Lists.of("Test")),
            () -> list.removeAll(Lists.of("something else")),
            list::clear
        ).forEach(throwingCallable -> assertThatThrownBy(throwingCallable).isInstanceOf(UnsupportedOperationException.class));


        assertThat(ImmutableList.copyOf(Lists.of("Test"))).size().isEqualTo(1);
        assertThat(ImmutableList.empty()).isEmpty();
    }

    @Test
    public void immutableSet() {
        Set<String> set = ImmutableSet.of("Test");
        assertThat(set).size().isEqualTo(1);
        assertThat(set).isNotEmpty();
        assertThat(set.contains("Test")).isTrue();
        assertThat(set.containsAll(Lists.of("Test"))).isTrue();
        assertThat(set.iterator().next()).isEqualTo("Test");
        assertThat(set.toArray()).isEqualTo(new Object[]{"Test"});
        assertThat(set.toArray(new String[0])).isEqualTo(new String[]{"Test"});

        Stream.<ThrowableAssert.ThrowingCallable>of(
            () -> set.add("Test2"),
            () -> set.remove("Test"),
            () -> set.addAll(Lists.of("something else")),
            () -> set.retainAll(Lists.of("Test")),
            () -> set.removeAll(Lists.of("something else")),
            set::clear
        ).forEach(throwingCallable -> assertThatThrownBy(throwingCallable).isInstanceOf(UnsupportedOperationException.class));


        assertThat(ImmutableSet.copyOf(Lists.of("Test"))).size().isEqualTo(1);
        assertThat(ImmutableSet.empty()).isEmpty();
    }

    @Test
    public void immutableMap() {
        Map<String, String> map = ImmutableMap.of("key", "value");
        assertThat(map).size().isEqualTo(1);
        assertThat(map).isNotEmpty();
        assertThat(map).containsKey("key");
        assertThat(map).containsValue("value");
        assertThat(map.get("key")).isEqualTo("value");
        assertThat(map.keySet()).isInstanceOf(ImmutableSet.class);
        assertThat(map.values()).isInstanceOf(ImmutableList.class);
        assertThat(map.entrySet()).isInstanceOf(ImmutableSet.class);

        Stream.<ThrowableAssert.ThrowingCallable>of(
            () -> map.put("key2", "value2"),
            () -> map.remove("key"),
            () -> map.putAll(ImmutableMap.of("key2", "value2")),
            map::clear
        ).forEach(throwingCallable -> assertThatThrownBy(throwingCallable).isInstanceOf(UnsupportedOperationException.class));

        assertThat(ImmutableMap.copyOf(ImmutableMap.of("key", "value"))).size().isEqualTo(1);
        assertThat(ImmutableMap.of("key", "value", "key2", "value2")).size().isEqualTo(2);
        assertThat(ImmutableMap.of("key", "value", "key2", "value2", "key3", "value3")).size().isEqualTo(3);
        assertThat(ImmutableMap.of("key", "value", "key2", "value2", "key3", "value3", "key4", "value4")).size().isEqualTo(4);
    }
}