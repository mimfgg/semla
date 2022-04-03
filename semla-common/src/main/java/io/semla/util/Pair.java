package io.semla.util;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Pair<L, R> implements Map.Entry<L, R> {

    private final L left;
    private R right;

    protected Pair(L left, R right) {
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

    public Then<L> ifLeft(Predicate<L> filter) {
        return new Then<>(filter, left);
    }

    public Pair<L, R> forRight(Consumer<R> consumer) {
        consumer.accept(right);
        return this;
    }

    public Then<R> ifRight(Predicate<R> filter) {
        return new Then<>(filter, right);
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public class Then<T> {

        private final Predicate<T> filter;
        private final T target;

        public Then(Predicate<T> filter, T target) {
            this.filter = filter;
            this.target = target;
        }

        public Pair<L, R> then(Consumer<T> consumer) {
            if (filter.test(target)) {
                consumer.accept(target);
            }
            return Pair.this;
        }
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
