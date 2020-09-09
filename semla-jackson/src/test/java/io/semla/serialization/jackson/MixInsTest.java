package io.semla.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.semla.config.DatasourceConfiguration;
import io.semla.config.InMemoryDatasourceConfiguration;
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
            DatasourceConfiguration.class,
            MixIns.createFor(InMemoryDatasourceConfiguration.class)
        );

        DatasourceConfiguration datasourceConfiguration = objectMapper.readValue(
            "type: in-memory\n",
            DatasourceConfiguration.class);
        assertThat(datasourceConfiguration).isNotNull().isInstanceOf(InMemoryDatasourceConfiguration.class);
    }

    @Test
    public void mixedMixin() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        assertThatThrownBy(() ->
            objectMapper.addMixIn(
                DatasourceConfiguration.class,
                MixIns.createFor(InMemoryDatasourceConfiguration.class, OtherClass.class)
            ))
            .hasMessage("classes: " +
                "[class io.semla.config.InMemoryDatasourceConfiguration, class io.semla.serialization.jackson.MixInsTest$OtherClass] " +
                "don't all share the same superType! " +
                "found: [interface io.semla.config.DatasourceConfiguration, interface io.semla.serialization.jackson.MixInsTest$OtherSuperClass]");
    }

    @TypeInfo
    public interface OtherSuperClass {}

    @TypeName("test")
    public static class OtherClass implements OtherSuperClass {}
}