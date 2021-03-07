package io.semla.util.function;


@FunctionalInterface
public interface PentaConsumer<T, U, V, W, X> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @param w the fourth input argument
     * @param x the fifth input argument
     */
    void accept(T t, U u, V v, W w, X x);

}