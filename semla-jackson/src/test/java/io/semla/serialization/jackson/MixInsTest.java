package io.semla.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.semla.datasource.Datasource;
import io.semla.datasource.InMemoryDatasource;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MixInsTest {

    @Test
    public void mixin() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        objectMapper.addMixIn(
            Datasource.Configuration.class,
            MixIns.createFor(InMemoryDatasource.Configuration.class)
        );

        Datasource.Configuration datasourceConfiguration = objectMapper.readValue("""
                type: in-memory
                """,
            Datasource.Configuration.class
        );

        assertThat(datasourceConfiguration).isNotNull().isInstanceOf(InMemoryDatasource.Configuration.class);
    }

    @Test
    public void write() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        objectMapper.addMixIn(
            Datasource.Configuration.class,
            MixIns.createFor(InMemoryDatasource.Configuration.class)
        );

        InMemoryDatasource.Configuration configuration = new InMemoryDatasource.Configuration();
        String value = objectMapper.writeValueAsString(configuration);
        assertThat(value).isEqualTo("--- !<in-memory> {}\n");
        assertThat(objectMapper.readValue(value, Datasource.Configuration.class)).isNotNull().isInstanceOf(InMemoryDatasource.Configuration.class);
    }

    @Test
    public void mixedMixin() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        assertThatThrownBy(() ->
            objectMapper.addMixIn(
                Datasource.Configuration.class,
                MixIns.createFor(InMemoryDatasource.Configuration.class, OtherClass.class)
            ))
            .hasMessage("classes: " +
                "[class io.semla.datasource.InMemoryDatasource$Configuration, class io.semla.serialization.jackson.MixInsTest$OtherClass] " +
                "don't all share the same unique superType! " +
                "found: [interface io.semla.datasource.Datasource$Configuration, interface io.semla.serialization.jackson.MixInsTest$OtherSuperClass]");
    }

    @TypeInfo
    public interface OtherSuperClass {}

    @TypeName("test")
    public static class OtherClass implements OtherSuperClass {}
}
