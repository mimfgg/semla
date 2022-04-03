package io.semla.persistence;

import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.persistence.annotations.Managed;
import io.semla.persistence.annotations.StrictIndices;
import io.semla.query.*;
import io.semla.reflect.Proxy;
import io.semla.reflect.Types;
import io.semla.relation.Relation;
import io.semla.util.Lists;
import io.semla.util.Plural;
import io.semla.util.Strings;
import net.jodah.typetools.TypeResolver;
import org.slf4j.LoggerFactory;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Methods.findMethod;
import static io.semla.reflect.Methods.invoke;
import static io.semla.reflect.Types.rawTypeArgumentOf;
import static io.semla.reflect.Types.wrap;
import static io.semla.util.Unchecked.unchecked;

@SuppressWarnings("unchecked")
public abstract class TypedEntityManager<K, T, GetType, CreateType, SetterType, PatchType, SelectType, PredicateTypes, IncludesType>
    extends AbstractEntityManager<K, T> {

    private final Class<GetType> getType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 2);
    private final Class<CreateType> createType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 3);
    private final Class<SetterType> setterInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 4);
    private final Class<PatchType> patchInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 5);
    private final Class<SelectType> selectType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 6);
    private final Class<PredicateTypes> predicatesInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 7);
    private final Class<IncludesType> includesType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 8);
    protected final EntityManager<K, T> entityManager;

    public TypedEntityManager(EntityManager<K, T> entityManager) {
        super(entityManager.datasource, entityManager.entityManagerFactory);
        this.entityManager = entityManager;
    }

    public EntityManager<K, T> unwrap() {
        return entityManager;
    }

    protected CreateType newInstance() {
        Create<T> create = new Create<>(newContext(), model());
        return Proxy.of(createType, new Object[]{new CreateHandler(create)}, (createProxy, method, args) ->
            findMethod(create.getClass(), method.getName(), method.getParameterTypes())
                .map(m -> invoke(create, method.getName(), args))
                .orElseGet(() -> {
                    create.with(method.getName(), args != null && args.length > 0 ? args[0] : new Object[0]);
                    return createProxy;
                })
        );
    }

    protected GetType get() {
        Get<K, T> get = new Get<>(newContext(), model());
        return Proxy.of(getType, new Object[]{new GetHandler(get)}, (getProxy, method, args) -> {
            switch (method.getName()) {
                case "cached":
                    get.cached();
                    return getProxy;
                case "cachedFor":
                    get.cachedFor((Duration) args[0]);
                    return getProxy;
                case "invalidateCache":
                    get.invalidateCache();
                    return getProxy;
                default:
                    return invoke(get, method.getName(), args);
            }
        });
    }

    protected SelectType select() {
        Select<T> select = new Select<>(newContext(), model());
        return Proxy.of(selectType, new Object[]{new SelectHandler(select)}, (selectProxy, method, args) -> {
                switch (method.getName()) {
                    case "cached":
                        select.cached();
                        return selectProxy;
                    case "cachedFor":
                        select.cachedFor((Duration) args[0]);
                        return selectProxy;
                    case "invalidateCache":
                        select.invalidateCache();
                        return selectProxy;
                    case "and":
                        return Proxy.of(predicatesInterface, (predicateProxy, predicate, none) ->
                            Proxy.of(predicate.getReturnType(), (ignore, operatorMethod, values) -> {
                                String operator = switch (operatorMethod.getName()) {
                                    case "hasKey" -> "is";
                                    case "hasNotKey" -> "not";
                                    case "hasKeyIn" -> "in";
                                    case "hasKeyNotIn" -> "notIn";
                                    default -> operatorMethod.getName();
                                };
                                return invoke(select.predicates().where(selectProxy, predicate.getName()), operator, values);
                            })
                        );
                    case "orderedBy":
                        Stream.concat(Stream.of((BaseSort<?>) args[0]), Stream.of((BaseSort<?>[]) args[1]))
                            .forEach(sort -> select.orderedBy(sort.fieldName, sort.sort));
                        return selectProxy;
                    case "startAt":
                        select.startAt((int) args[0]);
                        return selectProxy;
                    case "limitTo":
                        select.limitTo((int) args[0]);
                        return selectProxy;
                    default:
                        return invoke(select, method.getName(), args);
                }
            }
        );
    }

    public SetterType set() {
        Patch<T> patch = new Patch<>(newContext(), model());
        PatchType patchProxy = Proxy.of(patchInterface, (proxy, method, args) -> {
            if ("and".equals(method.getName())) {
                return Proxy.of(predicatesInterface, (predicateProxy, predicate, none) ->
                    Proxy.of(predicate.getReturnType(), (proxy1, method1, args1) ->
                        invoke(patch.predicates().where(proxy, predicate.getName()), method1.getName(), args1)
                    )
                );
            }
            return invoke(patch, method.getName(), args);
        });
        return Proxy.of(setterInterface, (setterProxy, method, args) -> {
                if ("where".equals(method.getName())) {
                    return Proxy.of(predicatesInterface, (predicateProxy, predicate, none) ->
                        Proxy.of(predicate.getReturnType(), (proxy1, method1, args1) ->
                            invoke(patch.predicates().where(patchProxy, predicate.getName()), method1.getName(), args1)
                        )
                    );
                }
                patch.set(method.getName(), args != null && args.length > 0 ? args[0] : null);
                return setterProxy;
            }
        );
    }

    protected class GetHandler {

        private final Get<K, T> get;

        public GetHandler(Get<K, T> get) {
            this.get = get;
        }

        @SafeVarargs
        public final Optional<T> get(K key, Consumer<IncludesType>... includes) {
            return get.get(key, extractIncludes(Includes::defaultEagersOf, includes)::override);
        }

        @SafeVarargs
        public final Map<K, T> get(Collection<K> keys, Consumer<IncludesType>... includes) {
            return get.get(keys, extractIncludes(Includes::defaultEagersOf, includes)::override);
        }

        public GetHandler.Evict evictCache() {
            return new Evict();
        }

        public class Evict {

            @SafeVarargs
            public final void get(K key, Consumer<IncludesType>... includes) {
                get.evictCache().get(key, extractIncludes(Includes::defaultEagersOf, includes)::override);
            }

            @SafeVarargs
            public final void get(Collection<K> keys, Consumer<IncludesType>... includes) {
                get.evictCache().get(keys, extractIncludes(Includes::defaultEagersOf, includes)::override);
            }
        }
    }

    protected RawHandler handle() {
        return new RawHandler();
    }

    protected class RawHandler {

        @SafeVarargs
        public final T create(T entity, Consumer<IncludesType>... includes) {
            return entityManager.create(entity, extractIncludes(Includes::defaultPersistsOrMergesOf, includes)::override);
        }

        @SafeVarargs
        public final <CollectionType extends Collection<T>> CollectionType create(CollectionType entities, Consumer<IncludesType>... includes) {
            return entityManager.create(entities, extractIncludes(Includes::defaultPersistsOrMergesOf, includes)::override);
        }

        @SafeVarargs
        public final T update(T entity, Consumer<IncludesType>... includes) {
            return entityManager.update(entity, extractIncludes(Includes::defaultPersistsOrMergesOf, includes)::override);
        }

        @SafeVarargs
        public final <CollectionType extends Collection<T>> CollectionType update(CollectionType entities, Consumer<IncludesType>... includes) {
            return entityManager.update(entities, extractIncludes(Includes::defaultPersistsOrMergesOf, includes)::override);
        }

        @SafeVarargs
        public final boolean delete(K key, Consumer<IncludesType>... includes) {
            return entityManager.delete(key, extractIncludes(Includes::defaultRemovesOrDeleteOf, includes)::override);
        }

        @SafeVarargs
        public final long delete(Collection<K> keys, Consumer<IncludesType>... includes) {
            return entityManager.delete(keys, extractIncludes(Includes::defaultRemovesOrDeleteOf, includes)::override);
        }
    }

    protected class CreateHandler {

        private final Create<T> create;

        protected CreateHandler(Create<T> create) {
            this.create = create;
        }

        @SafeVarargs
        public final T create(Consumer<IncludesType>... includes) {
            return create.create(extractIncludes(Includes::defaultPersistsOrMergesOf, includes)::addTo);
        }
    }

    protected class SelectHandler {

        private final Select<T> select;

        protected SelectHandler(Select<T> select) {
            this.select = select;
        }

        @SafeVarargs
        public final Optional<T> first(Consumer<IncludesType>... includes) {
            return select.first(extractIncludes(Includes::defaultEagersOf, includes)::override);
        }

        @SafeVarargs
        public final List<T> list(Consumer<IncludesType>... includes) {
            return select.list(extractIncludes(Includes::defaultEagersOf, includes)::override);
        }

        @SafeVarargs
        public final long delete(Consumer<IncludesType>... includes) {
            return select.delete(extractIncludes(Includes::defaultRemovesOrDeleteOf, includes)::override);
        }

        public Evict evictCache() {
            return new Evict();
        }

        public class Evict {

            @SafeVarargs
            public final void first(Consumer<IncludesType>... includes) {
                select.evictCache().first(extractIncludes(Includes::defaultEagersOf, includes)::override);
            }

            @SafeVarargs
            public final void list(Consumer<IncludesType>... includes) {
                select.evictCache().list(extractIncludes(Includes::defaultEagersOf, includes)::override);
            }

            public void count() {
                select.evictCache().count();
            }
        }
    }

    private Includes<T> extractIncludes(Function<EntityModel<T>, Includes<T>> defaultIncludes, Consumer<IncludesType>[] consumers) {
        if (consumers.length > 0) {
            Includes<T> includes = Includes.of(entityManager.model());
            extractConsumers(includes, consumers, includesType);
            return includes;
        }
        return defaultIncludes.apply(entityManager.model());
    }

    private static <R> void extractConsumers(Includes<?> includes, Consumer<R>[] nextConsumers, Class<?> includesClass) {
        Stream.of(nextConsumers).forEach(consumer ->
            unchecked(() -> consumer.accept((R) includesClass.getConstructor(IncludesHandler.class).newInstance(new IncludesHandler(includes))))
        );
    }

    public interface BooleanHandler<CallBackType> {

        CallBackType is(Boolean value);

        CallBackType not(Boolean value);

    }

    public interface ObjectHandler<ObjectType, CallBackType> {

        CallBackType is(ObjectType value);

        CallBackType not(ObjectType value);

        CallBackType in(ObjectType value, ObjectType... values);

        CallBackType in(Collection<ObjectType> values);

        CallBackType notIn(ObjectType value, ObjectType... values);

        CallBackType notIn(Collection<ObjectType> numbers);
    }

    public interface EntityHandler<KeyType, ObjectType, CallBackType> extends ObjectHandler<ObjectType, CallBackType> {

        CallBackType hasKey(KeyType value);

        CallBackType hasNotKey(KeyType value);

        CallBackType hasKeyIn(KeyType value, KeyType... values);

        CallBackType hasKeyIn(Collection<KeyType> values);

        CallBackType hasKeyNotIn(KeyType value, KeyType... values);

        CallBackType hasKeyNotIn(Collection<KeyType> numbers);
    }

    public interface NumberHandler<NumberType, CallBackType> extends ObjectHandler<NumberType, CallBackType> {

        CallBackType greaterOrEquals(NumberType value);

        CallBackType greaterThan(NumberType value);

        CallBackType lessOrEquals(NumberType value);

        CallBackType lessThan(NumberType value);
    }

    public interface StringHandler<CallBackType> extends ObjectHandler<String, CallBackType> {

        CallBackType like(String pattern);

        CallBackType notLike(String pattern);

        CallBackType contains(String pattern);

        CallBackType doesNotContain(String pattern);

        CallBackType containedIn(String pattern);

        CallBackType notContainedIn(String pattern);
    }

    protected static abstract class BaseSort<SelfType> {

        private final String fieldName;
        private Pagination.Sort sort;

        protected BaseSort(String fieldName) {
            this.fieldName = fieldName;
        }

        public SelfType asc() {
            sort = Pagination.Sort.ASC;
            return (SelfType) this;
        }

        public SelfType desc() {
            sort = Pagination.Sort.DESC;
            return (SelfType) this;
        }
    }

    protected static class IncludesHandler {

        private final Includes<?> includes;

        public IncludesHandler(Includes<?> includes) {
            this.includes = includes;
        }

        public void none() {
            includes.none();
        }

        public void include(String name, Consumer<?>... nextConsumers) {
            includes.include(name);
            if (nextConsumers != null && nextConsumers.length > 0) {
                Stream.of(nextConsumers).forEach(nextConsumer -> {
                    Class<?> nextIncludes = TypeResolver.resolveRawArgument(Consumer.class, nextConsumer.getClass());
                    extractConsumers(includes.get(name).includes(), new Consumer[]{nextConsumer}, nextIncludes);
                });
            }
        }
    }

    public static List<File> preProcessSources(List<String> classPathElements, String outputPath, List<String> patterns) throws IOException {
        FileSystem fileSystem = FileSystems.getDefault();
        List<PathMatcher> matchers = patterns.stream()
            .map(pattern -> pattern.startsWith("glob:") || pattern.startsWith("regex:") ? pattern : "glob:" + pattern)
            .map(fileSystem::getPathMatcher)
            .toList();
        File[] files = Files.find(new File(System.getProperty("user.dir")).toPath(), 100,
            (path, basicFileAttributes) -> !basicFileAttributes.isDirectory() && matchers.stream().anyMatch(matcher -> matcher.matches(path))
        ).map(Path::toFile).distinct().toArray(File[]::new);
        if (files.length == 0) {
            LoggerFactory.getLogger(TypedEntityManager.class).error("no sources matching " + patterns);
            return Lists.empty();
        } else {
            return preProcessSources(classPathElements, outputPath, files);
        }
    }

    // Used by reflection by the maven plugin
    public static List<File> preProcessSources(List<String> classPathElements, String outputPath, String sourcePath) {
        File sources = new File(sourcePath);
        File[] files = sources.listFiles();
        if (files == null || files.length == 0) {
            LoggerFactory.getLogger(TypedEntityManager.class).error("no sources found in " + sourcePath);
            return Lists.empty();
        } else {
            return preProcessSources(classPathElements, outputPath, files);
        }
    }

    public static List<File> preProcessSources(List<String> classPathElements, String outputPath, File... files) {
        return Types.compileFromFiles(classPathElements, files).stream()
            .filter(EntityModel::isEntity)
            .filter(clazz -> clazz.isAnnotationPresent(Managed.class))
            .map(clazz -> writeSource(clazz, outputPath))
            .collect(Collectors.toList());
    }

    public static <T> File writeSource(Class<T> clazz, String outputPath) {
        LoggerFactory.getLogger(TypedEntityManager.class).info("Generating manager for " + clazz);
        String className = getManagerClass(clazz);
        String packageName = getManagerPackage(clazz);
        String source = generateSourceFor(clazz, packageName, className);
        File file = new File(outputPath +
            packageName.replaceAll("\\.", "/") + "/" +
            className + ".java");
        file.getParentFile().mkdirs();
        unchecked(() -> new FileOutputStream(file).write(source.getBytes()));
        return file;
    }

    public static <T> String getManagerCanonicalName(Class<T> clazz) {
        return getManagerPackage(clazz) + "." + getManagerClass(clazz);
    }

    public static <T> String getManagerPackage(Class<T> clazz) {
        return Strings.defaultIfEmptyOrNull(clazz.getAnnotation(Managed.class).packageName(), clazz.getPackage().getName());
    }

    public static <T> String getManagerClass(Class<T> clazz) {
        return Strings.defaultIfEmptyOrNull(clazz.getAnnotation(Managed.class).className(), clazz.getSimpleName() + "Manager");
    }

    public static <T> String generateSourceFor(Class<T> clazz, String packageName, String className) {
        EntityModel<T> model = EntityModel.of(clazz);
        String entityName = packageName.equals(clazz.getPackage().getName()) ? clazz.getSimpleName() : clazz.getCanonicalName();
        String key = model.key().member().getName();
        String keys = Plural.of(key);
        String keyType = getTypeName(model.key().member().getGenericType());
        String wrappedKeyType = getTypeName(wrap(model.key().member().getType()));

        boolean indexedOnly = clazz.isAnnotationPresent(StrictIndices.class);

        StringBuilder constructorParameters = new StringBuilder();
        StringBuilder constructionChain = new StringBuilder();

        model.members().stream()
            .filter(field -> field.annotation(NotNull.class).isPresent() ||
                (field.annotation(GeneratedValue.class).isEmpty() && field.annotation(Id.class).isPresent()))
            .forEach(field -> {
                constructorParameters.append(getTypeName(field.getGenericType())).append(' ').append(field.getName()).append(", ");
                constructionChain.append('.').append(field.getName()).append('(').append(field.getName()).append(')');
            });

        if (constructorParameters.length() > 0) {
            constructorParameters.delete(constructorParameters.length() - 2, constructorParameters.length());
        }

        StringBuilder source = new StringBuilder("""
            package %1$s;

            import io.semla.persistence.AbstractEntityManager;
            import io.semla.persistence.EntityManager;
            import io.semla.persistence.TypedEntityManager;
            import io.semla.util.concurrent.Async;

            import javax.annotation.Generated;
            import java.time.Duration;
            import java.util.Collection;
            import java.util.List;
            import java.util.Map;
            import java.util.Optional;
            import java.util.concurrent.CompletionStage;
            import java.util.stream.Stream;
            import java.util.function.Consumer;

            @Generated(value = "by semla.io", date = "%2$s")
            public class %3$s extends TypedEntityManager<%4$s, %5$s, %3$s.Get, %3$s.Create,
                %3$s.Setter, %3$s.Patch, %3$s.Select, %3$s.PredicateHandler<?>, %3$s.Includes> {

                public %3$s(EntityManager<%4$s, %5$s> entityManager) {
                    super(entityManager);
                }

                public %3$s.Create new%6$s(%7$s) {
                    return super.newInstance()%8$s;
                }

                public PredicateHandler<Select> where() {
                    return select().and();
                }

                public Select orderedBy(Sort sort, Sort... sorts) {
                    return select().orderedBy(sort, sorts);
                }

                public Select startAt(int start) {
                    return select().startAt(start);
                }

                public Select limitTo(int limit) {
                    return select().limitTo(limit);
                }
                
                public AsyncHandler async() {
                    return new AsyncHandler();
                }

            """.formatted(packageName, Strings.toString(Instant.now()), className, wrappedKeyType, entityName, clazz.getSimpleName(), constructorParameters, constructionChain));

        if (!model.relations().isEmpty()) {
            source.append("""
                    @SafeVarargs
                    public final Optional<%1$s> get(%2$s %3$s, Consumer<Includes>... includes) {
                        return get().get(%3$s, includes);
                    }

                    @SafeVarargs
                    public final Map<%4$s, %1$s> get(Collection<%4$s> %5$s, Consumer<Includes>... includes) {
                        return get().get(%5$s, includes);
                    }

                    @SafeVarargs
                    public final Optional<%1$s> first(Consumer<Includes>... includes) {
                        return select().first(includes);
                    }

                    @SafeVarargs
                    public final List<%1$s> list(Consumer<Includes>... includes) {
                        return select().list(includes);
                    }

                    @SafeVarargs
                    public final %1$s create(%1$s %6$s, Consumer<Includes>... includes) {
                        return handle().create(%6$s, includes);
                    }

                    @SafeVarargs
                    public final <CollectionType extends Collection<%1$s>> CollectionType create(CollectionType %7$s, Consumer<Includes>... includes) {
                        return handle().create(%7$s, includes);
                    }

                    @SafeVarargs
                    public final %1$s update(%1$s %6$s, Consumer<Includes>... includes) {
                        return handle().update(%6$s, includes);
                    }

                    @SafeVarargs
                    public final <CollectionType extends Collection<%1$s>> CollectionType update(CollectionType %7$s, Consumer<Includes>... includes) {
                        return handle().update(%7$s, includes);
                    }

                    @SafeVarargs
                    public final boolean delete(%2$s %3$s, Consumer<Includes>... includes) {
                        return handle().delete(%3$s, includes);
                    }

                    @SafeVarargs
                    public final long delete(Collection<%4$s> %5$s, Consumer<Includes>... includes) {
                        return handle().delete(%5$s, includes);
                    }
                    
                    public class AsyncHandler implements AbstractEntityManager.AsyncHandler<%4$s, %1$s> {
                    
                        @SafeVarargs
                        public final CompletionStage<Optional<%1$s>> get(%2$s id, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.get(id, includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<Map<%4$s, %1$s>> get(Collection<%4$s> ids, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.get(ids, includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<Optional<%1$s>> first(Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.first(includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<List<%1$s>> list(Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.list(includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<%1$s> create(%1$s fruit, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.create(fruit, includes));
                        }
                
                        @SafeVarargs
                        public final <CollectionType extends Collection<%1$s>> CompletionStage<CollectionType> create(CollectionType fruits, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.create(fruits, includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<%1$s> update(%1$s fruit, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.update(fruit, includes));
                        }
                
                        @SafeVarargs
                        public final <CollectionType extends Collection<%1$s>> CompletionStage<CollectionType> update(CollectionType fruits, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.update(fruits, includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<Boolean> delete(%2$s id, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.delete(id, includes));
                        }
                
                        @SafeVarargs
                        public final CompletionStage<Long> delete(Collection<%4$s> ids, Consumer<Includes>... includes) {
                            return Async.supplyBlocking(() -> %8$s.this.delete(ids, includes));
                        }
                """.formatted(entityName, keyType, key, wrappedKeyType, keys, model.singularName(), model.pluralName(), className));
        } else {
            // no need for the other relationless methods, they are in the AbstractEntityManager class
            source.append("""
                    public Optional<%1$s> first() {
                        return select().first();
                    }

                    public List<%1$s> list() {
                        return select().list();
                    }
                    
                    public class AsyncHandler implements AbstractEntityManager.AsyncHandler<%3$s, %1$s> {
                    
                        public final CompletionStage<Optional<%1$s>> first() {
                            return Async.supplyBlocking(() -> %2$s.this.first());
                        }
                
                        public final CompletionStage<List<%1$s>> list() {
                            return Async.supplyBlocking(() -> %2$s.this.list());
                        }
                """.formatted(entityName, className, wrappedKeyType));
        }

        source.append("""
                                       
                    @Override
                    public CompletionStage<Optional<%1$s>> get(%4$s key) {
                        return %8$s.super.async().get(key);
                    }
            
                    @Override
                    public CompletionStage<Map<%4$s, %1$s>> get(%4$s key, %4$s... keys) {
                        return %8$s.super.async().get(key, keys);
                    }
            
                    @Override
                    public CompletionStage<Map<%4$s, %1$s>> get(List<%4$s> keys) {
                        return %8$s.super.async().get(keys);
                    }
            
                    @Override
                    public CompletionStage<Long> count() {
                        return %8$s.super.async().count();
                    }
            
                    @Override
                    public CompletionStage<%1$s> create(%1$s entity) {
                        return %8$s.super.async().create(entity);
                    }
            
                    @Override
                    public CompletionStage<List<%1$s>> create(%1$s first, %1$s... rest) {
                        return %8$s.super.async().create(first, rest);
                    }
            
                    @Override
                    public <CollectionType extends Collection<%1$s>> CompletionStage<CollectionType> create(CollectionType entities) {
                        return %8$s.super.async().create(entities);
                    }
            
                    @Override
                    public CompletionStage<List<%1$s>> create(Stream<%1$s> stream) {
                        return %8$s.super.async().create(stream);
                    }
            
                    @Override
                    public CompletionStage<%1$s> update(%1$s entity) {
                        return %8$s.super.async().update(entity);
                    }
            
                    @Override
                    public CompletionStage<List<%1$s>> update(%1$s first, %1$s... rest) {
                        return %8$s.super.async().update(first, rest);
                    }
            
                    @Override
                    public CompletionStage<List<%1$s>> update(Stream<%1$s> stream) {
                        return %8$s.super.async().update(stream);
                    }
            
                    @Override
                    public <CollectionType extends Collection<%1$s>> CompletionStage<CollectionType> update(CollectionType entities) {
                        return %8$s.super.async().update(entities);
                    }
            
                    @Override
                    public CompletionStage<Boolean> delete(%4$s key) {
                        return %8$s.super.async().delete(key);
                    }
            
                    @Override
                    public CompletionStage<Long> delete(%4$s key, %4$s... keys) {
                        return %8$s.super.async().delete(key, keys);
                    }
            
                    @Override
                    public CompletionStage<Long> delete(Collection<%4$s> keys) {
                        return %8$s.super.async().delete(keys);
                    }
                }
                
                public Get cached() {
                    return get().cached();
                }

                public Get cachedFor(Duration ttl) {
                    return get().cachedFor(ttl);
                }

                public Get invalidateCache() {
                    return get().invalidateCache();
                }

                public Get.Evict evictCache() {
                    return get().evictCache();
                }

                public abstract static class Get {

                    private final GetHandler handler;

                    protected Get(GetHandler handler) {
                        this.handler = handler;
                    }
                    
                    public final AsyncHandler async() {
                        return new AsyncHandler();
                    }

            """.formatted(entityName, keyType, key, wrappedKeyType, keys, model.singularName(), model.pluralName(), className));
        if (!model.relations().isEmpty()) {
            source.append("""
                        @SafeVarargs
                        public final Optional<%1$s> get(%2$s %3$s, Consumer<Includes>... includes) {
                            return handler.get(%3$s, includes);
                        }

                        @SafeVarargs
                        public final Map<%4$s, %1$s> get(Collection<%4$s> %5$s, Consumer<Includes>... includes) {
                            return handler.get(%5$s, includes);
                        }
                        
                        public class AsyncHandler {
                        
                            @SafeVarargs
                            public final CompletionStage<Optional<%1$s>> get(%2$s %3$s, Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> Get.this.get(%3$s, includes));
                            }
                
                            @SafeVarargs
                            public final CompletionStage<Map<%4$s, %1$s>> get(Collection<%4$s> %5$s, Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> Get.this.get(%5$s, includes));
                            }
                        }
                """.formatted(entityName, keyType, key, wrappedKeyType, keys));
        } else {
            source.append("""
                        public final Optional<%1$s> get(%2$s %3$s) {
                            return handler.get(%3$s);
                        }

                        public final Map<%4$s, %1$s> get(Collection<%4$s> %5$s) {
                            return handler.get(%5$s);
                        }
                        
                        public class AsyncHandler {
                        
                            public final CompletionStage<Optional<%1$s>> get(%2$s %3$s) {
                                return Async.supplyBlocking(() -> Get.this.get(%3$s));
                            }
                
                            public final CompletionStage<Map<%4$s, %1$s>> get(Collection<%4$s> %5$s) {
                                return Async.supplyBlocking(() -> Get.this.get(%5$s));
                            }
                        }
                """.formatted(entityName, keyType, key, wrappedKeyType, keys));
        }
        source.append("""
                    
                    public abstract Get cached();

                    public abstract Get cachedFor(Duration ttl);

                    public abstract Get invalidateCache();

                    public final Evict evictCache() {
                        return new Evict();
                    }

                    public class Evict {
                    
                        public final AsyncHandler async() {
                            return new AsyncHandler();
                        }

            """);
        if (!model.relations().isEmpty()) {
            source.append("""
                            @SafeVarargs
                            public final void get(%1$s %2$s, Consumer<Includes>... includes) {
                                handler.evictCache().get(%2$s, includes);
                            }

                            @SafeVarargs
                            public final void get(Collection<%3$s> %4$s, Consumer<Includes>... includes) {
                                handler.evictCache().get(%4$s, includes);
                            }
                            
                            public class AsyncHandler {
                            
                                @SafeVarargs
                                public final CompletionStage<Void> get(%1$s %2$s, Consumer<Includes>... includes) {
                                    return Async.runBlocking(() -> Evict.this.get(%2$s, includes));
                                }
                
                                @SafeVarargs
                                public final CompletionStage<Void> get(Collection<%3$s> %4$s, Consumer<Includes>... includes) {
                                    return Async.runBlocking(() -> Evict.this.get(%4$s, includes));
                                }
                            }
                """.formatted(keyType, key, wrappedKeyType, keys));
        } else {
            source.append("""
                            public final void get(%1$s %2$s) {
                                handler.evictCache().get(%2$s);
                            }

                            public final void get(Collection<%3$s> %4$s) {
                                handler.evictCache().get(%4$s);
                            }
                            
                            public class AsyncHandler {
                            
                                public final CompletionStage<Void> get(%1$s %2$s) {
                                    return Async.runBlocking(() -> Evict.this.get(%2$s));
                                }
                
                                public final CompletionStage<Void> get(Collection<%3$s> %4$s) {
                                    return Async.runBlocking(() -> Evict.this.get(%4$s));
                                }
                            }
                """.formatted(keyType, key, wrappedKeyType, keys));
        }

        source.append("""
                    }
                }
                
                public interface Properties<SelfType> {
            """);
        model.members().stream()
            .filter(field -> new Column<>(field).insertable())
            .forEach(field -> source.append("""

                        SelfType %1$s(%2$s %1$s);
                """.formatted(field.getName(), getTypeName(field.getGenericType())))
            );
        source.append("""
                }

                public abstract static class Create implements Properties<Create> {

                    private final CreateHandler handler;

                    protected Create(CreateHandler handler) {
                        this.handler = handler;
                    }
                    
                    public final AsyncHandler async() {
                        return new AsyncHandler();
                    }
                    
            """);
        if (!model.relations().isEmpty()) {
            source.append("""
                        @SafeVarargs
                        public final %1$s create(Consumer<Includes>... includes) {
                            return handler.create(includes);
                        }
                        
                        public class AsyncHandler {
                        
                            @SafeVarargs
                            public final CompletionStage<%1$s> create(Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> Create.this.create(includes));
                            }
                        }
                """.formatted(entityName));
        } else {
            source.append("""
                        public %1$s create() {
                            return handler.create();
                        }
                        
                        public class AsyncHandler {
                        
                            public final CompletionStage<%1$s> create() {
                                return Async.supplyBlocking(() -> Create.this.create());
                            }
                        }
                """.formatted(entityName));
        }

        source.append("""
                }
                
                public interface Setter extends Properties<Setter> {

                    PredicateHandler<Patch> where();
                }

                public interface Patch {

                    PredicateHandler<Patch> and();

                    long patch();
                    
                    io.semla.query.Patch.AsyncHandler<%s> async();
                }

                public abstract static class Select {

                    private final SelectHandler handler;

                    protected Select(SelectHandler handler) {
                        this.handler = handler;
                    }
                    
                    public final AsyncHandler async() {
                        return new AsyncHandler();
                    }

            """.formatted(entityName));
        if (!model.relations().isEmpty()) {
            source.append("""
                        @SafeVarargs
                        public final Optional<%1$s> first(Consumer<Includes>... includes) {
                            return handler.first(includes);
                        }

                        @SafeVarargs
                        public final List<%1$s> list(Consumer<Includes>... includes) {
                            return handler.list(includes);
                        }

                        @SafeVarargs
                        public final long delete(Consumer<Includes>... includes) {
                            return handler.delete(includes);
                        }
                        
                        public class AsyncHandler {
                        
                            @SafeVarargs
                            public final CompletionStage<Optional<%1$s>> first(Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> handler.first(includes));
                            }
                                
                            @SafeVarargs
                            public final CompletionStage<List<%1$s>> list(Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> handler.list(includes));
                            }
                                
                            @SafeVarargs
                            public final CompletionStage<Long> delete(Consumer<Includes>... includes) {
                                return Async.supplyBlocking(() -> handler.delete(includes));
                            }
                """.formatted(entityName));
        } else {
            source.append("""
                        public final Optional<%1$s> first() {
                            return handler.first();
                        }

                        public final List<%1$s> list() {
                            return handler.list();
                        }

                        public final long delete() {
                            return handler.delete();
                        }
                        
                        public class AsyncHandler {
                        
                            public final CompletionStage<Optional<%1$s>> first() {
                                return Async.supplyBlocking(() -> handler.first());
                            }
                                
                            public final CompletionStage<List<%1$s>> list() {
                                return Async.supplyBlocking(() -> handler.list());
                            }
                                
                            public final CompletionStage<Long> delete() {
                                return Async.supplyBlocking(() -> handler.delete());
                            }
                """.formatted(entityName));
        }

        source.append("""
                        
                        public final CompletionStage<Long> count() {
                            return Async.supplyBlocking(() -> Select.this.count());
                        }
                    }
                    
                    public abstract long count();

                    public abstract PredicateHandler<Select> and();

                    public abstract Select orderedBy(Sort sort, Sort... sorts);

                    public abstract Select startAt(int start);

                    public abstract Select limitTo(int limit);

                    public abstract Select cached();

                    public abstract Select cachedFor(Duration ttl);

                    public abstract Select invalidateCache();

                    public final Evict evictCache() {
                        return new Evict();
                    }

                    public class Evict {

            """);
        if (!model.relations().isEmpty()) {
            source.append("""
                            @SafeVarargs
                            public final void first(Consumer<Includes>... includes) {
                                handler.evictCache().first(includes);
                            }

                            @SafeVarargs
                            public final void list(Consumer<Includes>... includes) {
                                handler.evictCache().list(includes);
                            }
                """);
        } else {
            source.append("""
                            public final void first() {
                                handler.evictCache().first();
                            }

                            public final void list() {
                                handler.evictCache().list();
                            }
                """);
        }
        source.append("""
                        
                        public void count() {
                            handler.evictCache().count();
                        }
                    }
                }
                
                public interface PredicateHandler<CallBack> {
            """);

        model.members().stream()
            .filter(member -> !indexedOnly || model.isIndexed(member))
            .forEach(member -> {
                source.append("\n        ");
                if (Types.isAssignableTo(member.getType(), Number.class)) {
                    source.append("NumberHandler<").append(getTypeName(wrap(member.getType()))).append(", CallBack>");
                } else if (Types.isAssignableTo(member.getType(), Boolean.class)) {
                    source.append("BooleanHandler<CallBack>");
                } else if (member.getType().equals(String.class)) {
                    source.append("StringHandler<CallBack>");
                } else if (EntityModel.isEntity(member.getType())) {
                    source.append("EntityHandler<").append(getTypeName(wrap(EntityModel.of(member.getType()).key().member().getType())))
                        .append(", ").append(getTypeName(wrap(member.getType()))).append(", CallBack>");
                } else {
                    source.append("ObjectHandler<").append(getTypeName(wrap(member.getType()))).append(", CallBack>");
                }
                source.append(" ").append(member.getName()).append("();\n");
            });

        source.append("""
                }
                
                public static class Includes {

                    private final IncludesHandler handler;

                    public Includes(IncludesHandler handler) {
                        this.handler = handler;
                    }

            """);

        model.relations().stream()
            .filter(TypedEntityManager::isManaged)
            .forEach(relation -> {
                if (!relation.childModel().relations().isEmpty()) {
                    source.append("""
                                @SafeVarargs
                                public final void %1$s(Consumer<%2$s.Includes>... includes) {
                                    handler.include("%1$s", includes);
                                }

                        """.formatted(relation.member().getName(), getTypeName(relation)));
                } else {
                    source.append("""
                                public final void %1$s() {
                                    handler.include("%1$s");
                                }

                        """.formatted(relation.member().getName()));
                }
            });

        source.append("""
                    public void none() {
                        handler.none();
                    }
                }

                public static class Sort extends BaseSort<Sort> {

                    private Sort(String fieldName) {
                        super(fieldName);
                    }
            """);
        model.members().forEach(field ->
            source.append("""

                        public static Sort %1$s() {
                            return new Sort("%1$s");
                        }
                """.formatted(field.getName()))
        );

        source.append("""
                }
            }
            """);

        return source.toString();
    }

    private static String getTypeName(Type type) {
        Class<?> rawType = Types.rawTypeOf(type);
        if (rawType.getPackage() != null && rawType.getPackage().getName().equals("java.lang")) {
            return rawType.getSimpleName();
        }
        return type.getTypeName().replaceAll("\\$", ".").replaceAll("java\\.lang\\.", "");
    }

    private static <T> boolean isManaged(Relation<T, ?> relation) {
        if (Types.isAssignableTo(relation.member().getType(), Collection.class)) {
            return rawTypeArgumentOf(relation.member().getGenericType()).isAnnotationPresent(Managed.class);
        }
        return relation.member().getType().isAnnotationPresent(Managed.class);
    }

    private static <T> String getTypeName(Relation<T, ?> relation) {
        if (Types.isAssignableTo(relation.member().getType(), Collection.class)) {
            return getManagerCanonicalName(rawTypeArgumentOf(relation.member().getGenericType()));
        }
        return getManagerCanonicalName(relation.member().getType());
    }
}
