package io.semla.reflect;

import org.assertj.core.util.VisibleForTesting;
import org.junit.Test;

import static io.semla.reflect.Modifier.*;
import static org.junit.Assert.assertTrue;

public class ModifierTest {

    private Object member;

    @Test
    public void is() {
        assertTrue(Modifier.is(this.getClass(), PUBLIC));
        assertTrue(Modifier.is(Methods.getMethod(this.getClass(), "method"), PUBLIC));
        assertTrue(Modifier.is(Fields.getField(this.getClass(), "member"), PRIVATE));
    }

    @Test
    public void not() {
        assertTrue(Modifier.not(this.getClass(), PRIVATE));
        assertTrue(Modifier.not(Methods.getMethod(this.getClass(), "method"), STATIC, TRANSIENT, PROTECTED, PRIVATE));
        assertTrue(Modifier.not(Fields.getField(this.getClass(), "member"), PUBLIC));
    }

    @VisibleForTesting
    public void method() {

    }
}