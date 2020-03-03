package io.semla.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RUNTIME)
public @interface Deserialize {

    When value() default When.ALWAYS;

    String from() default "";
}
