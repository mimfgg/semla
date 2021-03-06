package io.semla.reflect;

import io.semla.model.Score;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldsTest {

    private Score scoreA;
    private Score scoreB;

    @Before
    public void init() {
        scoreA = Score.with("playerA", 100);
        scoreB = Score.with("playerB", 200);
    }

    @Test
    public void copyValue() {
        Fields.copyValue(scoreA, "name", scoreB);
        assertThat(scoreB.name).isEqualTo(scoreA.name);
    }

    @Test
    public void setValue() {
        Fields.setValue(scoreA, "score", 300);
        assertThat(scoreA.score).isEqualTo(300);
    }

    @Test
    public void setNullValue() {
        Fields.setValue(scoreA, "score", null);
        assertThat(scoreA.score).isEqualTo(0);
    }

    @Test
    public void getValue() {
        assertThat(Fields.<Integer>getValue(scoreA, "score")).isEqualTo(100);
    }

    @Test
    public void hasField() {
        assertThat(Fields.hasField(scoreA, "score")).isTrue();
    }
}
