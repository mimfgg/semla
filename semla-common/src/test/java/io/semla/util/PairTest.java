package io.semla.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class PairTest {

    @Test
    public void test() {
        Pair<String, Object> pair = Pair.of("key", null);
        pair.setValue("value");
        assertThat(pair.getValue()).isEqualTo("value");
        AtomicReference<String> callback = new AtomicReference<>();
        pair.ifLeft(key -> key.equals("key")).then(callback::set);
        assertThat(callback.get()).isEqualTo("key");

        AtomicBoolean valueWasCorrect = new AtomicBoolean(false);
        pair.ifRight("value"::equals).then(value -> valueWasCorrect.set(true));
        assertThat(valueWasCorrect).isTrue();

    }

    @Test
    public void equalityTest() {
        assertThat(Pair.of(1, 2)).isEqualTo(Pair.of(1, 2));
    }

}