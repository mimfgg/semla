package io.semla.model;

import io.semla.exception.SemlaException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ModelTest {

    @Test
    public void merge() {
        assertThatThrownBy(() ->
            Model.of(Score.class).merge(
                Score.with("test", 300),
                Score.with("test", 200)))
            .isInstanceOf(SemlaException.class)
            .hasMessage("couldn't merge already set value to '200' for 'io.semla.model.Score.score', was '300'");
    }

    @Test
    public void newInstance() {
        assertThatThrownBy(() -> Model.of(Integer.class).newInstance())
            .isInstanceOf(SemlaException.class)
            .hasMessage("couldn't create a new instance of class java.lang.Integer");
    }

    @Test
    public void getClassBy() {
        assertThatThrownBy(() -> Model.getClassBy("unknown"))
            .isInstanceOf(SemlaException.class)
            .hasMessage("could not find any class known by the name 'unknown'");
    }
}
