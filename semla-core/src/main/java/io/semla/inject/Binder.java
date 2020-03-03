package io.semla.inject;

import io.semla.util.function.TriFunction;

import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.UnaryOperator;

public interface Binder {

    Binder requireExplicitBinding();

    <E> Binding<E> bind(Class<E> clazz);

    <E> Binding<E> bind(TypeLiteral<E> typeLiteral);

    Binder register(Factory<?> factory);

    Binder register(Class<? extends Factory<?>> factoryClass);

    Binder register(Class<? extends Annotation> scope, TriFunction<Type, Provider<?>, AnnotationMatcher, Factory<?>> factoryBuilder);

    <E> BindingInterceptor<E> intercept(Class<E> clazz);

    <E> BindingInterceptor<E> intercept(TypeLiteral<E> typeLiteral);

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

        Binder toProvider(Class<? extends Provider<? extends E>> provider);

        Binder toProvider(Provider<? extends E> provider);

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
