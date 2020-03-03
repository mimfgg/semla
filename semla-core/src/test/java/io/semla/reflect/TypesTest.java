package io.semla.reflect;

import io.semla.config.DatasourceConfiguration;
import io.semla.config.InMemoryDatasourceConfiguration;
import io.semla.datasource.Datasource;
import io.semla.persistence.CacheEntry;
import io.semla.persistence.EntityManager;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TypesTest {

    @Test
    public void constructTypes() {
        assertThat(Types.parameterized(Optional.class, String.class).getTypeName()).isEqualTo("java.util.Optional<java.lang.String>");
        assertThat(Types.parameterized(List.class, String.class).getTypeName()).isEqualTo("java.util.List<java.lang.String>");
        assertThat(Types.parameterized(Map.class, String.class, String.class).getTypeName()).isEqualTo("java.util.Map<java.lang.String, java.lang.String>");
        assertThat(Types.parameterized(EntityManager.class, CacheEntry.class).getTypeName()).isEqualTo("io.semla.persistence.EntityManager<io.semla.persistence.CacheEntry>");
        assertThat(((ParameterizedType) Types.parameterized(Optional.class, String.class)).getOwnerType()).isNull();
        assertThat(((ParameterizedType) Types.parameterized(Map.Entry.class, Integer.class, String.class)).getOwnerType()).isEqualTo(Map.class);
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
        Types.registerSubType(InMemoryDatasourceConfiguration.class);
        assertThat(Types.getSubTypeOf(DatasourceConfiguration.class, "type", "in-memory")).isEqualTo(InMemoryDatasourceConfiguration.class);
        assertThatThrownBy(() -> Types.getSubTypeOf(Datasource.class, "type", "mem"))
            .hasMessage("no subtype known for class io.semla.datasource.Datasource");
        assertThatThrownBy(() -> Types.getSubTypeOf(DatasourceConfiguration.class, "types", "in-memory"))
            .hasMessage("no type property 'types' registered for interface io.semla.config.DatasourceConfiguration");
        assertThatThrownBy(() -> Types.getSubTypeOf(DatasourceConfiguration.class, "type", "mem"))
            .hasMessage("no subtype 'mem' registered for interface io.semla.config.DatasourceConfiguration");
    }
}
