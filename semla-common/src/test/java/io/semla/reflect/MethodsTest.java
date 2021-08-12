package io.semla.reflect;

import io.semla.serialization.annotations.Deserialize;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodsTest {

    @Test
    public void findMethodWithProperParameters() {
        assertThat(Methods.findMethod(TestClass.class, "getName", int.class)).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "getName", Integer.class)).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "getName", String.class)).isEmpty();
    }

    @Test
    public void findInheritedMethods() {
        assertThat(Methods.findMethod(TestClass.class, "publicLocalMethod")).isPresent();
        assertThat(Methods.findMethod(EmptyTopClass.class, "publicLocalMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "protectedOverridenMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "privateLocalMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "privateParentMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "protectedInheritedMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "publicInheritedMethod")).isPresent();
        assertThat(Methods.findMethod(TestClass.class, "init")).isPresent();
        assertThat(Methods.findMethod(BaseInterface.class, "getName", int.class)).isEmpty();
        assertThat(Methods.findMethod(BaseInterface.class, "init")).isPresent();

        assertThat(Methods.<String>invoke(new TestClass(), "protectedOverridenMethod")).isEqualTo("protectedOverridenMethod");
        assertThat(Methods.<String>invoke(new TestParentClass(), "protectedOverridenMethod")).isEqualTo("protectedOriginalMethod");

        assertThat(Methods.findMethod(Annotations.defaultOf(Deserialize.class).getClass(), "value")).isPresent();
    }

    @Test
    public void testConcurrentAccess() {
        IntStream.range(1, 100).parallel().forEach(i -> Methods.findMethod(TestClass.class, "init"));
    }

    public interface BaseInterface {

        String getName(int i);

        default void init() {
        }
    }

    public static abstract class AbstractClass {

        public abstract String publicInheritedMethod();
    }

    public static class TestParentClass extends AbstractClass {

        private String privateParentMethod() {
            return "privateParentMethod";
        }

        protected String protectedInheritedMethod() {
            return "protectedInheritedMethod";
        }

        protected String protectedOverridenMethod() {
            return "protectedOriginalMethod";
        }

        public String publicInheritedMethod() {
            return "publicInheritedMethod";
        }
    }

    public static class TestClass extends TestParentClass implements BaseInterface {

        @Override
        public String getName(int i) {
            return "";
        }

        protected String privateLocalMethod() {
            return "privateLocalMethod";
        }

        @Override
        protected String protectedOverridenMethod() {
            return "protectedOverridenMethod";
        }

        public String publicLocalMethod() {
            return "publicLocalMethod";
        }
    }

    public static class EmptyTopClass extends TestClass {}

}
