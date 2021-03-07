package io.semla.util;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Pair<L, R> implements Map.Entry<L, R> {

    private final L left;
    private R right;

    protected Pair(L left, R right) {
        if (left == null) {
            throw new IllegalArgumentException("left/key cannot be null");
        }
        this.left = left;
        this.right = right;
    }

    public L left() {
        return left;
    }

    public L key() {
        return left;
    }

    public L first() {
        return left;
    }

    public R right() {
        return right;
    }

    public R value() {
        return right;
    }

    public R second() {
        return right;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        R oldValue = right;
        right = value;
        return oldValue;
    }

    public boolean matches(Predicate<L> left, Predicate<R> right) {
        return left.test(this.left) && right.test(this.right);
    }

    public Pair<L, R> forLeft(Consumer<L> consumer) {
        consumer.accept(left);
        return this;
    }

    public Pair<L, R> ifLeft(Predicate<L> filter, Consumer<L> consumer) {
        if (filter.test(left)) {
            return forLeft(consumer);
        }
        return this;
    }

    public Pair<L, R> forRight(Consumer<R> consumer) {
        consumer.accept(right);
        return this;
    }

    public Pair<L, R> ifRight(Predicate<R> filter, Consumer<R> consumer) {
        if (filter.test(right)) {
            return forRight(consumer);
        }
        return this;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    @Override
    public String toString() {
        return "(" + Strings.toString(left) + ", " + Strings.toString(right) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return left.equals(pair.left) && right.equals(pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
