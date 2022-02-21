package io.semla.serialization.json;

import io.semla.serialization.Deserializer;
import io.semla.serialization.Serializer;
import io.semla.util.Strings;

import java.lang.reflect.Type;

public class Json {

    private Json() {}

    private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();
    private static final JsonDeserializer JSON_DESERIALIZER = new JsonDeserializer();

    public static JsonDeserializer defaultDeserializer() {
        return JSON_DESERIALIZER;
    }

    public static JsonSerializer defaultSerializer() {
        return JSON_SERIALIZER;
    }

    public static <T> T read(String json, Deserializer.Option... options) {
        return JSON_DESERIALIZER.read(json, options);
    }

    public static <T> T read(String json, Class<T> clazz, Deserializer.Option... options) {
        return JSON_DESERIALIZER.read(json, clazz, options);
    }

    public static <T> T read(String json, Type type, Deserializer.Option... options) {
        return JSON_DESERIALIZER.read(json, type, options);
    }

    public static <T> String write(T object, Serializer.Option... options) {
        return JSON_SERIALIZER.write(object, options);
    }

    public static boolean isJson(String value) {
        return Strings.firstNonWhitespaceCharacterIs(value, '{', '[');
    }
}
