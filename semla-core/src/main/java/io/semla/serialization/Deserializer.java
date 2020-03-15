package io.semla.serialization;

import io.semla.exception.DeserializationException;
import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.persistence.EntityContext;
import io.semla.reflect.Annotations;
import io.semla.reflect.Properties;
import io.semla.reflect.Setter;
import io.semla.reflect.Types;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.When;
import io.semla.serialization.io.CharacterReader;
import io.semla.serialization.io.InputStreamReader;
import io.semla.serialization.io.StringReader;
import io.semla.util.Arrays;
import io.semla.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.util.TypeLiteral;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Types.*;
import static io.semla.serialization.Token.*;

@SuppressWarnings("unchecked")
public abstract class Deserializer<ContextType extends Deserializer<ContextType>.Context> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<Type, BiFunction<ContextType, Type, ?>> readers = new LinkedHashMap<>();
    private final Set<Option> defaultOptions = new LinkedHashSet<>();

    public Set<Option> options() {
        return defaultOptions;
    }

    protected abstract String read(ContextType context);

    private BiFunction<ContextType, Type, ?> createReader(Token token, Function<ContextType, ?> function) {
        return (context, type) -> {
            if (context.currentOrNext() == NULL) {
                return null;
            }
            if (token == STRING || !context.unwrapStrings()) {
                if (context.current() != PROPERTY || token != STRING) {
                    if (context.current() != token) {
                        throw new DeserializationException("was expecting " + token + " but was " + context.current() + "@" + context.reader().toString());
                    }
                }
            }
            if (context.current() != PROPERTY) {
                context.push(token);
            }
            Object object = function.apply(context);
            if (context.current() != PROPERTY) {
                context.pop();
            }
            return object;
        };
    }

    protected abstract ContextType newContext(CharacterReader reader, Set<Option> options);

    public <E> E read(String content, Option... options) {
        return newContext(new StringReader(content), Option.concat(defaultOptions, options))
            .applyTo(context -> read(context, context.getDefaultTypeFromToken()));
    }

    public <E> E read(String content, TypeLiteral<E> type, Option... options) {
        return read(content, type.getType(), options);
    }

    public <E> E read(String content, Type type, Option... options) {
        return newContext(new StringReader(content), Option.concat(defaultOptions, options))
            .applyTo(context -> read(context, type));
    }

    public <E> E read(InputStream inputStream, Option... options) {
        return newContext(new InputStreamReader(inputStream), Option.concat(defaultOptions, options))
            .applyTo(context -> read(context, context.getDefaultTypeFromToken()));
    }

    public <E> E read(InputStream inputStream, TypeLiteral<E> type, Option... options) {
        return read(inputStream, type.getType(), options);
    }

    public <E> E read(InputStream inputStream, Type type, Option... options) {
        return newContext(new InputStreamReader(inputStream), Option.concat(defaultOptions, options))
            .applyTo(context -> read(context, type));
    }

    protected <E> E read(ContextType context, Type type) {
        if (context.reader().isNull()) {
            return null;
        }
        return (E) readers.computeIfAbsent(type, thatType -> {
            Class<?> rawType = rawTypeOf(thatType);
            if (isAssignableToOneOf(thatType, String.class, Character.class, Date.class, Temporal.class, Calendar.class, UUID.class) || rawType.isEnum()) {
                return createReader(STRING, c -> Strings.parse(read(c), rawType));
            } else if (isAssignableTo(thatType, Number.class)) {
                return createReader(NUMBER, c -> Strings.parse(read(c), rawType));
            } else if (isAssignableTo(thatType, Boolean.class)) {
                return createReader(BOOLEAN, c -> Strings.parse(read(c), rawType));
            } else if (isAssignableTo(thatType, Collection.class)) {
                return (c, t) -> readArray(c,
                    () -> optionalTypeArgumentOf(t).orElseGet(c::getDefaultTypeFromToken), Types.supplierOf(t)
                );
            } else if (rawType.isArray()) {
                return (c, t) -> {
                    Collection<?> collection = readArray(c, () -> Types.wrap(rawTypeOf(t).getComponentType()), Types.supplierOf(List.class));
                    if (collection == null) {
                        return null;
                    }
                    return Arrays.toArray(collection, rawTypeOf(t).getComponentType());
                };
            } else if (rawType.isAnnotation()) {
                return (c, t) ->
                    Annotations.proxyOf((Class<Annotation>) rawType, readMap(c, () -> (Class<Object>) c.getDefaultTypeFromToken(), supplierOf(Map.class)));
            } else if (isAssignableTo(thatType, Map.class)) {
                return (c, t) -> readMap(c,
                    () -> optionalTypeArgumentOf(thatType, 1).orElseGet(c::getDefaultTypeFromToken),
                    Types.supplierOf(thatType)
                );
            } else if (rawType.equals(Optional.class)) {
                return (c, t) -> Optional.ofNullable(read(c, typeArgumentOf(thatType)));
            }
            return (c, t) -> readObject(c, rawType);
        }).apply(context, type);
    }

    protected <T> T readObject(ContextType context, Class<T> clazz) {
        if (logger.isTraceEnabled()) {
            logger.trace("deserializing a " + clazz.getCanonicalName());
        }
        T object = null;
        if (context.currentOrNext() != NULL) {
            Model<T> model = Model.of(clazz);
            Optional<Class<?>> annotatedParent = getParentClassAnnotatedWith(clazz, TypeInfo.class);
            if (annotatedParent.isPresent()) {
                clazz = (Class<T>) annotatedParent.get(); // doesn't have to be the class that is actually passed, could be a parent type
            }
            if (context.current() == OBJECT) {
                context.push(OBJECT);
                if (annotatedParent.isPresent()) {
                    context.next();
                    String type = clazz.getAnnotation(TypeInfo.class).property();
                    context.push(PROPERTY);
                    String firstProperty = read(context);
                    if (firstProperty.equals(type)) {
                        context.next();
                        model = Model.of(getSubTypeOf(clazz, type, read(context, String.class)));
                        context.pop(PROPERTY);
                    } else {
                        throw new DeserializationException("while using polymorphic deserialization on " + clazz + ", " +
                            "'" + type + "' must be the first property, was '" + firstProperty + "'");
                    }
                }
                object = model.newInstance();
                Map<String, Setter<T>> setters = Properties.settersOf(model.getType());
                while (context.next() != OBJECT_END) {
                    context.push(PROPERTY);
                    String property = read(context);
                    Setter<T> setter = setters.get(property);
                    context.next();
                    readValue:
                    {
                        if (setter == null) {
                            Object value = read(context, context.getDefaultTypeFromToken());
                            if (context.ignoreUnknownProperties()) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("skipping member '{}' as '{}'", property, Strings.toString(value));
                                }
                            } else {
                                throw new DeserializationException("unknown property '" + property + "' on " + model.getType());
                            }
                        } else {
                            Object value = read(context, setter.getGenericType());
                            if (logger.isTraceEnabled()) {
                                logger.trace("deserialized member '{}' as '{}'", setter.getName(), Strings.toString(value));
                            }
                            if (setter.deserializeWhen() != When.NEVER) {
                                switch (setter.deserializeWhen()) {
                                    case NOT_NULL:
                                        if (value == null) {
                                            if (logger.isTraceEnabled()) {
                                                logger.trace("skipping null value");
                                            }
                                            break readValue;
                                        }
                                        break;
                                    case NOT_EMPTY:
                                        if (value != null
                                            && ((isAssignableTo(value.getClass(), Collection.class) && ((Collection<?>) value).isEmpty())
                                            || (isAssignableTo(value.getClass(), Map.class) && ((Map<?, ?>) value).isEmpty()))
                                        ) {
                                            if (logger.isTraceEnabled()) {
                                                logger.trace("skipping empty value");
                                            }
                                            break readValue;
                                        }
                                        break;
                                }
                                setter.setOn(object, value);
                            }
                        }
                    }
                    context.pop(PROPERTY);
                }
                if (model instanceof EntityModel) {
                    object = context.cache().remapOrCache(object);
                }
                context.pop(OBJECT);
            } else if (context.current() == STRING && annotatedParent.isPresent()) {
                // let's deserialize a default instance out of its type
                object = Model.of(Types.getSubTypeOf(clazz, clazz.getAnnotation(TypeInfo.class).property(), read(context))).newInstance();
            } else if (model instanceof EntityModel && context.current() == Token.fromType(((EntityModel<T>) model).key().member().getType())) {
                object = context.cache().remapOrCache(((EntityModel<T>) model).newInstanceFromKey(read(context, ((EntityModel<T>) model).key().member().getType())));
            } else {
                throw new DeserializationException("cannot deserialize a " + clazz + " out of a " + context.current());
            }
        }
        return object;
    }

    protected <E, CollectionType extends Collection<E>> CollectionType readArray(ContextType context, Supplier<Class<E>> valueClassSupplier, Supplier<CollectionType> arraySupplier) {
        CollectionType target = null;
        if (context.currentOrNext() == ARRAY) {
            context.push(ARRAY);
            target = arraySupplier.get();
            while (context.next() != ARRAY_END) {
                target.add(read(context, valueClassSupplier.get()));
            }
            context.pop(ARRAY);
        } else if (context.current() != NULL) {
            throw new DeserializationException("cannot deserialize a " + arraySupplier.get().getClass() + " out of a " + context.current());
        }
        return target;
    }

    protected <K, V, MapType extends Map<K, V>> MapType readMap(ContextType context, Supplier<Class<V>> valueClassSupplier, Supplier<MapType> mapSupplier) {
        MapType map = null;
        if (context.currentOrNext() == OBJECT) {
            context.push(OBJECT);
            map = mapSupplier.get();
            while (context.next() != OBJECT_END) {
                context.push(PROPERTY);
                K key = read(context, context.getDefaultTypeFromToken());
                context.next();
                V value = read(context, valueClassSupplier.get());
                if (key.equals("<<") && value instanceof Map) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("merging values: " + value);
                    }
                    for (Map.Entry<K, V> entry : ((Map<K, V>) value).entrySet()) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("property: " + key + " with value: " + value);
                    }
                    map.put(key, value);
                }
                context.pop(PROPERTY);
            }
            context.pop(OBJECT);
        } else if (context.current() != NULL) {
            throw new DeserializationException("cannot deserialize a " + mapSupplier.get().getClass() + " out of a " + context.current());
        }
        return map;
    }

    public <E> ReaderHandler<E> read(Class<E> clazz) {
        return new ReaderHandler<>(clazz);
    }

    public class ReaderHandler<E> {

        private final Type type;

        public ReaderHandler(Class<E> type) {
            this.type = type;
        }

        public <DeserializerType extends Deserializer<?>> DeserializerType as(Token token, Function<String, E> function) {
            readers.put(type, createReader(token, c -> function.apply(read(c))));
            return (DeserializerType) Deserializer.this;
        }
    }

    public abstract class Context {

        protected final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final CharacterReader reader;
        protected final LinkedList<Token> queued = new LinkedList<>();
        private final LinkedList<Token> structure = new LinkedList<>();
        private final EntityContext cache = new EntityContext();
        private final boolean ignoreUnknownProperties;
        private final boolean unwrapStrings;
        private Token current;

        public Context(CharacterReader reader, Set<Option> options) {
            this.reader = reader;
            this.ignoreUnknownProperties = options.contains(IGNORE_UNKNOWN_PROPERTIES);
            this.unwrapStrings = options.contains(UNWRAP_STRINGS);
        }

        public boolean ignoreUnknownProperties() {
            return ignoreUnknownProperties;
        }

        public boolean unwrapStrings() {
            return unwrapStrings;
        }

        public CharacterReader reader() {
            return reader;
        }

        protected abstract Token evaluateNextToken();

        protected LinkedList<Token> structure() {
            return structure;
        }

        protected Token last() {
            return structure.isEmpty() ? null : structure.getLast();
        }

        public Token current() {
            return current;
        }

        protected Token currentOrNext() {
            return current != null ? current : next();
        }

        protected Token next() {
            if (!queued.isEmpty()) {
                current = queued.removeFirst();
            } else {
                current = evaluateNextToken();
            }
            if (logger.isTraceEnabled()) {
                logger.trace("current token is: {} queued: {}", current, queued);
            }
            if (current == SKIP) {
                next();
            }
            return current;
        }

        protected void push(Token token) {
            structure.add(token);
            if (logger.isTraceEnabled()) {
                logger.trace("structure is now: {}", structure.stream().map(Enum::toString).collect(Collectors.joining("->")));
            }
        }

        protected void pop() {
            pop(current());
        }

        protected void pop(Token token) {
            Token last = structure.removeLast();
            if (!last.equals(token)) {
                if (!(unwrapStrings && token.equals(STRING))) {
                    throw new DeserializationException("was expecting token " + last + " to be popped, but was " + token);
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("popping: {}", last);
            }
        }

        protected Class<?> getDefaultTypeFromToken() {
            switch (currentOrNext()) {
                case NULL:
                case OBJECT:
                    return Map.class;
                case NUMBER:
                    return Number.class;
                case BOOLEAN:
                    return Boolean.class;
                case STRING:
                case PROPERTY:
                    return String.class;
                case ARRAY:
                    return List.class;
                default:
                    throw new IllegalStateException("cannot deduct type from token " + current);
            }
        }

        public EntityContext cache() {
            return cache;
        }

        public void enqueue(Token token) {
            if (logger.isTraceEnabled()) {
                logger.trace("enqueuing token: {} queued: {}", token, queued);
            }
            queued.add(token);
        }

        public void doNext(Token token) {
            if (logger.isTraceEnabled()) {
                logger.trace("placing token to be next: {} queued: {}", token, queued);
            }
            queued.addFirst(token);
        }

        public <E> E applyTo(Function<ContextType, E> supplier) {
            E e = supplier.apply((ContextType) this);
            if (e != null) {
                Token nextToken = evaluateNextToken();
                if (!nextToken.equals(END)) {
                    throw new DeserializationException(String.format("unexpected trailing content of type %s in %s", nextToken, reader()));
                }
            }
            return e;
        }
    }

    public static final class Option {

        public static Set<Option> concat(Set<Option> defaultOptions, Option... options) {
            return Stream.concat(defaultOptions.stream(), Stream.of(options)).collect(Collectors.toSet());
        }
    }

    public static final Option IGNORE_UNKNOWN_PROPERTIES = new Option();

    public static final Option UNWRAP_STRINGS = new Option();

}
