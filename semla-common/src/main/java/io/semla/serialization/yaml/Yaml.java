package io.semla.serialization.yaml;

import io.semla.serialization.Deserializer;
import io.semla.serialization.Serializer;

import java.lang.reflect.Type;

public class Yaml {

    private Yaml() {}

    private static final YamlSerializer YAML_SERIALIZER = new YamlSerializer();
    private static final YamlDeserializer YAML_DESERIALIZER = new YamlDeserializer();

    public static YamlDeserializer defaultDeserializer() {
        return YAML_DESERIALIZER;
    }

    public static YamlSerializer defaultSerializer() {
        return YAML_SERIALIZER;
    }

    public static <T> T read(String content, Deserializer.Option... options) {
        return YAML_DESERIALIZER.read(content, options);
    }

    public static <T> T read(String content, Class<T> clazz, Deserializer.Option... options) {
        return YAML_DESERIALIZER.read(content, clazz, options);
    }

    public static <T> T read(String content, Type type, Deserializer.Option... options) {
        return YAML_DESERIALIZER.read(content, type, options);
    }

    public static String write(Object object, Serializer.Option... options) {
        return YAML_SERIALIZER.write(object, options);
    }
}
