package io.semla.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Stream;


public enum Modifier {

    PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, SYNCHRONIZED, VOLATILE, TRANSIENT, NATIVE, INTERFACE, ABSTRACT, STRICT, ANNOTATION, ENUM;

    private final int value;

    Modifier() {
        this.value = (int) Math.pow(2, this.ordinal());
    }

    public int value() {
        return value;
    }

    public static int valueOf(Modifier... modifiers) {
        return Stream.of(modifiers).map(Modifier::value).reduce(0, (a, b) -> a | b);
    }

    public static boolean is(Class<?> clazz, Modifier... modifiers) {
        return (clazz.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.classModifiers())) != 0;
    }

    public static boolean not(Class<?> clazz, Modifier... modifiers) {
        return (clazz.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.classModifiers())) == 0;
    }

    public static boolean is(Method method, Modifier... modifiers) {
        return (method.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.methodModifiers())) != 0;
    }

    public static boolean not(Method method, Modifier... modifiers) {
        return (method.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.methodModifiers())) == 0;
    }

    public static boolean is(Field field, Modifier... modifiers) {
        return (field.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.fieldModifiers())) != 0;
    }

    public static boolean not(Field field, Modifier... modifiers) {
        return (field.getModifiers() & (valueOf(modifiers) & java.lang.reflect.Modifier.fieldModifiers())) == 0;
    }
}
