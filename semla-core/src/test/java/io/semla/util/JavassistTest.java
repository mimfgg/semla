package io.semla.util;

import io.semla.model.Player;
import io.semla.reflect.Fields;
import io.semla.reflect.Methods;
import io.semla.reflect.Modifier;
import io.semla.reflect.Types;
import org.junit.Test;

import javax.inject.Inject;
import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Class<?> someTypeWithAnAnnotationWithAnEnum =
            Javassist.getOrCreate("SomeTypeWithAnAnnotationWithAnEnum",
                clazz -> clazz.addField("player", Player.class,
                    field -> field.addAnnotation(OneToOne.class,
                        oneToOne -> oneToOne.set("fetch", FetchType.LAZY))));
        assertThat(Fields.getField(someTypeWithAnAnnotationWithAnEnum, "player").getAnnotation(OneToOne.class).fetch()).isEqualTo(FetchType.LAZY);
        assertThatThrownBy(() ->
            Javassist.getOrCreate("SomeTypeThatWontCompile",
                clazz -> clazz.addField("player", Player.class,
                    field -> field.addAnnotation(OneToOne.class,
                        oneToOne -> oneToOne.set("fetch", 1))))
        ).hasMessage("cannot create a ctMember value out of 1");
    }


}