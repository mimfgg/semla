package io.semla.reflect;

import io.semla.model.Player;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class FieldsTest {

    private Player playerA;
    private Player playerB;

    @Before
    public void init() {
        playerA = Player.with(1, "playerA", 100);
        playerB = Player.with(2, "playerB", 200);
    }

    @Test
    public void copyValue() {
        Fields.copyValue(playerA, "name", playerB);
        assertThat(playerB.name).isEqualTo(playerA.name);
    }

    @Test
    public void setValue() {
        Fields.setValue(playerA, "score", 300);
        assertThat(playerA.score).isEqualTo(300);
    }

    @Test
    public void setNullValue() {
        Fields.setValue(playerA, "score", null);
        assertThat(playerA.score).isEqualTo(0);
    }

    @Test
    public void getValue() {
        assertThat(Fields.<Integer>getValue(playerA, "score")).isEqualTo(100);
    }

    @Test
    public void hasField() {
        assertThat(Fields.hasField(playerA, "score")).isTrue();
    }
}