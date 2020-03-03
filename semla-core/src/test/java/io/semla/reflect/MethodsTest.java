package io.semla.reflect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodsTest {

    public String getName(int i) {
        return "";
    }

    @Test
    public void findMethod() {
        assertThat(Methods.findMethod(this.getClass(), "getName", int.class)).isPresent();
        assertThat(Methods.findMethod(this.getClass(), "getName", String.class)).isEmpty();
    }

}