package io.semla.util.function;


@FunctionalInterface
public interface QuadFunction<T, U, V, W, R> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @param w the fourth input argument
     * @return the function result
     */
    R apply(T t, U u, V v, W w);

}