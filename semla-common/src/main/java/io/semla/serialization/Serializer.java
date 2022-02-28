package io.semla.serialization;

import io.semla.model.Model;
import io.semla.reflect.Getter;
import io.semla.reflect.Property;
import io.semla.reflect.Types;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.annotations.When;
import io.semla.serialization.io.CharacterWriter;
import io.semla.serialization.io.OutputStreamWriter;
import io.semla.serialization.io.StringWriter;
import io.semla.util.*;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Properties.gettersOf;
import static io.semla.reflect.Types.*;
import static io.semla.serialization.annotations.When.NEVER;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;

@SuppressWarnings("unchecked")
public abstract class Serializer<ContextType extends Serializer<?>.Context> {

    private static final Map<Predicate<Type>, BiConsumer<Serializer<?>.Context, Object>> CUSTOM_WRITERS = new LinkedHashMap<>();
    private final Map<Type, BiConsumer<ContextType, Object>> writers = new LinkedHashMap<>();
    private final Set<Option> defaultOptions = new LinkedHashSet<>();

    public Set<Option> defaultOptions() {
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

    public Consumer<ContextType> getWriterFor(Object value) {
        if (value == null) {
            return this::writeNull;
        } else if (value instanceof Optional optional) {
            return this.getWriterFor(optional.orElse(null));
        } else {
            return context -> getWriterForType(value.getClass()).accept(context, value);
        }
    }

    protected BiConsumer<ContextType, Object> getWriterForType(Type type) {
        return writers.computeIfAbsent(type, t ->
            CUSTOM_WRITERS.entrySet().stream()
                .filter(e -> e.getKey().test(t))
                .map(Map.Entry::getValue)
                .map(Types::<BiConsumer<ContextType, Object>>cast)
                .findFirst()
                .orElseGet(() -> {
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
                }));
    }

    protected <T> void writeObject(ContextType context, T object) {
        Model<T> model = Model.of(object);
        startObject(context);

        AtomicBoolean isFirst = new AtomicBoolean(true);

        Types.getParentClassAnnotatedWith(object.getClass(), TypeInfo.class).ifPresent(superClass -> {
            if (object.getClass().isAnnotationPresent(TypeName.class)) {
                writeKey(context, superClass.getAnnotation(TypeInfo.class).property());
                getWriterFor(object.getClass().getAnnotation(TypeName.class).value()).accept(context);
                next(context);
            }
        });

        List<Getter<T>> getters;
        if (context.sortAlphabetically()) {
            getters = Singleton.named("Serializer.sortedGettersOf." + model.getType(),
                gettersOf(model.getType()).stream().sorted(comparing(Property::getName))::toList
            ).get();
        } else {
            getters = Singleton.named("Serializer.gettersOf." + model.getType(),
                () -> {
                    List<Getter<T>> original = new ArrayList<>(gettersOf(model.getType()));
                    Map<Integer, Getter<T>> fixedIndexes = new LinkedHashMap<>();
                    for (int i = 0; i < original.size(); i++) {
                        Optional<Serialize> serialize = original.get(i).annotation(Serialize.class);
                        if (serialize.isPresent() && serialize.get().order() > -1) {
                            fixedIndexes.put(serialize.get().order(), original.get(i));
                            original.remove(i--);
                        }
                    }
                    if (!fixedIndexes.isEmpty()) {
                        fixedIndexes.entrySet().stream()
                            .sorted(comparingInt(Map.Entry::getKey))
                            .forEach(e -> {
                                original.add(e.getKey(), e.getValue());
                            });
                    }
                    return ImmutableList.copyOf(original);
                }
            ).get();
        }
        getters.forEach(getter -> {
            When serializeWhen = context.serializeWhen().orElseGet(getter::serializeWhen);
            if (serializeWhen != NEVER) {
                Object value = getter.getOn(object);
                switch (serializeWhen) {
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

    public abstract class Context {

        private final Set<Object> serialized = new HashSet<>();
        private final CharacterWriter writer;
        private final boolean sortAlphabetically;
        private final When serializeWhen;

        public Context(CharacterWriter writer, Set<Option> options) {
            this.writer = writer;
            this.sortAlphabetically = options.contains(SORT_ALPHABETICALLY);
            if (options.contains(NON_DEFAULT)) {
                serializeWhen = When.NOT_DEFAULT;
            } else {
                serializeWhen = null;
            }
        }

        public CharacterWriter writer() {
            return writer;
        }

        public boolean sortAlphabetically() {
            return sortAlphabetically;
        }

        public Optional<When> serializeWhen() {
            return ofNullable(serializeWhen);
        }

        public boolean hasBeenSerialized(Object object) {
            return serialized.contains(object);
        }

        public void markAsSerialized(Object object) {
            serialized.add(object);
        }

        public void write(Object object) {
            Serializer.this.getWriterFor(object).accept((ContextType) this);
        }

        public void writeObject(Object object) {
            Serializer.this.writeObject((ContextType) this, object);
        }
    }

    public static final class Option {

        public static Set<Option> concat(Set<Option> defaultOptions, Option... options) {
            return Stream.concat(defaultOptions.stream(), Stream.of(options)).collect(Collectors.toSet());
        }
    }

    public static final Option SORT_ALPHABETICALLY = new Option();

    public static final Option NON_DEFAULT = new Option();

    public static WithBuilder<BiConsumer<Serializer<?>.Context, Object>> write(Predicate<Type> predicate) {
        return new WithBuilder<>(writer -> CUSTOM_WRITERS.put(predicate, writer));
    }
}
