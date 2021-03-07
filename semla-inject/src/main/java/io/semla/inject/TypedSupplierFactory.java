package io.semla.inject;

import io.semla.reflect.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class TypedSupplierFactory<E> extends SupplierFactory<E> {

    private final Type type;
    private final AnnotationMatcher annotationMatcher;

    public TypedSupplierFactory(Type type, Supplier<E> supplier, AnnotationMatcher annotationMatcher) {
        super(supplier);
        this.type = type;
        this.annotationMatcher = annotationMatcher;
    }

    @Override
    public boolean appliesTo(Type type, Annotation[] annotations) {
        return type.equals(this.type) && annotationMatcher.test(annotations);
    }

    @Override
    public String toString() {
        return "io.semla.inject.TypedProviderFactory<" + Types.rawTypeOf(type).getCanonicalName() + ">";
    }
}
