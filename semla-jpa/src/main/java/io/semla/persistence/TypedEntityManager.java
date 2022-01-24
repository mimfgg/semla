package io.semla.persistence;

import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.persistence.annotations.Managed;
import io.semla.persistence.annotations.StrictIndices;
import io.semla.query.*;
import io.semla.reflect.Proxy;
import io.semla.reflect.Types;
import io.semla.relation.Relation;
import io.semla.util.Arrays;
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
public abstract class TypedEntityManager<K, T, GetType, CreateType, SetterType, PatchType, SelectType, PredicateTypes, IncludesType> extends AbstractEntityManager<T> {

    private final Class<GetType> getType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 2);
    private final Class<CreateType> createType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 3);
    private final Class<SetterType> setterInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 4);
    private final Class<PatchType> patchInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 5);
    private final Class<SelectType> selectType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 6);
    private final Class<PredicateTypes> predicatesInterface = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 7);
    private final Class<IncludesType> includesType = rawTypeArgumentOf(this.getClass().getGenericSuperclass(), 8);
    protected final EntityManager<T> entityManager;

    public TypedEntityManager(EntityManager<T> entityManager) {
        super(entityManager.datasource, entityManager.entityManagerFactory);
        this.entityManager = entityManager;
    }

    public EntityManager<T> unwrap() {
        return entityManager;
    }

    protected CreateType newInstance() {
        Create<T> create = new Create<>(newContext(), model());
        return Proxy.of(createType, new Object[]{new CreateHandler(create)}, (createProxy, method, args) ->
            findMethod(create.getClass(), method.getName(), method.getParameterTypes())
                .map(m -> invoke(create, method.getName(), Arrays.emptyIfNull(args)))
                .orElseGet(() -> {
                    create.with(method.getName(), args != null && args.length > 0 ? args[0] : new Object[0]);
                    return createProxy;
                })
        );
    }

    protected GetType get() {
        Get<T> get = new Get<>(newContext(), model());
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
                    return invoke(get, method.getName(), Arrays.emptyIfNull(args));
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
                                return invoke(select.predicates().where(selectProxy, predicate.getName()), operator, Arrays.emptyIfNull(values));
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
                        return invoke(select, method.getName(), Arrays.emptyIfNull(args));
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
            return invoke(patch, method.getName(), Arrays.emptyIfNull(args));
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

        private final Get<T> get;

        public GetHandler(Get<T> get) {
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

        StringBuilder source = new StringBuilder("package " + packageName + ";\n\n" +
            "import io.semla.persistence.EntityManager;\n" +
            "import io.semla.persistence.TypedEntityManager;\n" +
            "import io.semla.relation.IncludeType;\n" +
            "import io.semla.relation.IncludeTypes;\n\n" +
            "import java.time.Duration;\n" +
            "import java.util.List;\n" +
            "import java.util.Map;\n" +
            "import java.util.Collection;\n" +
            "import java.util.Optional;\n" +
            "import java.util.function.Consumer;\n" +
            "import javax.annotation.Generated;\n\n" +
            "@Generated(value = \"by semla.io\", date = \"" + Strings.toString(Instant.now()) + "\")\n" +
            "public class " + className + " extends TypedEntityManager<" + wrappedKeyType + ", " + entityName + ", " + className + ".Get, " + className + ".Create, " +
            "\n    " + className + ".Setter," + className + ".Patch, " + className + ".Select, " + className + ".PredicateHandler<?>, " + className + ".Includes> {\n\n" +
            "    public " + className + "(EntityManager<" + entityName + "> entityManager) {\n" +
            "        super(entityManager);\n    }\n\n" +
            "    public " + className + ".Create new" + clazz.getSimpleName() + "(" + constructorParameters + ") {\n        return super.newInstance()" + constructionChain + ";\n    }\n\n" +
            "    public PredicateHandler<Select> where() {\n        return select().and();\n    }\n\n" +
            "    public Select orderedBy(Sort sort, Sort... sorts) {\n        return select().orderedBy(sort, sorts);\n    }\n\n" +
            "    public Select startAt(int start) {\n        return select().startAt(start);\n    }\n\n" +
            "    public Select limitTo(int limit) {\n        return select().limitTo(limit);\n    }\n\n");

        if (!model.relations().isEmpty()) {
            source
                .append("    @SafeVarargs\n")
                .append("    public final Optional<").append(entityName).append("> get(").append(keyType).append(" ").append(key).append(", Consumer<Includes>... includes) {\n")
                .append("        return get().get(").append(key).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final Map<").append(wrappedKeyType).append(", ").append(entityName).append("> get(Collection<")
                .append(wrappedKeyType).append("> ").append(keys).append(", Consumer<Includes>... includes) {\n")
                .append("        return get().get(").append(keys).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final Optional<").append(entityName).append("> first(Consumer<Includes>... includes) {\n")
                .append("        return select().first(includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final List<").append(entityName).append("> list(Consumer<Includes>... includes) {\n")
                .append("        return select().list(includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final ").append(entityName).append(" create(").append(entityName).append(" ").append(model.singularName()).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().create(").append(model.singularName()).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final <CollectionType extends Collection<").append(entityName).append(">> CollectionType create(CollectionType ").append(model.pluralName()).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().create(").append(model.pluralName()).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final ").append(entityName).append(" update(").append(entityName).append(" ").append(model.singularName()).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().update(").append(model.singularName()).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final <CollectionType extends Collection<").append(entityName).append(">> CollectionType update(CollectionType ").append(model.pluralName()).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().update(").append(model.pluralName()).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final boolean delete(").append(keyType).append(" ").append(key).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().delete(").append(key).append(", includes);\n")
                .append("    }\n\n")
                .append("    @SafeVarargs\n")
                .append("    public final long delete(Collection<").append(wrappedKeyType).append("> ").append(keys).append(", Consumer<Includes>... includes) {\n")
                .append("        return handle().delete(").append(keys).append(", includes);\n")
                .append("    }\n\n");
        } else {
            source
                // no need for the other relationless methods, they are in the AbstractEntityManager class
                .append("    public Optional<").append(entityName).append("> first() {\n")
                .append("        return select().first();\n")
                .append("    }\n\n")
                .append("    public List<").append(entityName).append("> list() {\n")
                .append("        return select().list();\n")
                .append("    }\n\n");
        }

        source
            .append("    public Get cached() {\n")
            .append("        return get().cached();\n")
            .append("    }\n\n")
            .append("    public Get cachedFor(Duration ttl) {\n")
            .append("        return get().cachedFor(ttl);\n")
            .append("    }\n\n")
            .append("    public Get invalidateCache() {\n")
            .append("        return get().invalidateCache();\n")
            .append("    }\n\n")
            .append("    public Get.Evict evictCache() {\n")
            .append("        return get().evictCache();\n")
            .append("    }\n\n");


        source
            .append("    public abstract static class Get {\n\n")
            .append("        private final GetHandler handler;\n\n")
            .append("        protected Get(GetHandler handler) {\n")
            .append("            this.handler = handler;\n")
            .append("        }\n\n");
        if (!model.relations().isEmpty()) {
            source
                .append("        @SafeVarargs\n")
                .append("        public final Optional<").append(entityName).append("> get(").append(keyType).append(" ").append(key).append(", Consumer<Includes>... includes) {\n")
                .append("            return handler.get(").append(key).append(", includes);\n")
                .append("        }\n\n")
                .append("        @SafeVarargs\n")
                .append("        public final Map<").append(wrappedKeyType).append(", ").append(entityName).append("> get(Collection<")
                .append(wrappedKeyType).append("> ").append(keys).append(", Consumer<Includes>... includes) {\n")
                .append("            return handler.get(").append(keys).append(", includes);\n")
                .append("        }\n\n");
        } else {
            source
                .append("        public final Optional<").append(entityName).append("> get(").append(keyType).append(" ").append(key).append(") {\n")
                .append("            return handler.get(").append(key).append(");\n")
                .append("        }\n\n")
                .append("        public final Map<").append(wrappedKeyType).append(", ").append(entityName).append("> get(Collection<")
                .append(wrappedKeyType).append("> ").append(keys).append(") {\n")
                .append("            return handler.get(").append(keys).append(");\n")
                .append("        }\n\n");
        }
        source
            .append("        public abstract Get cached();\n\n")
            .append("        public abstract Get cachedFor(Duration ttl);\n\n")
            .append("        public abstract Get invalidateCache();\n\n")
            .append("        public final Evict evictCache() {\n")
            .append("            return new Evict();\n")
            .append("        }\n\n")
            .append("        public class Evict {\n\n");
        if (!model.relations().isEmpty()) {
            source
                .append("            @SafeVarargs\n")
                .append("            public final void get(").append(keyType).append(" ").append(key).append(", Consumer<Includes>... includes) {\n")
                .append("                handler.evictCache().get(").append(key).append(", includes);\n")
                .append("            }\n\n")
                .append("            @SafeVarargs\n")
                .append("            public final void get(Collection<").append(wrappedKeyType).append("> ").append(keys).append(", Consumer<Includes>... includes) {\n")
                .append("                handler.evictCache().get(").append(keys).append(", includes);\n")
                .append("            }\n\n");
        } else {
            source
                .append("            public final void get(").append(keyType).append(" ").append(key).append(") {\n")
                .append("                handler.evictCache().get(").append(key).append(");\n")
                .append("            }\n\n")
                .append("            public final void get(Collection<").append(wrappedKeyType).append("> ").append(keys).append(") {\n")
                .append("                handler.evictCache().get(").append(keys).append(");\n")
                .append("            }\n\n");
        }

        source
            .append("        }\n")
            .append("    }\n\n");

        source.append("    public interface Properties<SelfType> {\n");
        model.members().stream().filter(field -> new Column<>(field).insertable())
            .forEach(field -> source.append("\n        SelfType ").append(field.getName())
                .append("(").append(getTypeName(field.getGenericType())).append(" ").append(field.getName()).append(");\n")
            );
        source.append("    }\n\n");

        source
            .append("    public abstract static class Create implements Properties<Create> {\n\n")
            .append("        private final CreateHandler handler;\n\n")
            .append("        protected Create(CreateHandler handler){\n")
            .append("            this.handler = handler;\n")
            .append("        }\n\n");
        if (!model.relations().isEmpty()) {
            source
                .append("        @SafeVarargs\n")
                .append("        public final ").append(entityName).append(" create(Consumer<Includes>... includes) {\n")
                .append("            return handler.create(includes);\n")
                .append("        }\n");
        } else {
            source
                .append("        public ").append(entityName).append(" create() {\n")
                .append("            return handler.create();\n")
                .append("        }\n");
        }
        source.append("    }\n\n");

        source.append("    public interface Setter extends Properties<Setter> {\n\n");
        source.append("        PredicateHandler<Patch> where();\n");
        source.append("    }\n\n");

        source.append("    public interface Patch {\n\n        PredicateHandler<Patch> and();\n\n        long patch();\n    }\n\n");

        source.append("    public abstract static class Select {\n\n" +
            "        private final SelectHandler handler;\n\n" +
            "        protected Select(SelectHandler handler) {\n" +
            "            this.handler = handler;\n" + "        }\n\n");
        if (!model.relations().isEmpty()) {
            source.append("        @SafeVarargs\n" + "        public final Optional<").append(entityName).append("> first(Consumer<Includes>... includes) {\n")
                .append("            return handler.first(includes);\n")
                .append("        }\n\n")
                .append("        @SafeVarargs\n").append("        public final List<").append(entityName).append("> list(Consumer<Includes>... includes) {\n")
                .append("            return handler.list(includes);\n")
                .append("        }\n\n")
                .append("        @SafeVarargs\n").append("        public final long delete(Consumer<Includes>... includes) {\n")
                .append("            return handler.delete(includes);\n")
                .append("        }\n\n");
        } else {
            source.append("        public final Optional<").append(entityName).append("> first() {\n")
                .append("            return handler.first();\n")
                .append("        }\n\n")
                .append("        public final List<").append(entityName).append("> list() {\n")
                .append("            return handler.list();\n")
                .append("        }\n\n")
                .append("        public final long delete() {\n")
                .append("            return handler.delete();\n")
                .append("        }\n\n");
        }

        source
            .append("        public abstract long count();\n\n")
            .append("        public abstract PredicateHandler<Select> and();\n\n")
            .append("        public abstract Select orderedBy(Sort sort, Sort... sorts);\n\n")
            .append("        public abstract Select startAt(int start);\n\n")
            .append("        public abstract Select limitTo(int limit);\n\n")
            .append("        public abstract Select cached();\n\n")
            .append("        public abstract Select cachedFor(Duration ttl);\n\n")
            .append("        public abstract Select invalidateCache();\n\n")
            .append("        public final Evict evictCache() {\n")
            .append("             return new Evict();\n")
            .append("        }\n\n")
            .append("        public class Evict {\n\n");
        if (!model.relations().isEmpty()) {
            source
                .append("            @SafeVarargs\n")
                .append("            public final void first(Consumer<Includes>... includes) {\n")
                .append("                handler.evictCache().first(includes);\n")
                .append("            }\n\n")
                .append("            @SafeVarargs\n")
                .append("            public final void list(Consumer<Includes>... includes) {\n")
                .append("                handler.evictCache().list(includes);\n")
                .append("            }\n\n");
        } else {
            source
                .append("            public final void first() {\n")
                .append("                handler.evictCache().first();\n")
                .append("            }\n\n")
                .append("            public final void list() {\n")
                .append("                handler.evictCache().list();\n")
                .append("            }\n\n");
        }
        source.append("            public void count() {\n")
            .append("                handler.evictCache().count();\n")
            .append("            }\n")
            .append("        }\n")
            .append("    }\n\n");

        source.append("    public interface PredicateHandler<CallBack> {\n");
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
        source.append("    }\n\n");

        source.append("""
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
                    source.append("        @SafeVarargs\n" +
                            "        public final void ")
                        .append(relation.member().getName()).append("(Consumer<").append(getTypeName(relation)).append(".Includes>... includes) {\n")
                        .append("            handler.include(\"").append(relation.member().getName()).append("\", includes);\n" +
                            "        }\n\n");
                } else {
                    source.append("        public final void ").append(relation.member().getName()).append("() {\n")
                        .append("            handler.include(\"").append(relation.member().getName()).append("\");\n" +
                            "        }\n\n");
                }
            });

        source.append("""
                    public void none() {
                        handler.none();
                    }
                }

            """);

        source.append("""
                public static class Sort extends BaseSort<Sort> {

                    private Sort(String fieldName) {
                        super(fieldName);
                    }
            """);
        model.members().forEach(field ->
            source.append("\n        public static Sort ").append(field.getName())
                .append("() {\n            return new Sort(\"").append(field.getName()).append("\");\n        }\n")
        );

        source.append("    }\n");
        source.append("}\n");

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
