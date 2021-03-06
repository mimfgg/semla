package io.semla.query;

import io.semla.model.Player;
import org.junit.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ValuesTest {

    @Test
    public void wrapTypes() {
        assertThat(Values.of(Player.class).with("score", "200").toString()).isEqualTo("{io.semla.model.Player.score=200}");
        assertThatThrownBy(() -> Values.of(Player.class).with("score", BigInteger.valueOf(200)))
            .hasMessage("int cannot be assigned value '200' of type class java.math.BigInteger");
    }
}
