package io.semla.cucumber.steps;

import io.semla.serialization.json.Json;
import io.semla.serialization.yaml.Yaml;

@SuppressWarnings("unchecked")
public class Mapper {

    public static <T> T deserialize(String content, Class<T> clazz) {
        if (clazz.equals(String.class)) {
            return (T) content;
        }
        if (Json.isJson(content)) {
            return Json.read(content, clazz);
        }
        // assume YAML
        return Yaml.read(content, clazz);
    }

    public static String serialize(Object object) {
        return Json.write(object);
    }
}
