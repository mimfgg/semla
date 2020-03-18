package io.semla.util;

import io.semla.model.Player;
import io.semla.reflect.*;
import org.junit.Test;

import javax.inject.Inject;
import javax.persistence.*;
import javax.validation.constraints.Max;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class JavassistTest {

    @Test
    public void extending() {
        Class<? extends Map<String, Object>> extendedLinkedHashMap =
            Javassist.getOrCreate("ExtendedLinkedHashMap", clazz -> clazz.extending(LinkedHashMap.class));
        Map<String, Object> map = Types.newInstance(extendedLinkedHashMap);
        Class<? extends Map<String, Object>> extendedLinkedHashMap2 =
            Javassist.getOrCreate("ExtendedLinkedHashMap", clazz -> clazz.extending(LinkedHashMap.class));
        assertThat(extendedLinkedHashMap2).isEqualTo(extendedLinkedHashMap);
    }

    @Test
    public void implementing() {
        Class<?> someSerializableType =
            Javassist.getOrCreate("SomeSerializableType", clazz -> clazz.implementing(Serializable.class));
        Object o = Types.newInstance(someSerializableType);
        assertThat(o).isInstanceOf(Serializable.class);
    }

    @Test
    public void addAnnotation() {
        Class<?> someAnnotatedType =
            Javassist.getOrCreate("SomeAnnotatedType", clazz -> clazz.addAnnotation(Entity.class));
        assertThat(someAnnotatedType).hasAnnotation(Entity.class);
        Class<?> someAnnotatedTypeWithATable =
            Javassist.getOrCreate("SomeAnnotatedTypeWithATable",
                clazz -> clazz.addAnnotation(Entity.class, annotation -> annotation.set("name", "test")));
        assertThat(someAnnotatedTypeWithATable).hasAnnotation(Entity.class);
        assertThat(someAnnotatedTypeWithATable.getAnnotation(Entity.class).name()).isEqualTo("test");
    }

    @Test
    public void addField() {
        Class<?> someTypeWithAField =
            Javassist.getOrCreate("SomeTypeWithAField", clazz -> clazz.addField("value", String.class));
        assertThat(Fields.getField(someTypeWithAField, "value")).isNotNull();
        Class<?> someTypeWithAPrivateField =
            Javassist.getOrCreate("SomeTypeWithAPrivateField",
                clazz -> clazz.addField("value", String.class,
                    field -> field.setModifier(Modifier.PRIVATE)));
        Field privateField = Fields.getField(someTypeWithAPrivateField, "value");
        assertThat(privateField).isNotNull();
        assertThat(privateField.getModifiers() & java.lang.reflect.Modifier.PRIVATE).isEqualTo(java.lang.reflect.Modifier.PRIVATE);
        Class<?> someTypeWithAnAnnotatedField =
            Javassist.getOrCreate("SomeTypeWithAnAnnotatedField",
                clazz -> clazz.addField("id", Integer.class, field -> field.addAnnotation(Id.class)));
        assertThat(Fields.getField(someTypeWithAnAnnotatedField, "id").isAnnotationPresent(Id.class)).isTrue();
    }

    @Test
    public void addMethod() {
        Class<?> someTypeWithAMethod =
            Javassist.getOrCreate("SomeTypeWithAMethod", clazz -> clazz.addMethod("public String test(){return \"hello world!\";}"));
        assertThat(Methods.<String>invoke(Types.newInstance(someTypeWithAMethod), "test")).isEqualTo("hello world!");
        Class<?> someTypeWithAnAnnotatedMethod =
            Javassist.getOrCreate("SomeTypeWithAnAnnotatedMethod",
                clazz -> clazz.addMethod("public String test(){return \"hello world!\";}",
                    method -> method.addAnnotation(Transient.class)));
        assertThat(Methods.findMethod(someTypeWithAnAnnotatedMethod, "test").get().isAnnotationPresent(Transient.class)).isTrue();
    }

    @Test
    public void addConstructor() throws NoSuchMethodException {
        Class<?> someTypeWithAConstructor =
            Javassist.getOrCreate("SomeTypeWithAConstructor", clazz -> clazz.addConstructor("public SomeTypeWithAConstructor(int id){}"));
        assertThat(someTypeWithAConstructor.getConstructor(int.class)).isNotNull();

        Class<?> someTypeWithAnAnnotatedConstructor =
            Javassist.getOrCreate("SomeTypeWithAnAnnotatedConstructor",
                clazz -> clazz.addConstructor("public SomeTypeWithAnAnnotatedConstructor(int id){}",
                    constructor -> constructor.addAnnotation(Inject.class)));
        assertThat(someTypeWithAnAnnotatedConstructor.getConstructor(int.class).isAnnotationPresent(Inject.class)).isTrue();
    }

    @Test
    public void annotations() {
        Max maximum = Annotations.proxyOf(Max.class, Maps.of("value", 1L));
        Max[] maxima = Arrays.of(Annotations.proxyOf(Max.class, Maps.of("value", 2L)));

        Class<?> someAnnotatedType =
            Javassist.getOrCreate("SomeTypeAnnotatedWithAllMemberValues",
                clazz -> clazz
                    .addAnnotation(Entity.class)
                    .addAnnotation(AllMemberValues.class, annotation -> annotation
                        .set("maximum", maximum)
                        .set("maxima", maxima)
                        .set("test", true)
                        .set("b", (byte) 1)
                        .set("c", 'p')
                        .set("clazz", Entity.class)
                        .set("d", 1d)
                        .set("fetchType", FetchType.EAGER)
                        .set("f", 1f)
                        .set("i", 1)
                        .set("l", 1L)
                        .set("s", (short) 1)
                        .set("value", "test")
                    ));
        AllMemberValues allMemberValues = someAnnotatedType.getAnnotation(AllMemberValues.class);
        assertThat(allMemberValues.maximum()).isEqualTo(maximum);
        assertThat(allMemberValues.maxima()).isEqualTo(maxima);
        assertThat(allMemberValues.test()).isEqualTo(true);
        assertThat(allMemberValues.b()).isEqualTo((byte) 1);
        assertThat(allMemberValues.c()).isEqualTo('p');
        assertThat(allMemberValues.clazz()).isEqualTo(Entity.class);
        assertThat(allMemberValues.d()).isEqualTo(1d);
        assertThat(allMemberValues.fetchType()).isEqualTo(FetchType.EAGER);
        assertThat(allMemberValues.f()).isEqualTo(1f);
        assertThat(allMemberValues.i()).isEqualTo(1);
        assertThat(allMemberValues.l()).isEqualTo(1L);
        assertThat(allMemberValues.s()).isEqualTo((short) 1);
        assertThat(allMemberValues.value()).isEqualTo("test");

        assertThatThrownBy(() ->
            Javassist.getOrCreate("SomeTypeThatWontCompile",
                clazz -> clazz.addField("player", Player.class,
                    field -> field.addAnnotation(OneToOne.class,
                        oneToOne -> oneToOne.set("fetch", Lists.of(1)))))
        ).hasMessage("cannot create a ctMember value out of class java.util.ArrayList");
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface AllMemberValues {

        Max maximum() default @Max(value = 0L); // Annotation

        Max[] maxima() default {}; // Array

        boolean test() default false; // Boolean

        byte b() default -1; // Byte

        char c() default '0'; // Char

        Class<?> clazz() default Void.class; // Class

        double d() default 0d;// Double

        FetchType fetchType() default FetchType.LAZY; // Enum

        float f() default 0f; // Float

        int i() default 0; // Integer

        long l() default 0L; // Long

        short s() default 0; // Short

        String value() default ""; // String


    }


}