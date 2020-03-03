package io.semla.serialization;

import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.reflect.Getter;
import io.semla.reflect.Properties;
import io.semla.reflect.Types;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.io.CharacterWriter;
import io.semla.serialization.io.OutputStreamWriter;
import io.semla.serialization.io.StringWriter;
import io.semla.util.Lists;
import io.semla.util.Strings;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Types.*;
import static io.semla.serialization.annotations.When.NEVER;

@SuppressWarnings("unchecked")
public abstract class Serializer<ContextType extends Serializer.Context> {

    private final Map<Type, BiConsumer<ContextType, Object>> writers = new LinkedHashMap<>();
    private final Set<Option> defaultOptions = new LinkedHashSet<>();

    public Set<Option> options() {
        return defaultOptions;
    }

    public String write(Object object, Option... options) {
        CharacterWriter writer = new StringWriter();
        getWriterFor(object).accept(newContext(writer, Option.concat(defaultOptions, options)));
        return writer.toString();
    }

    public void write(Object object, OutputStream outputStream, Option... options) {
        CharacterWriter writer = new OutputStreamWriter(outputStream);
        getWriterFor(object).accept(newContext(writer, Option.concat(defaultOptions, options)));
    }

    protected abstract ContextType newContext(CharacterWriter writer, Set<Option> options);

    protected Consumer<ContextType> getWriterFor(Object value) {
        if (value == null) {
            return this::writeNull;
        } else if (value instanceof Optional) {
            return this.getWriterFor(((Optional<?>) value).orElse(null));
        } else {
            return context -> getWriterForType(value.getClass()).accept(context, value);
        }
    }

    protected BiConsumer<ContextType, Object> getWriterForType(Type type) {
        return writers.computeIfAbsent(type, t -> {
            Class<?> rawType = rawTypeOf(t);
            if (rawType.isArray()) {
                return (c, v) -> writeArray(c, Lists.fromArray(v));
            } else if (isAssignableTo(t, Collection.class)) {
                return (c, v) -> writeArray(c, (Collection<?>) v);
            } else if (isAssignableTo(t, Map.class)) {
                return (c, v) -> writeMap(c, (Map<?, ?>) v);
            } else if (t.equals(String.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((String) o));
            } else if (isAssignableTo(t, Boolean.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((boolean) o));
            } else if (isAssignableTo(t, Integer.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((int) o));
            } else if (isAssignableTo(t, Short.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((short) o));
            } else if (isAssignableTo(t, Byte.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((byte) o));
            } else if (isAssignableTo(t, Long.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((long) o));
            } else if (isAssignableTo(t, Float.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((float) o));
            } else if (isAssignableTo(t, Double.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append((double) o));
            } else if (isAssignableTo(t, Number.class)) {
                return (c, v) -> writeWith(c, v, (contextType, o) -> contextType.writer().append(Strings.toString(o)));
            } else if (isAssignableToOneOf(t, Character.class, Date.class, Temporal.class, Calendar.class, UUID.class) || rawType.isEnum()) {
                return (c, v) -> getWriterForType(String.class).accept(c, Strings.toString(v));
            }
            return this::writeObject;
        });
    }

    protected <T> void writeObject(ContextType context, T object) {
        Model<T> model = Model.of(object);
        if (model instanceof EntityModel && (context.isPrinted(object) || EntityModel.isReference(object))) {
            getWriterFor(EntityModel.keyOf(object)).accept(context);
        } else {
            if (model instanceof EntityModel) {
                context.markAsPrinted(object);
            }
            startObject(context);
            List<Getter<T>> getters = Properties.gettersOf(model.getType());

            AtomicBoolean isFirst = new AtomicBoolean(true);

            Types.getParentClassAnnotatedWith(object.getClass(), TypeInfo.class).ifPresent(superClass -> {
                if (object.getClass().isAnnotationPresent(TypeName.class)) {
                    writeKey(context, superClass.getAnnotation(TypeInfo.class).property());
                    getWriterFor(object.getClass().getAnnotation(TypeName.class).value()).accept(context);
                }
            });

            getters.forEach(getter -> {
                if (getter.serializeWhen() != NEVER) {
                    Object value = getter.getOn(object);
                    switch (getter.serializeWhen()) {
                        case NOT_NULL:
                            if (value == null) {
                                return;
                            }
                            break;
                        case NOT_EMPTY:
                            if (value != null
                                && ((isAssignableToOneOf(value.getClass(), Collection.class) && ((Collection<?>) value).isEmpty())
                                || (isAssignableToOneOf(value.getClass(), Map.class) && ((Map<?, ?>) value).isEmpty()))
                            ) {
                                return;
                            }
                            break;
                        case NOT_DEFAULT:
                            if (model.isDefaultValue(getter, value)) {
                                return;
                            }
                            break;
                        default:
                            break;
                    }
                    if (!isFirst.getAndSet(false)) {
                        next(context);
                    }
                    writeKey(context, Optional.of(getter.serializeAs()).filter(as -> !as.equals("")).orElse(getter.getName()));
                    getWriterFor(value).accept(context);
                }
            });
            endObject(context);
        }
    }

    protected void writeWith(ContextType context, Object value, BiConsumer<ContextType, Object> writer) {
        writer.accept(context, value);
    }

    protected void writeNull(ContextType context) {
        context.writer().append("null");
    }

    protected void writeMap(ContextType context, Map<?, ?> map) {
        startObject(context);
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            writeKey(context, entry.getKey());
            getWriterFor(entry.getValue()).accept(context);
            if (iterator.hasNext()) {
                next(context);
            }
        }
        endObject(context);
    }

    protected void writeArray(ContextType context, Collection<?> values) {
        startArray(context);
        Iterator<?> iterator = values.iterator();
        if (iterator.hasNext()) {
            writeArrayComponent(context, iterator.next());
            while (iterator.hasNext()) {
                next(context);
                writeArrayComponent(context, iterator.next());
            }
        }
        endArray(context);
    }

    protected abstract void writeArrayComponent(ContextType context, Object value);

    protected abstract void next(ContextType context);

    protected abstract void startObject(ContextType context);

    protected abstract void endObject(ContextType context);

    protected abstract void writeKey(ContextType context, Object key);

    protected abstract void endArray(ContextType context);

    protected abstract void startArray(ContextType context);

    public <E> WriterHandler<E> write(Class<E> clazz) {
        return new WriterHandler<>(clazz);
    }

    public class WriterHandler<E> {

        private final Class<E> clazz;

        public WriterHandler(Class<E> clazz) {
            this.clazz = clazz;
        }

        public <SerializerType extends Serializer<?>> SerializerType with(BiConsumer<ContextType, E> consumer) {
            writers.put(clazz, (BiConsumer<ContextType, Object>) consumer);
            return (SerializerType) Serializer.this;
        }

        public <SerializerType extends Serializer<?>, R> SerializerType as(Function<E, R> function) {
            writers.put(clazz, (contextType, o) -> getWriterFor(function.apply((E) o)).accept(contextType));
            return (SerializerType) Serializer.this;
        }
    }

    public static class Context {

        private final Set<Object> printed = new HashSet<>();
        private final CharacterWriter writer;

        public Context(CharacterWriter writer) {
            this.writer = writer;
        }

        public CharacterWriter writer() {
            return writer;
        }

        protected boolean isPrinted(Object object) {
            return printed.contains(object);
        }

        protected <T> void markAsPrinted(T object) {
            printed.add(object);
        }
    }

    public static final class Option {

        public static Set<Option> concat(Set<Option> defaultOptions, Option... options) {
            return Stream.concat(defaultOptions.stream(), Stream.of(options)).collect(Collectors.toSet());
        }
    }
}
