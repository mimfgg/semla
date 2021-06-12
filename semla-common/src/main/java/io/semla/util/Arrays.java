package io.semla.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class Arrays {

    private Arrays() {}

    public static Object[] emptyIfNull(Object[] objects) {
        return objects == null ? new Object[0] : objects;
    }

    public static <E> E[] box(Object value) {
        if (!value.getClass().isArray()) {
            throw new IllegalArgumentException(value + " is not an array");
        }
        Class<?> componentType = value.getClass().getComponentType();
        if (componentType.isPrimitive()) {
            switch (componentType.getSimpleName()) {
                case "long":
                    return (E[]) box((long[]) value);
                case "int":
                    return (E[]) box((int[]) value);
                case "short":
                    return (E[]) box((short[]) value);
                case "char":
                    return (E[]) box((char[]) value);
                case "byte":
                    return (E[]) box((byte[]) value);
                case "boolean":
                    return (E[]) box((boolean[]) value);
                case "float":
                    return (E[]) box((float[]) value);
                case "double":
                    return (E[]) box((double[]) value);
            }
        }
        return (E[]) value;
    }

    public static Long[] box(long[] value) {
        Long[] boxed = new Long[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Integer[] box(int[] value) {
        Integer[] boxed = new Integer[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Short[] box(short[] value) {
        Short[] boxed = new Short[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Character[] box(char[] value) {
        Character[] boxed = new Character[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Byte[] box(byte[] value) {
        Byte[] boxed = new Byte[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Boolean[] box(boolean[] value) {
        Boolean[] boxed = new Boolean[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Float[] box(float[] value) {
        Float[] boxed = new Float[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Double[] box(double[] value) {
        Double[] boxed = new Double[value.length];
        for (int i = 0; i < value.length; i++) {
            boxed[i] = value[i];
        }
        return boxed;
    }

    public static Object unbox(Object[] value) {
        Class<?> componentType = value.getClass().getComponentType();
        if (!componentType.isPrimitive()) {
            switch (componentType.getSimpleName()) {
                case "Long":
                    return unbox((Long[]) value);
                case "Integer":
                    return unbox((Integer[]) value);
                case "Short":
                    return unbox((Short[]) value);
                case "Character":
                    return unbox((Character[]) value);
                case "Byte":
                    return unbox((Byte[]) value);
                case "Boolean":
                    return unbox((Boolean[]) value);
                case "Float":
                    return unbox((Float[]) value);
                case "Double":
                    return unbox((Double[]) value);
            }
        }
        return value;
    }

    public static long[] unbox(Long[] value) {
        long[] unboxed = new long[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static int[] unbox(Integer[] value) {
        int[] unboxed = new int[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static short[] unbox(Short[] value) {
        short[] unboxed = new short[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static char[] unbox(Character[] value) {
        char[] unboxed = new char[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static byte[] unbox(Byte[] value) {
        byte[] unboxed = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static boolean[] unbox(Boolean[] value) {
        boolean[] unboxed = new boolean[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static float[] unbox(Float[] value) {
        float[] unboxed = new float[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static double[] unbox(Double[] value) {
        double[] unboxed = new double[value.length];
        for (int i = 0; i < value.length; i++) {
            unboxed[i] = value[i];
        }
        return unboxed;
    }

    public static Object toArray(Collection<?> collection, Class<?> componentType) {
        Object array = Array.newInstance(componentType, collection.size());
        int i = 0;
        for (Object o : collection) {
            Array.set(array, i++, o);
        }
        return array;
    }

    @SafeVarargs
    public static <E> E[] of(E... values) {
        return values;
    }

    @SafeVarargs
    public static <E> E[] toArray(E first, E... others) {
        Object array = Array.newInstance(others.getClass().getComponentType(), others.length + 1);
        Array.set(array, 0, first);
        for (int i = 0; i < others.length; i++) {
            Array.set(array, i + 1, others[i]);
        }
        return (E[]) array;
    }

    public static <E> Stream<E> toStream(Object value) {
        if (!value.getClass().isArray()) {
            throw new IllegalArgumentException(value + " is not an array");
        }
        if (value.getClass().getComponentType().isPrimitive()) {
            value = Arrays.box(value);
        }
        return Stream.of((E[]) value);
    }

    public static <E> boolean in(E value, E... values) {
        for (E e : values) {
            if (e.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static <E> E[] concat(E[] first, E[] second) {
        E[] array = (E[]) Array.newInstance(first.getClass().getComponentType(), first.length + second.length);
        System.arraycopy(first, 0, array, 0, first.length);
        System.arraycopy(second, 0, array, first.length, second.length);
        return array;
    }
}


