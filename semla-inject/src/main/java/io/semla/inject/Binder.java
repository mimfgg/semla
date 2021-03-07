package io.semla.inject;

import io.semla.reflect.TypeReference;
import io.semla.util.function.TriFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface Binder {

    Binder requireExplicitBinding();

    <E> Binding<E> bind(Class<E> clazz);

    <E> Binding<E> bind(TypeReference<E> typeReference);

    Binder register(Factory<?> factory);

    Binder register(Class<? extends Factory<?>> factoryClass);

    Binder register(Class<? extends Annotation> scope, TriFunction<Type, Supplier<?>, AnnotationMatcher, Factory<?>> factoryBuilder);

    <E> BindingInterceptor<E> intercept(Class<E> clazz);

    <E> BindingInterceptor<E> intercept(TypeReference<E> typeReference);

    <E> MultiBinding<E> multiBind(Class<E> clazz);

    interface FilteredBinding<SelfType> {

        SelfType named(String name);

        SelfType annotatedWith(Class<? extends Annotation> annotation);

        SelfType annotatedWith(Annotation annotation);

    }

    interface Binding<E> extends FilteredBinding<Binding<E>> {

        Binding<E> in(Class<? extends Annotation> scope);

        Binder to(Class<? extends E> clazz);

        Binder to(E instance);

        Binder toConstructor(Constructor<? extends E> constructor);

        Binder toSupplier(Class<? extends Supplier<? extends E>> supplier);

        Binder toSupplier(Supplier<? extends E> supplier);

    }

    interface MultiBinding<E> extends FilteredBinding<MultiBinding<E>> {

        Binder add(Class<? extends E> clazz);

        Binder add(Collection<Class<? extends E>> classes);

        Binder add(E instance);
    }

    interface BindingInterceptor<E> extends FilteredBinding<BindingInterceptor<E>> {

        Binder with(UnaryOperator<E> unaryOperator);

    }
}
