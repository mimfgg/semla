package io.semla.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import io.semla.reflect.Annotations;
import io.semla.reflect.Types;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.json.Json;
import io.semla.util.Javassist;
import io.semla.util.Maps;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixIns {

    private static final AtomicInteger mixins = new AtomicInteger();

    private MixIns() {}

    public static Class<?> createFor(Class<?>... classes) {
        Types.registerSubTypes(classes);

        Set<Class<?>> superType = Stream.of(classes)
            .map(clazz -> Types.getParentClassAnnotatedWith(clazz, TypeInfo.class).orElse(null))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Class::getName))
            .collect(Collectors.toCollection(LinkedHashSet::new));


        if (superType.size() > 1) {
            throw new IllegalArgumentException(
                "classes: " + Arrays.toString(classes) + " don't all share the same superType! found: " + superType);
        }


        String property = superType.iterator().next().getAnnotation(TypeInfo.class).property();

        return Javassist.getOrCreate(MixIns.class.getName() + "$$" + mixins.getAndIncrement(),
                MixIns.class,
                builder -> builder
                    .addAnnotation(JsonTypeInfo.class, annotation -> annotation
                        .set("use", JsonTypeInfo.Id.NAME)
                        .set("property", property)
                    )
                    .addAnnotation(JsonSubTypes.class, annotation -> annotation
                        .set("value", Stream.of(classes).map(clazz ->
                            Annotations.proxyOf(JsonSubTypes.Type.class, Maps.of(
                                "value", clazz,
                                "name", clazz.getAnnotation(TypeName.class).value()))).toArray()))
                    .addAnnotation(JsonDeserialize.class, annotation -> annotation
                        .set("using", Deserializer.class))
            );
    }

    // The Deserializer is required because of the semla @Deserialize annotations
    // on the non Java bean setters
    public static class Deserializer<T> extends JsonDeserializer<T>
        implements ContextualDeserializer {

        private Class<T> clazz;

        public Deserializer() {} // required by jackson

        public Deserializer(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T deserialize(final JsonParser jp,
                             final DeserializationContext ctxt)
            throws IOException {
            Map<String, Object> asMap = new LinkedHashMap<>();
            asMap.put("type", clazz.getAnnotation(TypeName.class).value());
            while (!jp.currentToken().equals(JsonToken.END_OBJECT)) {
                jp.nextToken();
                asMap.put(jp.currentName(), jp.getValueAsString());
                jp.nextToken();
            }
            // let's write/read it again
            return Json.read(Json.write(asMap), clazz);
        }

        @Override
        public JsonDeserializer<?> createContextual(final DeserializationContext ctxt,
                                                    final BeanProperty property) {
            JavaType type = ctxt.getContextualType() != null
                ? ctxt.getContextualType()
                : property.getMember().getType();
            return new Deserializer<>(type.getRawClass());
        }
    }
}
