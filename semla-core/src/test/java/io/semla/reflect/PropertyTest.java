package io.semla.reflect;

import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.When;
import io.semla.util.Lists;
import org.hibernate.validator.constraints.Length;
import org.junit.Test;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyTest {

    private List<String> list = Lists.empty();

    @Length(max = 3)
    @Serialize(as = "something")
    public List<String> getList() {
        return list;
    }

    @NotNull
    @Deserialize(from = "something")
    public void setList(List<String> list) {
        this.list = list;
    }

    @Test
    public void memberOfField() {
        Member<PropertyTest> member = Member.from(Fields.getField(this.getClass(), "list"));
        assertThat(member.<List<String>>getOn(this)).isEmpty();
        assertThat(member.getDeclaringClass()).isEqualTo(this.getClass());
        assertThat(member.getGenericType()).isEqualTo(Types.parameterized(List.class, String.class));
        assertThat(member.getName()).isEqualTo("list");
        assertThat(member.toGenericString()).isEqualTo("private java.util.List<java.lang.String> io.semla.reflect.PropertyTest.list");
        assertThat(member.serializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(member.serializeAs()).isEqualTo("list");
        assertThat(member.deserializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(member.deserializeFrom()).isEqualTo("list");
        assertThat(member.annotation(Length.class).isPresent()).isFalse();
        assertThat(member.annotation(NotNull.class).isPresent()).isFalse();
        assertThat(member.toString()).isEqualTo("io.semla.reflect.PropertyTest.list");
    }

    @Test
    public void memberOfFieldAndGetterSetter() {
        Member<PropertyTest> member = Member.from(
            Fields.getField(this.getClass(), "list"),
            Methods.getMethod(this.getClass(), "getList"),
            Methods.getMethod(this.getClass(), "setList", List.class)
        );
        assertThat(member.<List<String>>getOn(this)).isEmpty();
        assertThat(member.getDeclaringClass()).isEqualTo(this.getClass());
        assertThat(member.getGenericType()).isEqualTo(Types.parameterized(List.class, String.class));
        assertThat(member.getName()).isEqualTo("list");
        assertThat(member.toGenericString()).isEqualTo("private java.util.List<java.lang.String> io.semla.reflect.PropertyTest.list");
        assertThat(member.serializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(member.serializeAs()).isEqualTo("something");
        assertThat(member.deserializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(member.deserializeFrom()).isEqualTo("something");
        assertThat(member.annotation(Length.class).isPresent()).isTrue();
        assertThat(member.annotation(NotNull.class).isPresent()).isTrue();
        assertThat(member.toString()).isEqualTo("io.semla.reflect.PropertyTest.list");
    }

    @Test
    public void getter() {
        Getter<PropertyTest> getter = Getter.from(Methods.getMethod(this.getClass(), "getList"));
        assertThat(getter.<List<String>>getOn(this)).isEmpty();
        assertThat(getter.getDeclaringClass()).isEqualTo(this.getClass());
        assertThat(getter.getGenericType()).isEqualTo(Types.parameterized(List.class, String.class));
        assertThat(getter.getName()).isEqualTo("something");
        assertThat(getter.toGenericString()).isEqualTo("public java.util.List<java.lang.String> io.semla.reflect.PropertyTest.getList()");
        assertThat(getter.serializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(getter.serializeAs()).isEqualTo("something");
        assertThat(getter.annotation(Length.class).isPresent()).isTrue();
        assertThat(getter.toString()).isEqualTo("io.semla.reflect.PropertyTest.something");
    }

    @Test
    public void setter() {
        Setter<PropertyTest> setter = Setter.from(Methods.getMethod(this.getClass(), "setList", List.class));
        setter.setOn(this, Lists.empty());
        assertThat(setter.getDeclaringClass()).isEqualTo(this.getClass());
        assertThat(setter.getGenericType()).isEqualTo(Types.parameterized(List.class, String.class));
        assertThat(setter.getName()).isEqualTo("something");
        assertThat(setter.toGenericString()).isEqualTo("public void io.semla.reflect.PropertyTest.setList(java.util.List<java.lang.String>)");
        assertThat(setter.deserializeWhen()).isEqualTo(When.ALWAYS);
        assertThat(setter.deserializeFrom()).isEqualTo("something");
        assertThat(setter.annotation(NotNull.class).isPresent()).isTrue();
        assertThat(setter.toString()).isEqualTo("io.semla.reflect.PropertyTest.something");
    }

}