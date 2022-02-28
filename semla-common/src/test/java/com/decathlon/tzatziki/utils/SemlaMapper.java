package com.decathlon.tzatziki.utils;

import io.semla.serialization.Serializer;
import io.semla.serialization.json.Json;
import io.semla.serialization.yaml.Yaml;

import java.lang.reflect.Type;
import java.util.List;

import static com.decathlon.tzatziki.utils.Mapper.isJson;

public class SemlaMapper implements MapperDelegate {

    @Override
    public <E> E read(String content) {
        return isJson(content) ? Json.read(content) : Yaml.read(content);
    }

    @Override
    public <E> List<E> readAsAListOf(String content, Class<E> clazz) {
        return read(content, Types.parameterized(List.class).of(clazz));
    }

    @Override
    public <E> E read(String content, Class<E> clazz) {
        return isJson(content) ? Json.read(content, clazz) : Yaml.read(content, clazz);
    }

    @Override
    public <E> E read(String content, Type type) {
        return isJson(content) ? Json.read(content, type) : Yaml.read(content, type);
    }

    @Override
    public String toJson(Object object) {
        String value = Json.write(object);
        if (value.charAt(0) == '\"' && value.charAt(value.length() - 1) == '\"') {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public String toNonDefaultJson(Object object) {
        return Json.write(object, Serializer.NON_DEFAULT);
    }

    @Override
    public String toYaml(Object object) {
        return Yaml.write(object);
    }
}
