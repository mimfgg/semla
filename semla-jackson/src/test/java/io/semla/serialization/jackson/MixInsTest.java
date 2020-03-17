package io.semla.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.semla.config.DatasourceConfiguration;
import io.semla.config.PostgresqlDatasourceConfiguration;
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
            MixIns.createFor(PostgresqlDatasourceConfiguration.class)
        );

        DatasourceConfiguration datasourceConfiguration = objectMapper.readValue(
            "type: postgresql\n" +
                "jdbcUrl: jdbc:postgresql://host:port/database\n" +
                "username: username\n" +
                "password: password",
            DatasourceConfiguration.class);
        assertThat(datasourceConfiguration).isNotNull().isInstanceOf(PostgresqlDatasourceConfiguration.class);
    }

    @Test
    public void mixedMixin() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        assertThatThrownBy(() ->
            objectMapper.addMixIn(
                DatasourceConfiguration.class,
                MixIns.createFor(PostgresqlDatasourceConfiguration.class, OtherClass.class)
            )
        ).hasMessage("classes: " +
            "[class io.semla.config.PostgresqlDatasourceConfiguration, class io.semla.serialization.jackson.MixInsTest$OtherClass] " +
            "don't all share the same superType! " +
            "found: [interface io.semla.config.DatasourceConfiguration, interface io.semla.serialization.jackson.MixInsTest$OtherSuperClass]");
    }

    @TypeInfo
    public interface OtherSuperClass {}

    @TypeName("test")
    public static class OtherClass implements OtherSuperClass {}
}