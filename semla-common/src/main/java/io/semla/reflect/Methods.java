package io.semla.reflect;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Types.asAccessible;
import static io.semla.util.Unchecked.unchecked;
import static java.util.Collections.synchronizedMap;
import static java.util.Optional.ofNullable;

@Slf4j
public final class Methods {

    private static final Map<Class<?>, Map<Class<? extends Annotation>, List<MethodInvocator>>> ANNOTATED_METHODS = synchronizedMap(new HashMap<>());
    private static final Map<Class<?>, Map<String, Method>> METHOD_CACHE = synchronizedMap(new HashMap<>());

    private Methods() {
    }

    public static <E> Stream<Method> of(E instance) {
        return of(instance.getClass());
    }

    public static Stream<Method> of(Class<?> clazz) {
        return METHOD_CACHE.computeIfAbsent(clazz, Methods::recursivelyFindAllMethodsOf).values().stream();
    }

    public static Map<String, Method> byName(Class<?> clazz) {
        return METHOD_CACHE.computeIfAbsent(clazz, Methods::recursivelyFindAllMethodsOf);
    }

    private static Map<String, Method> recursivelyFindAllMethodsOf(Class<?> clazz) {
        Map<String, Method> methods = synchronizedMap(new LinkedHashMap<>());
        recursivelyFindAllMethodsOf(clazz, methods);
        return methods;
    }

    private static synchronized void recursivelyFindAllMethodsOf(Class<?> clazz, Map<String, Method> methods) {
        Stream.of(clazz.getDeclaredMethods())
            .filter(method -> !method.getDeclaringClass().equals(Object.class))
            .filter(method -> clazz.isAnnotation() || !Modifier.is(method, Modifier.ABSTRACT))
            .filter(method -> !methods.containsValue(method))
            .forEach(method ->
                methods.put(getMethodSignature(clazz, method.getName(), method.getParameterTypes()), asAccessible(method))
            );
        ofNullable(clazz.getSuperclass()).ifPresent(superClass -> recursivelyFindAllMethodsOf(superClass, methods));
        Stream.of(clazz.getInterfaces()).forEach(interfaceClass -> recursivelyFindAllMethodsOf(interfaceClass, methods));
    }

    @SuppressWarnings("unchecked")
    public static <R> R invoke(Class<?> clazz, String name, Object... parameters) {
        return (R) invoke(clazz, null, name, parameters);
    }

    @SuppressWarnings("unchecked")
    public static <R> R invoke(Object instance, String name, Object... parameters) {
        return (R) invoke(instance.getClass(), instance, name, parameters);
    }

    @SuppressWarnings("unchecked")
    public static <E, R> R invoke(E instance, Method method, Object... parameters) {
        return unchecked(() -> (R) method.invoke(instance, parameters), Throwable::getCause);
    }

    @SuppressWarnings("unchecked")
    private static <E, R> R invoke(Class<?> clazz, E instance, String name, Object... parameters) {
        Class<?>[] parameterTypes = Stream.of(io.semla.util.Arrays.emptyIfNull(parameters))
            .map(value -> value != null ? value.getClass() : Object.class)
            .toArray(Class<?>[]::new);
        return (R) unchecked(() -> getMethod(clazz, name, parameterTypes).invoke(instance, parameters));
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return unchecked(() -> findMethod(clazz, name, parameterTypes)
            .orElseThrow(() -> new NoSuchMethodException("method '" + name + "' doesn't exist on " + clazz))
        );
    }

    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return ofNullable(byName(clazz).computeIfAbsent(
                getMethodSignature(clazz, name, parameterTypes),
                methodSignature -> byName(clazz).values().stream()
                    .filter(method -> isApplicableMethod(method, name, parameterTypes))
                    .min(Comparator.comparingInt(m ->
                        Types.computeInheritanceDistanceBetween(parameterTypes, m.getParameterTypes())
                    )).orElse(null)
            )
        );
    }

    private static boolean isApplicableMethod(Method method, String name, Class<?>[] parameterTypes) {
        if (name.equals(method.getName()) && method.getParameterTypes().length == parameterTypes.length) {
            boolean isApplicableMethod = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!Types.isAssignableTo(parameterTypes[i], method.getParameterTypes()[i])) {
                    isApplicableMethod = false;
                    break;
                }
            }
            return isApplicableMethod;
        }
        return false;
    }

    private static String getMethodSignature(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        return clazz.getCanonicalName() + "." + name + Arrays.toString(parameterTypes);
    }

    public static Stream<MethodInvocator> findAnnotatedWith(Class<?> type, Class<? extends Annotation> annotation) {
        return ANNOTATED_METHODS
            .computeIfAbsent(type, t -> synchronizedMap(new LinkedHashMap<>()))
            .computeIfAbsent(annotation, a ->
                Stream.of(type.getMethods())
                    .filter(method -> method.isAnnotationPresent(a))
                    .map(MethodInvocator::new)
                    .collect(Collectors.toList())
            ).stream();
    }

    public record MethodInvocator(Method method) {

        public Object invoke(Object host, Object... parameters) {
            return unchecked(() -> method.invoke(host, parameters), Throwable::getCause);
        }
    }
}
