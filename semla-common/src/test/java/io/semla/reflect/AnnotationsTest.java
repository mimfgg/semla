package io.semla.reflect;

import io.semla.util.Maps;
import org.junit.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationsTest {

    @Test
    public void proxyOf() {
        assertThat(Annotations.proxyOf(TestAnnotationWith1Field.class, Maps.of("value", "test")).toString())
                .isEqualTo(Methods.getMethod(this.getClass(), "methodAnnotatedWith1Field").getAnnotations()[0].toString());

        assertThat(Annotations.proxyOf(TestAnnotationWith2Fields.class, Maps.of("value", "test", "number", 2)).toString())
                .isEqualTo(Methods.getMethod(this.getClass(), "methodAnnotatedWith2Fields").getAnnotations()[0].toString());
    }

    @TestAnnotationWith1Field("test")
    public static void methodAnnotatedWith1Field(){

    }

    @TestAnnotationWith2Fields(value = "test", number = 2)
    public static void methodAnnotatedWith2Fields(){

    }

    @Target({METHOD, CONSTRUCTOR, FIELD})
    @Retention(RUNTIME)
    @Documented
    public static @interface TestAnnotationWith2Fields {

        String value() default "";

        int number() default 0;
    }

    @Target({METHOD, CONSTRUCTOR, FIELD})
    @Retention(RUNTIME)
    @Documented
    public static @interface TestAnnotationWith1Field {

        String value() default "";
    }
}