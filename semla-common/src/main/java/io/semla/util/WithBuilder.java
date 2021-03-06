package io.semla.util;

import java.util.function.Consumer;

public class WithBuilder<ValueType> {

    private final Consumer<ValueType> next;

    public WithBuilder(Consumer<ValueType> next) {
        this.next = next;
    }

    public void with(ValueType value) {
        next.accept(value);
    }
}
