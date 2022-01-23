package io.semla.util;

import io.semla.reflect.*;
import io.semla.serialization.Token;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JavassistTest {

    @Test
    public void extending() {
        assertThat(
            Javassist.getOrCreate("io.semla.util.ExtendedLinkedHashMap", this.getClass(), clazz -> clazz.extending(LinkedHashMap.class))
        ).isEqualTo(
            Javassist.getOrCreate("io.semla.util.ExtendedLinkedHashMap", this.getClass(), clazz -> clazz.extending(LinkedHashMap.class))
        );
    }

    @Test
    public void implementing() {
        Class<?> someSerializableType =
            Javassist.getOrCreate("io.semla.util.SomeSerializableType", this.getClass(), clazz -> clazz.implementing(Serializable.class));
        Object o = Types.newInstance(someSerializableType);
        assertThat(o).isInstanceOf(Serializable.class);
    }

    @Test
    public void addAnnotation() {
        Class<?> someAnnotatedType =
            Javassist.getOrCreate("io.semla.util.SomeAnnotatedType", this.getClass(), clazz -> clazz.addAnnotation(AllMemberValues.class));
        assertThat(someAnnotatedType).hasAnnotation(AllMemberValues.class);
        Class<?> someAnnotatedTypeWithATable =
            Javassist.getOrCreate("io.semla.util.SomeAnnotatedTypeWithMoreInfo", this.getClass(),
                clazz -> clazz.addAnnotation(AllMemberValues.class, annotation -> annotation.set("value", "something")));
        assertThat(someAnnotatedTypeWithATable).hasAnnotation(AllMemberValues.class);
        assertThat(someAnnotatedTypeWithATable.getAnnotation(AllMemberValues.class).value()).isEqualTo("something");
    }

    @Test
    public void addField() {
        Class<?> someTypeWithAField =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAField", this.getClass(), clazz -> clazz.addField("value", String.class));
        assertThat(Fields.getField(someTypeWithAField, "value")).isNotNull();
        Class<?> someTypeWithAPrivateField =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAPrivateField", this.getClass(),
                clazz -> clazz.addField("value", String.class,
                    field -> field.setModifier(Modifier.PRIVATE)));
        Field privateField = Fields.getField(someTypeWithAPrivateField, "value");
        assertThat(privateField).isNotNull();
        assertThat(privateField.getModifiers() & java.lang.reflect.Modifier.PRIVATE).isEqualTo(java.lang.reflect.Modifier.PRIVATE);
        Class<?> someTypeWithAnAnnotatedField =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAnAnnotatedField", this.getClass(),
                clazz -> clazz.addField("id", Integer.class, field -> field.addAnnotation(Serialize.class)));
        assertThat(Fields.getField(someTypeWithAnAnnotatedField, "id").isAnnotationPresent(Serialize.class)).isTrue();
    }

    @Test
    public void addMethod() {
        Class<?> someTypeWithAMethod =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAMethod", this.getClass(), clazz -> clazz.addMethod("public String test(){return \"hello world!\";}"));
        assertThat(Methods.<String>invoke(Types.newInstance(someTypeWithAMethod), "test")).isEqualTo("hello world!");
        Class<?> someTypeWithAnAnnotatedMethod =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAnAnnotatedMethod", this.getClass(),
                clazz -> clazz.addMethod("public String test(){return \"hello world!\";}",
                    method -> method.addAnnotation(Serialize.class)));
        assertThat(Methods.getMethod(someTypeWithAnAnnotatedMethod, "test").isAnnotationPresent(Serialize.class)).isTrue();
    }

    @Test
    public void addConstructor() throws NoSuchMethodException {
        Class<?> someTypeWithAConstructor =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAConstructor", this.getClass(), clazz -> clazz.addConstructor("public SomeTypeWithAConstructor(int id){}"));
        assertThat(someTypeWithAConstructor.getConstructor(int.class)).isNotNull();

        Class<?> someTypeWithAnAnnotatedConstructor =
            Javassist.getOrCreate("io.semla.util.SomeTypeWithAnAnnotatedConstructor", this.getClass(),
                clazz -> clazz.addConstructor("public SomeTypeWithAnAnnotatedConstructor(int id){}",
                    constructor -> constructor.addAnnotation(SomeAllAroundAnnotation.class)));
        assertThat(someTypeWithAnAnnotatedConstructor.getConstructor(int.class).isAnnotationPresent(SomeAllAroundAnnotation.class)).isTrue();
    }

    @Test
    public void annotations() {
        Deserialize deserialize = Annotations.proxyOf(Deserialize.class, Maps.of("from", "test"));
        Deserialize[] deserializes = Arrays.of(Annotations.proxyOf(Deserialize.class, Maps.of("from", "test")));

        Class<?> someAnnotatedType =
            Javassist.getOrCreate("io.semla.util.SomeTypeAnnotatedWithAllMemberValues", this.getClass(),
                clazz -> clazz
                    .addAnnotation(AllMemberValues.class, annotation -> annotation
                        .set("deserialize", deserialize)
                        .set("deserializes", deserializes)
                        .set("test", true)
                        .set("b", (byte) 1)
                        .set("c", 'p')
                        .set("clazz", String.class)
                        .set("d", 1d)
                        .set("token", Token.NUMBER)
                        .set("f", 1f)
                        .set("i", 1)
                        .set("l", 1L)
                        .set("s", (short) 1)
                        .set("value", "test")
                    ));
        AllMemberValues allMemberValues = someAnnotatedType.getAnnotation(AllMemberValues.class);
        assertThat(allMemberValues.deserialize()).isEqualTo(deserialize);
        assertThat(allMemberValues.deserializes()).isEqualTo(deserializes);
        assertThat(allMemberValues.test()).isEqualTo(true);
        assertThat(allMemberValues.b()).isEqualTo((byte) 1);
        assertThat(allMemberValues.c()).isEqualTo('p');
        assertThat(allMemberValues.clazz()).isEqualTo(String.class);
        assertThat(allMemberValues.d()).isEqualTo(1d);
        assertThat(allMemberValues.token()).isEqualTo(Token.NUMBER);
        assertThat(allMemberValues.f()).isEqualTo(1f);
        assertThat(allMemberValues.i()).isEqualTo(1);
        assertThat(allMemberValues.l()).isEqualTo(1L);
        assertThat(allMemberValues.s()).isEqualTo((short) 1);
        assertThat(allMemberValues.value()).isEqualTo("test");

        assertThatThrownBy(() ->
            Javassist.getOrCreate("SomeTypeThatWontCompile", this.getClass(),
                clazz -> clazz.addAnnotation(AllMemberValues.class,
                    values -> values.set("deserializes", Lists.of("whatever"))))
        ).hasMessage("cannot create a ctMember value out of class java.util.ArrayList");
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface AllMemberValues {

        Deserialize deserialize() default @Deserialize(from = "nothing"); // Annotation

        Deserialize[] deserializes() default {}; // Array

        boolean test() default false; // Boolean

        byte b() default -1; // Byte

        char c() default '0'; // Char

        Class<?> clazz() default Void.class; // Class

        double d() default 0d;// Double

        Token token() default Token.ARRAY; // Enum

        float f() default 0f; // Float

        int i() default 0; // Integer

        long l() default 0L; // Long

        short s() default 0; // Short

        String value() default ""; // String
    }

    @Target(CONSTRUCTOR)
    @Retention(RUNTIME)
    public @interface SomeAllAroundAnnotation {

        String value();

    }
}
