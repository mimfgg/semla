package io.semla.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public abstract class SupplierFactory<E> implements Factory<E> {

    protected final Supplier<E> supplier;

    protected SupplierFactory(Supplier<E> supplier) {
        this.supplier = supplier;
    }

    @Override
    public E create(Type type, Annotation[] annotations) {
        return supplier.get();
    }
}
