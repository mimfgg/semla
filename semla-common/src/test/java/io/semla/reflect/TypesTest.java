package io.semla.reflect;

import io.semla.model.Child;
import io.semla.model.Parent;
import io.semla.model.Sibbling;
import io.semla.util.Lists;
import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TypesTest {

    @Test
    public void constructTypes() {
        assertThat(Types.parameterized(Optional.class).of(String.class).getTypeName()).isEqualTo("java.util.Optional<java.lang.String>");
        assertThat(Types.parameterized(List.class).of(String.class).getTypeName()).isEqualTo("java.util.List<java.lang.String>");
        assertThat(Types.parameterized(Map.class).of(String.class, String.class).getTypeName()).isEqualTo("java.util.Map<java.lang.String, java.lang.String>");
        assertThat(Types.parameterized(Supplier.class).of(String.class).getTypeName()).isEqualTo("java.util.function.Supplier<java.lang.String>");
        assertThat(Types.parameterized(Map.class).of(String.class, Types.parameterized(List.class).of(String.class)).getTypeName())
            .isEqualTo("java.util.Map<java.lang.String, java.util.List<java.lang.String>>");
        assertThat(Types.parameterized(Optional.class).of(String.class).getOwnerType()).isNull();
        assertThat(Types.parameterized(Map.Entry.class).of(Integer.class, String.class).getOwnerType()).isEqualTo(Map.class);
        assertThatThrownBy(() -> Types.parameterized(Map.class, Integer.class, String.class, String.class))
            .hasMessage("type interface java.util.Map expects 2 arguments but got 3");
    }

    private List<String> list;

    @Test
    public void typeArgument() {
        assertThat(Types.typeArgumentOf(Fields.getField(this.getClass(), "list").getGenericType())).isEqualTo(String.class);
        assertThatThrownBy(() -> Types.typeArgumentOf(Fields.getField(this.getClass(), "list").getGenericType(), 2))
            .hasMessage("java.util.List<java.lang.String> doesn't have a TypeArgument 2");
    }

    @Test
    public void supplierOf() {
        assertThat(Types.supplierOf(Types.parameterized(List.class, String.class)).get()).isInstanceOf(List.class);
        assertThat(Types.supplierOf(Types.parameterized(Set.class, String.class)).get()).isInstanceOf(Set.class);
        assertThat(Types.supplierOf(Types.parameterized(Map.class, String.class, String.class)).get()).isInstanceOf(Map.class);
        assertThat(Types.supplierOf(Types.parameterized(Vector.class, String.class)).get()).isInstanceOf(Vector.class);
        assertThatThrownBy(() -> Types.supplierOf(Types.parameterized(AbstractList.class, String.class)))
            .hasMessage("Cannot create a supplier for class java.util.AbstractList");
    }

    @Test
    public void newInstance() {
        assertThatThrownBy(() -> Types.newInstance(List.class, "bork"))
            .isInstanceOf(NoSuchMethodException.class)
            .hasMessage("java.util.List.<init>(java.lang.String)");
    }

    @Test
    public void registerSubTypes() {
        assertThatThrownBy(() -> Types.registerSubType(String.class))
            .hasMessage("class java.lang.String doesn't have any super class annotated with @TypeInfo");
    }

    @Test
    public void getSubTypes() {
        Types.registerSubType(Child.class);
        assertThat(Types.getSubTypeOf(Parent.class, "type", "child")).isEqualTo(Child.class);
        assertThatThrownBy(() -> Types.getSubTypeOf(Sibbling.class, "type", "child"))
            .hasMessage("no subtype known for interface io.semla.model.Sibbling");
        assertThatThrownBy(() -> Types.getSubTypeOf(Parent.class, "types", "nephew"))
            .hasMessage("no type property 'types' registered for interface io.semla.model.Parent");
        assertThatThrownBy(() -> Types.getSubTypeOf(Parent.class, "type", "nephew"))
            .hasMessage("no subtype 'nephew' registered for interface io.semla.model.Parent");
    }

    @Test
    public void getCommonSuperClass() {
        assertThat(Types.getCommonSuperClass(1, 1)).isEqualTo(Integer.class);
        assertThat(Types.getCommonSuperClass(1, "a")).isEqualTo(Object.class);
        assertThat(Types.getCommonSuperClass(Integer.class, String.class)).isEqualTo(Object.class);
        assertThat(Types.getCommonSuperClass(Integer.class, Double.class)).isEqualTo(Number.class);
        assertThat(Types.getCommonSuperClass(Number.class, Double.class)).isEqualTo(Number.class);
        assertThat(Types.getCommonSuperClass(Double.class, Number.class)).isEqualTo(Number.class);
        assertThat(Types.getCommonSuperClass(Lists.of(1, 2))).isEqualTo(Integer.class);
        assertThat(Types.getCommonSuperClass(Lists.of(1, "a"))).isEqualTo(Object.class);
        assertThat(Types.getCommonSuperClass(Lists.of(1, 2.1d))).isEqualTo(Number.class);
    }

    @Test
    public void testInheritanceDistance() {
        assertThat(Types.computeInheritanceDistanceBetween(Object.class, Object.class)).isEqualTo(0);
        assertThat(Types.computeInheritanceDistanceBetween(List.class, Object.class)).isEqualTo(127);
        assertThat(Types.computeInheritanceDistanceBetween(ArrayList.class, List.class)).isEqualTo(1);
        assertThat(Types.computeInheritanceDistanceBetween(ArrayList.class, Collection.class)).isEqualTo(2);
        assertThat(Types.computeInheritanceDistanceBetween(ArrayList.class, Object.class)).isEqualTo(128);
        assertThat(Types.computeInheritanceDistanceBetween(LinkedHashMap.class, Cloneable.class)).isEqualTo(2);
        assertThatThrownBy(() -> Types.computeInheritanceDistanceBetween(new Class<?>[]{ArrayList.class}, new Class<?>[]{}))
            .hasMessage("[class java.util.ArrayList] and [] don't have the same size!");
        assertThat(Types.computeInheritanceDistanceBetween(new Class<?>[]{ArrayList.class}, new Class<?>[]{List.class})).isEqualTo(1);
    }
}
