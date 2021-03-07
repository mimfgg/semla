package io.semla.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PairTest {

    @Test
    public void test() {
        assertThatThrownBy(() -> Pair.of(null, null)).hasMessage("left/key cannot be null");
        Pair<String, Object> pair = Pair.of("key", null);
        pair.setValue("value");
        assertThat(pair.getValue()).isEqualTo("value");
        pair.ifLeft(key -> key.equals("key"), key -> assertThat(key).isEqualTo("key"));
    }

    @Test
    public void equalityTest() {
        assertThat(Pair.of(1, 2)).isEqualTo(Pair.of(1, 2));
    }

}