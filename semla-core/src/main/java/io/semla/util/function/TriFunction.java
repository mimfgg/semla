package io.semla.util.function;


@FunctionalInterface
public interface TriFunction<T, U, V, R> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @return the function result
     */
    R apply(T t, U u, V v);

}