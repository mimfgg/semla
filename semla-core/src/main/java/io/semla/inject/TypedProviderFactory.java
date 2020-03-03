package io.semla.inject;

import io.semla.reflect.Types;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class TypedProviderFactory<E> extends ProviderFactory<E> {

    private final Type type;
    private final AnnotationMatcher annotationMatcher;

    public TypedProviderFactory(Type type, Provider<E> provider, AnnotationMatcher annotationMatcher) {
        super(provider);
        this.type = type;
        this.annotationMatcher = annotationMatcher;
    }

    @Override
    public boolean appliesTo(Type type, Annotation[] annotations) {
        return Types.isAssignableTo(type, Types.rawTypeOf(this.type)) && annotationMatcher.test(annotations);
    }

    @Override
    public String toString() {
        return "io.semla.inject.TypedProviderFactory<" + Types.rawTypeOf(type).getCanonicalName() + ">";
    }
}