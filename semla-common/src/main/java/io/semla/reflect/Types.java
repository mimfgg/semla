package io.semla.reflect;

import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.serialization.json.Json;
import io.semla.util.ImmutableMap;
import io.semla.util.Strings;
import io.semla.util.WithBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.burningwave.core.assembler.StaticComponentContainer;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.semla.util.Arrays.toArray;
import static io.semla.util.Unchecked.unchecked;
import static org.burningwave.core.assembler.StaticComponentContainer.Modules;

@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class Types {

    static {
        Modules.exportPackageToAllUnnamed("java.base", "jdk.internal.loader");
    }

    private static final Map<Class<?>, Class<?>> WRAPPER_BY_PRIMITIVE = ImmutableMap.<Class<?>, Class<?>>builder()
        .put(byte.class, Byte.class)
        .put(short.class, Short.class)
        .put(int.class, Integer.class)
        .put(long.class, Long.class)
        .put(float.class, Float.class)
        .put(double.class, Double.class)
        .put(boolean.class, Boolean.class)
        .put(char.class, Character.class)
        .build();
    private static final Map<Predicate<Class<?>>, BiFunction<Class<?>, Object, Object>> CUSTOM_UNWRAPPERS = new LinkedHashMap<>();
    private static final Map<Class<?>, Map<Class<? extends Annotation>, Optional<Class<?>>>> ANNOTATED_SUPER_CLASSES = new LinkedHashMap<>();
    private static final Map<Class<?>, Map<String, Map<String, Class<?>>>> SUB_TYPES = new LinkedHashMap<>();

    public static <E> E safeNull(Type type, E value) {
        return safeNull(rawTypeOf(type), value);
    }

    public static <E> E safeNull(Class<?> clazz, E value) {
        if (clazz.isPrimitive() && value == null) {
            value = (E) Array.get(Array.newInstance(clazz, 1), 0);
        }
        return value;
    }

    public static <E> Optional<Class<E>> optionalRawTypeArgumentOf(Type type) {
        return optionalTypeArgumentOf(type, 0).map(Types::rawTypeOf);
    }

    public static Optional<Type> optionalTypeArgumentOf(Type type) {
        return optionalTypeArgumentOf(type, 0);
    }

    public static <E> Optional<Class<E>> optionalRawTypeArgumentOf(Type type, int argumentIndex) {
        return optionalTypeArgumentOf(type, argumentIndex).map(Types::rawTypeOf);
    }

    public static Optional<Type> optionalTypeArgumentOf(Type type, int argumentIndex) {
        return Optional.ofNullable(typeArgumentOf(type, argumentIndex));
    }

    public static <E> Class<E> rawTypeArgumentOf(Type type) {
        return rawTypeOf(typeArgumentOf(type));
    }

    public static Type typeArgumentOf(Type type) {
        return typeArgumentOf(type, 0);
    }

    public static <E> Class<E> rawTypeArgumentOf(Type type, int argumentIndex) {
        return rawTypeOf(typeArgumentOf(type, argumentIndex));
    }

    public static Type typeArgumentOf(Type type, int argumentIndex) {
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getActualTypeArguments().length > argumentIndex) {
                return parameterizedType.getActualTypeArguments()[argumentIndex];
            }
            throw new IllegalArgumentException(parameterizedType.getTypeName() + " doesn't have a TypeArgument " + argumentIndex);
        } else {
            return null;
        }
    }

    public static <E> Supplier<E> supplierOf(Type type) {
        Class<?> clazz = Types.rawTypeOf(type);
        if (clazz.equals(List.class)) {
            return () -> (E) new ArrayList<>();
        } else if (clazz.equals(Set.class)) {
            return () -> (E) new LinkedHashSet<>();
        } else if (clazz.equals(Map.class)) {
            return () -> (E) new LinkedHashMap<>();
        } else if (!Modifier.is(clazz, Modifier.ABSTRACT)) {
            return () -> unchecked(() -> (E) clazz.getDeclaredConstructor().newInstance());
        } else {
            throw new RuntimeException("Cannot create a supplier for " + clazz);
        }
    }

    public static <E> Class<E> rawTypeOf(Type type) {
        return type instanceof ParameterizedType parameterizedType ? (Class<E>) parameterizedType.getRawType() : (Class<E>) type;
    }

    public static List<Class<?>> compileFromFiles(List<String> classPathElements, File... files) {
        return compileFromFiles(classPathElements, createTempDirectory(), files);
    }

    public static List<Class<?>> compileFromFiles(List<String> classPathElements, String tmpDir, File... files) {
        if (files.length == 0) {
            throw new IllegalArgumentException("no files were provided for compilation!");
        }
        List<String> arguments = new ArrayList<>();
        if (!classPathElements.isEmpty()) {
            arguments.add("-cp");
            arguments.add(String.join(":", classPathElements));
        }
        arguments.add("-d");
        arguments.add(tmpDir);
        Stream.of(files).map(File::getPath).forEach(arguments::add);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler().run(null, out, out, arguments.toArray(new String[0]));
        if (result > 0) {
            throw new RuntimeException("compilation failed for " + files.length + " file" + (result > 1 ? "s" : "")
                + "\noutput:" + out
                + "\narguments: " + arguments);
        }
        addToClassLoader(unchecked(() -> new File(tmpDir).toURI().toURL()));
        return unchecked(() -> Files.walk(new File(tmpDir).toPath()))
            .filter(file -> file.getFileName().toString().endsWith(".class"))
            .map(file -> file.toAbsolutePath().toString().replace(tmpDir, "").replace(File.separator, ".").replace(".class", ""))
            .map(classname -> unchecked(() -> Class.forName(classname)))
            .collect(Collectors.toList());

    }

    public static void addToClassLoader(URL url) {
        Object ucp = Fields.getValue(Types.class.getClassLoader(), "ucp");
        Methods.invoke(ucp, "addURL", url);
    }

    public static List<Class<?>> compileFromSources(String... sources) {
        String tmpDir = createTempDirectory();
        return compileFromFiles(new ArrayList<>(), tmpDir, Stream.of(sources).map(source -> unchecked(() -> {
            String packagePath = getPackageFrom(source);
            String name = getClassNameFrom(source);
            File sourceFile = new File(tmpDir + packagePath + name + ".java");
            sourceFile.deleteOnExit();
            sourceFile.getParentFile().mkdirs();
            Files.write(sourceFile.toPath(), source.getBytes());
            return sourceFile;
        })).toArray(File[]::new));
    }

    private static String getPackageFrom(String source) {
        int packageNameStart = source.indexOf("package ") + 8;
        if (packageNameStart > 7) {
            int packageNameStop = source.indexOf(";", packageNameStart);
            return source.substring(packageNameStart, packageNameStop).replaceAll("\\.", File.separator) + File.separator;
        } else {
            return "";
        }
    }

    private static String getClassNameFrom(String source) {
        int classNameStart = source.indexOf("class ") + 6;
        int classNameStop = source.indexOf(" ", classNameStart);
        return source.substring(classNameStart, classNameStop);
    }

    private static String createTempDirectory() {
        return unchecked(() -> Files.createTempDirectory("").toAbsolutePath() + File.separator);
    }

    public static void assertIsAssignableTo(Object value, Class<?> toClass) {
        if (!Types.isAssignableTo(value.getClass(), toClass)) {
            throw new IllegalArgumentException(toClass + " cannot be assigned value '" + value + "' of type " + value.getClass());
        }
    }

    public static boolean isAssignableToOneOf(Type type, Class<?>... toClasses) {
        return isAssignableToOneOf(rawTypeOf(type), toClasses);
    }

    public static boolean isAssignableToOneOf(Class<?> clazz, Class<?>... toClasses) {
        return Stream.of(toClasses).anyMatch(toClass -> isAssignableTo(clazz, toClass));
    }

    public static boolean isEqualToOneOf(Class<?> clazz, Class<?>... toClasses) {
        return Arrays.asList(toClasses).contains(clazz);
    }

    public static boolean isAssignableTo(Type type, Class<?> toClass) {
        return isAssignableTo(rawTypeOf(type), toClass);
    }

    public static boolean isAssignableTo(Class<?> clazz, Class<?> toClass) {
        return toClass != null && clazz != null && wrap(toClass).isAssignableFrom(wrap(clazz));
    }

    public static Class<?> wrap(Class<?> clazz) {
        return clazz.isPrimitive() ? WRAPPER_BY_PRIMITIVE.get(clazz) : clazz;
    }

    public static <T> boolean hasSuperClass(Class<T> clazz) {
        return clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class);
    }

    public static <E> E newInstance(Class<E> clazz, Object... parameters) {
        Class<?>[] parametersTypes = Stream.of(parameters).map(Object::getClass).toArray(Class<?>[]::new);
        return unchecked(() -> Stream.of(clazz.getConstructors())
            .filter(constructor ->
                constructor.getParameterCount() == parameters.length
                    && IntStream.range(0, parameters.length)
                    .mapToObj(i -> Types.isAssignableTo(parametersTypes[i], constructor.getParameters()[i].getType()))
                    .reduce(true, (current, next) -> current && next))
            .findFirst()
            .map(constructor -> unchecked(() -> (E) constructor.newInstance(parameters)))
            .orElseThrow(() ->
                new NoSuchMethodException(clazz.getCanonicalName() + ".<init>(" +
                    Stream.of(parametersTypes).map(Class::getCanonicalName).collect(Collectors.joining(","))
                    + ")")
            )
        );
    }

    public static Optional<Class<?>> getParentClassAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return ANNOTATED_SUPER_CLASSES
            .computeIfAbsent(clazz, c -> new LinkedHashMap<>())
            .computeIfAbsent(annotationClass, a -> {
                Class<?> current = clazz;
                while (true) {
                    if (current.isAnnotationPresent(annotationClass)) {
                        return Optional.of(current);
                    }
                    for (Class<?> clazzInterface : current.getInterfaces()) {
                        if (clazzInterface.isAnnotationPresent(annotationClass)) {
                            return Optional.of(clazzInterface);
                        }
                    }
                    if (hasSuperClass(current)) {
                        current = current.getSuperclass();
                    } else {
                        break;
                    }
                }
                return Optional.empty();
            });
    }

    public static void registerSubTypes(Class<?>... classes) {
        Arrays.stream(classes).forEach(Types::registerSubType);
    }

    public static void registerSubType(Class<?> clazz) {
        Class<?> superType = Types.getParentClassAnnotatedWith(clazz, TypeInfo.class)
            .orElseThrow(() -> new IllegalArgumentException(clazz + " doesn't have any super class annotated with @TypeInfo"));
        TypeName typeName = clazz.getAnnotation(TypeName.class);
        if (typeName == null) {
            throw new IllegalArgumentException(clazz + " is not annotated with @TypeName");
        }

        SUB_TYPES.computeIfAbsent(superType, c -> new LinkedHashMap<>())
            .computeIfAbsent(superType.getAnnotation(TypeInfo.class).property(), t -> new LinkedHashMap<>())
            .put(typeName.value(), clazz);
    }

    public static <T, S extends T> Class<S> getSubTypeOf(Class<T> clazz, String property, String type) {
        if (!SUB_TYPES.containsKey(clazz)) {
            throw new IllegalStateException("no subtype known for " + clazz);
        }
        if (!SUB_TYPES.get(clazz).containsKey(property)) {
            throw new IllegalStateException("no type property '" + property + "' registered for " + clazz);
        }
        if (!SUB_TYPES.get(clazz).get(property).containsKey(type)) {
            throw new IllegalStateException("no subtype '" + type + "' registered for " + clazz);
        }
        return (Class<S>) SUB_TYPES.get(clazz).get(property).get(type);
    }

    public static ParameterizedTypeBuilder parameterized(Class<?> rawType) {
        return new ParameterizedTypeBuilder(rawType);
    }

    public static boolean isAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.isAnnotationPresent(annotation) || (Types.hasSuperClass(clazz) && isAnnotatedWith(clazz.getSuperclass(), annotation));
    }

    public static ParameterizedType parameterized(Class<?> rawType, Type parameter, Type... parameters) {
        return new ParameterizedTypeImpl(rawType, toArray(parameter, parameters));
    }

    public static WithBuilder<BiFunction<Class<?>, Object, Object>> unwrap(Predicate<Class<?>> predicate) {
        return new WithBuilder<>(unwrapper -> CUSTOM_UNWRAPPERS.put(predicate, unwrapper));
    }

    public static <E> E unwrap(Class<E> type, Object value) {
        if (value != null && !Types.isAssignableTo(value.getClass(), type)) {
            if (value instanceof String string) {
                value = Json.isJson(string) ? Json.read(string, type) : Strings.parse(string, type);
            } else {
                value = CUSTOM_UNWRAPPERS.entrySet().stream()
                    .filter(unwrapper -> unwrapper.getKey().test(type))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElse((c, v) -> v)
                    .apply(type, value);
            }
        }
        return (E) value;
    }

    public static <R> R cast(Object object) {
        return (R) object;
    }

    public static Class<?> getCommonSuperClass(Collection<?> objects) {
        if (objects == null) {
            return null;
        }
        if (objects.isEmpty()) {
            return Object.class;
        }
        Class<?> clazz = null;
        for (Object object : objects) {
            if (clazz == null) {
                clazz = object.getClass();
            } else {
                clazz = getCommonSuperClass(clazz, object.getClass());
            }
        }
        return clazz;
    }

    public static Class<?> getCommonSuperClass(Object object1, Object object2) {
        return getCommonSuperClass(object1.getClass(), object2.getClass());
    }

    public static Class<?> getCommonSuperClass(Class<?> class1, Class<?> class2) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        while (!class1.equals(Object.class)) {
            classes.add(class1);
            class1 = class1.getSuperclass();
        }
        while (!class2.equals(Object.class)) {
            if (classes.contains(class2)) {
                return class2;
            }
            class2 = class2.getSuperclass();
        }
        return Object.class;
    }

    public static <E extends AccessibleObject & Member> E asAccessible(E target) {
        try {
            target.setAccessible(true);
        } catch (Exception first) {
            log.debug("auto exporting {}!", target.getDeclaringClass().getPackageName());
            try {
                Modules.exportPackageToAllUnnamed("java.base", target.getDeclaringClass().getPackageName());
                target.setAccessible(true);
            } catch (Exception second) {
                log.debug("cannot export package: " + target.getDeclaringClass().getPackageName());
            }
        }
        return target;
    }

    public record ParameterizedTypeBuilder(Class<?> rawType) {

        public ParameterizedType of(Type parameter, Type... parameters) {
            return new ParameterizedTypeImpl(rawType, toArray(parameter, parameters));
        }
    }

    private record ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments) implements ParameterizedType {

        private ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            if (rawType.getTypeParameters().length != actualTypeArguments.length) {
                throw new IllegalArgumentException(
                    "type %s expects %d argument%s but got %d".formatted(
                        rawType, rawType.getTypeParameters().length,
                        rawType.getTypeParameters().length > 1 ? "s" : "",
                        actualTypeArguments.length));
            }
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return rawType.getDeclaringClass();
        }

        @Override
        public String toString() {
            return "%s<%s>".formatted(rawType.getTypeName(),
                Stream.of(actualTypeArguments).map(Type::getTypeName).collect(Collectors.joining(", ")));
        }
    }

    private static class ExtendedURLClassLoader extends URLClassLoader {

        public ExtendedURLClassLoader(URL[] urls) {
            super(urls, Types.class.getClassLoader());
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
