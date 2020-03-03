package io.semla.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface TypeName {

    String value();
}
