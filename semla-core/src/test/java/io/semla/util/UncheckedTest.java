package io.semla.util;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UncheckedTest {

    @Test(expected = IOException.class)
    public void rethrow() {
        Unchecked.rethrow(new IOException());
    }

    @Test(expected = IOException.class)
    public void uncheckedSupplier() {
        Unchecked.unchecked(() -> {
            throw new IOException();
        });
    }

    @Test(expected = IOException.class)
    public void uncheckedRunnable() {
        Unchecked.unchecked((Throwables.Runnable) () -> {
            throw new IOException();
        });
    }

    @Test
    public void catchAndPrefixMessageRunnable() {
        assertThatThrownBy(() ->
            Unchecked.catchAndPrefixMessage(() -> "something broke: ",
                (Throwables.Runnable) () -> {
                    throw new IOException("end of the thing");
                }))
            .hasMessage("something broke: end of the thing");
    }

    @Test
    public void catchAndPrefixMessageSupplier() {
        assertThatThrownBy(() ->
            Unchecked.catchAndPrefixMessage(() -> "something broke: ",
                () -> {
                    throw new IOException("end of the thing");
                }))
            .hasMessage("something broke: end of the thing");
    }
}