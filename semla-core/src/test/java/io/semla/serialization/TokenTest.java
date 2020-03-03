package io.semla.serialization;

import io.semla.model.Player;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenTest {

    @Test
    public void fromType() {
        assertThat(Token.fromType(String.class)).isEqualTo(Token.STRING);
        assertThat(Token.fromType(Integer.class)).isEqualTo(Token.NUMBER);
        assertThat(Token.fromType(Boolean.class)).isEqualTo(Token.BOOLEAN);
        assertThat(Token.fromType(List.class)).isEqualTo(Token.ARRAY);
        assertThat(Token.fromType(Integer[].class)).isEqualTo(Token.ARRAY);
        assertThat(Token.fromType(Player.class)).isEqualTo(Token.OBJECT);

        Token.register(Instant.class, Token.NUMBER);
        assertThat(Token.fromType(Instant.class)).isEqualTo(Token.NUMBER);
    }
}