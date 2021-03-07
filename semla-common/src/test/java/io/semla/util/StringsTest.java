package io.semla.util;

import io.semla.reflect.Fields;
import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class StringsTest {

    @Test
    public void a_default_parser_can_be_overriden() {
        assertThat(Strings.parse("3", Integer.class)).isEqualTo(3);
        Strings.parseType(Integer.class).with(s -> Integer.parseInt(s.substring(1, 2)));
        assertThat(Strings.parse("(3)", Integer.class)).isEqualTo(3);
        // resetting
        Strings.parseType(Integer.class).with(Integer::parseInt);

    }

    @Test
    public void a_default_stringifier_can_be_overriden() {
        assertThat(Strings.toString(3)).isEqualTo("3");
        Strings.writeType(Integer.class).with(o -> "(" + o + ")");
        assertThat(Strings.toString(3)).isEqualTo("(3)");
        // resetting
        Strings.writeType(Integer.class).with(String::valueOf);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void prefixIfNotNullOrEmpty() {
        assertThat(Strings.prefixIfNotNullOrEmpty("-", null)).isNull();
        assertThat(Strings.prefixIfNotNullOrEmpty("-", "")).isEmpty();
        assertThat(Strings.prefixIfNotNullOrEmpty("-", "test")).isEqualTo("-test");
    }

    @Test
    public void defaultIfEmptyOrNull() {
        assertThat(Strings.defaultIfEmptyOrNull("-", null)).isEqualTo("-");
        assertThat(Strings.defaultIfEmptyOrNull("-", "")).isEqualTo("-");
    }

    @Test
    public void decapitalize() {
        assertThat(Strings.decapitalize("")).isEqualTo("");
        assertThat(Strings.decapitalize("T")).isEqualTo("t");
        assertThat(Strings.decapitalize("Test")).isEqualTo("test");
        assertThat(Strings.decapitalize("TestOfMethod")).isEqualTo("testOfMethod");
    }

    @Test
    public void capitalize() {
        assertThat(Strings.capitalize("")).isEqualTo("");
        assertThat(Strings.capitalize("t")).isEqualTo("T");
        assertThat(Strings.capitalize("test")).isEqualTo("Test");
        assertThat(Strings.capitalize("testOfMethod")).isEqualTo("TestOfMethod");
    }

    @Test
    public void stringify() {
        assertThat(Strings.toString(ImmutableMap.builder().build())).isEqualTo("{}");
        assertThat(Strings.toString(ImmutableMap.of("name", "value"))).isEqualTo("{name: value}");
        assertThat(Strings.toString(Fields.getField(Strings.class, "PARSERS"))).isEqualTo("PARSERS");
    }

    @Test
    public void parse() {
        assertThat(Strings.parse(null, Character.class)).isNull();
        assertThatThrownBy(() -> Strings.parse("test", Character.class)).hasMessage("cannot parse \"test\" into a char");
    }

    @Test
    public void toSnakeCase() {
        assertThat(Strings.toSnakeCase("")).isEqualTo("");
        assertThat(Strings.toSnakeCase("t")).isEqualTo("t");
        assertThat(Strings.toSnakeCase("test")).isEqualTo("test");
        Stream.of("testing something", "testingSomething", "testing_something")
            .map(Strings::toSnakeCase).forEach(output -> assertThat(output).isEqualTo("testing_something"));
    }

    @Test
    public void toCamelCase() {
        assertThat(Strings.toCamelCaseCase("")).isEqualTo("");
        assertThat(Strings.toCamelCaseCase("t")).isEqualTo("t");
        assertThat(Strings.toCamelCaseCase("test")).isEqualTo("test");
        Stream.of("testing something", "testingSomething", "testing_something")
            .map(Strings::toCamelCaseCase).forEach(output -> assertThat(output).isEqualTo("testingSomething"));
    }

    @Test
    public void until() {
        assertThat(Strings.until("test", 'm')).isEqualTo("");
        assertThat(Strings.until("test", 's')).isEqualTo("te");
    }

    @Test
    public void firstNonWhitespaceCharacterIs() {
        assertThat(Strings.firstNonWhitespaceCharacterIs("    {}", '{')).isTrue();
        assertThat(Strings.firstNonWhitespaceCharacterIs("    {}", '[')).isFalse();
    }

}
