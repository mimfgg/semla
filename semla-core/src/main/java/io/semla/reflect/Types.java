package io.semla.reflect;

import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.ImmutableMap;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.semla.util.Unchecked.unchecked;


@SuppressWarnings("unchecked")
public final class Types {

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
    private static final Map<Class<?>, Map<Class<? extends Annotation>, Optional<Class<?>>>> ANNOTATED_SUPER_CLASSES = new LinkedHashMap<>();
    private static final Map<Class<?>, Map<String, Map<String, Class<?>>>> SUB_TYPES = new LinkedHashMap<>();

    private Types() {
    }

    public static <E> E safeNull(Type type, E value) {
        return safeNull(rawTypeOf(type), value);
    }

    public static <E> E safeNull(Class<?> clazz, E value) {
        if (clazz.isPrimitive() && value == null) {
            value = (E) Array.get(Array.newInstance(clazz, 1), 0);
        }
        return value;
    }

    public static Optional<Class<?>> optionalTypeArgumentOf(Type type) {
        return optionalTypeArgumentOf(type, 0);
    }

    public static Optional<Class<?>> optionalTypeArgumentOf(Type type, int argumentIndex) {
        return Optional.ofNullable(typeArgumentOf(type, argumentIndex));
    }

    public static <E> Class<E> typeArgumentOf(Type type) {
        return typeArgumentOf(type, 0);
    }

    public static <E> Class<E> typeArgumentOf(Type type, int argumentIndex) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getActualTypeArguments().length > argumentIndex) {
                return rawTypeOf(parameterizedType.getActualTypeArguments()[argumentIndex]);
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
            return () -> unchecked(() -> (E) clazz.newInstance());
        } else {
            throw new RuntimeException("Cannot create a supplier for " + clazz);
        }
    }

    public static <E> Class<E> rawTypeOf(Type type) {
        return type instanceof ParameterizedType ? (Class<E>) ((ParameterizedType) type).getRawType() : (Class<E>) type;
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
                + "\noutput:" + new String(out.toByteArray())
                + "\narguments: " + arguments);
        }
        unchecked(() -> Methods.invoke(Types.class.getClassLoader(), "addURL", new File(tmpDir).toURI().toURL()));
        return unchecked(() -> Files.walk(new File(tmpDir).toPath()))
            .filter(file -> file.getFileName().toString().endsWith(".class"))
            .map(file -> file.toAbsolutePath().toString().replace(tmpDir, "").replace(File.separator, ".").replace(".class", ""))
            .map(classname -> unchecked(() -> Class.forName(classname)))
            .collect(Collectors.toList());

    }

    public static List<Class<?>> compileFromSources(String... sources) {
        String tmpDir = createTempDirectory();
        return compileFromFiles(new ArrayList<>(), tmpDir, Stream.of(sources).map(source ->
            unchecked(() -> {
                String packagePath = getPackageFrom(source);
                String name = getClassNameFrom(source);
                File sourceFile = new File(tmpDir + packagePath + name + ".java");
                sourceFile.deleteOnExit();
                sourceFile.getParentFile().mkdirs();
                Files.write(sourceFile.toPath(), source.getBytes());
                return sourceFile;
            })).toArray(File[]::new)
        );
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
        return unchecked(() -> Files.createTempDirectory("").toAbsolutePath().toString() + File.separator);
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

    public static Type parameterized(Class<?> rawType, Type parameter, Type... parameters) {
        return new ParameterizedTypeImpl(rawType, io.semla.util.Arrays.concat(parameter, parameters));
    }

    public static class ParameterizedTypeBuilder {

        private final Class<?> rawType;

        public ParameterizedTypeBuilder(Class<?> rawType) {
            this.rawType = rawType;
        }

        public Type of(Type parameter, Type... parameters) {
            return new ParameterizedTypeImpl(rawType, io.semla.util.Arrays.concat(parameter, parameters));
        }
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Class<?> rawType;
        private final Type[] actualTypeArguments;

        private ParameterizedTypeImpl(Class<?> rawType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            if (rawType.getTypeParameters().length != actualTypeArguments.length) {
                throw new IllegalArgumentException(
                    "type " + rawType + " expects " + rawType.getTypeParameters().length
                        + " argument" + (rawType.getTypeParameters().length > 1 ? "s" : "")
                        + " but got " + actualTypeArguments.length);
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
            return rawType.getTypeName() + "<" + Stream.of(actualTypeArguments).map(Type::getTypeName).collect(Collectors.joining(", ")) + ">";
        }
    }
}
