package io.semla.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;


public class ArraysTest {

    @Test
    public void concat() {
        assertThat(Arrays.concat(new String[]{"a", "b"}, new String[]{"c", "d"})).isEqualTo(new String[]{"a", "b", "c", "d"});
    }

    @Test
    public void emptyIfNull() {
        assertThat(Arrays.emptyIfNull(null)).isEqualTo(new Object[0]);
        assertThat(Arrays.emptyIfNull(new Object[]{1})).isEqualTo(new Object[]{1});
    }

    @Test
    public void box() {
        assertThat(catchThrowableOfType(() -> Arrays.box(1), IllegalArgumentException.class)).hasMessage("1 is not an array");
        assertThat(Arrays.box((Object) new int[]{1}).getClass().getComponentType()).isEqualTo(Integer.class);
        assertThat(Arrays.box((Object) new long[]{1L}).getClass().getComponentType()).isEqualTo(Long.class);
        assertThat(Arrays.box((Object) new short[]{1}).getClass().getComponentType()).isEqualTo(Short.class);
        assertThat(Arrays.box((Object) new char[]{'a'}).getClass().getComponentType()).isEqualTo(Character.class);
        assertThat(Arrays.box((Object) new byte[]{1}).getClass().getComponentType()).isEqualTo(Byte.class);
        assertThat(Arrays.box((Object) new boolean[]{true}).getClass().getComponentType()).isEqualTo(Boolean.class);
        assertThat(Arrays.box((Object) new float[]{1f}).getClass().getComponentType()).isEqualTo(Float.class);
        assertThat(Arrays.box((Object) new double[]{1d}).getClass().getComponentType()).isEqualTo(Double.class);
        assertThat(Arrays.box(new Object[]{"test"}).getClass().getComponentType()).isEqualTo(Object.class);
    }

    @Test
    public void unbox() {
        assertThat(Arrays.unbox((Object[]) new Integer[]{1}).getClass().getComponentType()).isEqualTo(int.class);
        assertThat(Arrays.unbox((Object[]) new Long[]{1L}).getClass().getComponentType()).isEqualTo(long.class);
        assertThat(Arrays.unbox((Object[]) new Short[]{1}).getClass().getComponentType()).isEqualTo(short.class);
        assertThat(Arrays.unbox((Object[]) new Character[]{'a'}).getClass().getComponentType()).isEqualTo(char.class);
        assertThat(Arrays.unbox((Object[]) new Byte[]{1}).getClass().getComponentType()).isEqualTo(byte.class);
        assertThat(Arrays.unbox((Object[]) new Boolean[]{true}).getClass().getComponentType()).isEqualTo(boolean.class);
        assertThat(Arrays.unbox((Object[]) new Float[]{1f}).getClass().getComponentType()).isEqualTo(float.class);
        assertThat(Arrays.unbox((Object[]) new Double[]{1d}).getClass().getComponentType()).isEqualTo(double.class);
        assertThat(Arrays.unbox(new Object[]{"test"}).getClass().getComponentType()).isEqualTo(Object.class);
    }

    @Test
    public void toArray() {
        assertThat(Arrays.toArray(Lists.of(1, 2, 3), int.class)).isEqualTo(new int[]{1, 2, 3});
    }

    @Test
    public void toStream() {
        assertThat(Arrays.toStream(new int[]{0, 1, 2, 3}).count()).isEqualTo(4);
        assertThatThrownBy(() -> Arrays.toStream(1)).hasMessage("1 is not an array");
    }
}
