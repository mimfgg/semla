package io.semla.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public interface Factory<T> {

    boolean appliesTo(Type type, Annotation[] annotations);

    T create(Type type, Annotation[] annotations);
}
