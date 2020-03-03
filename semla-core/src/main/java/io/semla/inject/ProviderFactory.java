package io.semla.inject;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public abstract class ProviderFactory<E> implements Factory<E> {

    protected final Provider<E> provider;

    protected ProviderFactory(Provider<E> provider) {
        this.provider = provider;
    }

    @Override
    public E create(Type type, Annotation[] annotations) {
        return provider.get();
    }
}