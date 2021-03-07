package io.semla.reflect;

import io.semla.util.Maps;
import io.semla.util.Singleton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.semla.reflect.Modifier.*;

public class Properties {

    private Properties() {}

    public static <T> Map<String, Member<T>> membersOf(Class<T> clazz) {
        return Singleton.named("Properties.membersOf." + clazz.getCanonicalName(), () -> {
            Map<String, Member<T>> membersByName = new LinkedHashMap<>();
            Fields.byName(clazz).forEach((name, field) -> {
                if (isVisible(field)) {
                    membersByName.put(name, Member.from(field));
                }
            });
            Map<String, Method[]> methodsByName = new LinkedHashMap<>();
            Methods.byName(clazz).forEach((name, method) -> {
                if (isVisible(method)) {
                    if (Getter.isGetter(method)) {
                        methodsByName.computeIfAbsent(Getter.stripPrefix(method), m -> new Method[2])[0] = method;
                    } else if (Setter.isSetter(method)) {
                        methodsByName.computeIfAbsent(Setter.stripPrefix(method), m -> new Method[2])[1] = method;
                    }
                }
            });
            methodsByName.forEach((name, methods) -> {
                        Field field = Fields.getField(clazz, name);
                        if (field != null) {
                            Method getter = methods[0];
                            if (getter != null && !getter.getReturnType().equals(field.getType())) {
                                getter = null;
                            }
                            Method setter = methods[1];
                            if (setter != null && !setter.getParameterTypes()[0].equals(field.getType())) {
                                setter = null;
                            }
                            membersByName.put(name, Member.from(field, getter, setter));
                        }
                    }
            );
            return sortByFieldOrder(membersByName, clazz);
        }).get();
    }

    public static boolean isVisible(Method method) {
        return Modifier.not(method, STATIC, TRANSIENT, PROTECTED, PRIVATE);
    }

    public static boolean isVisible(Field field) {
        return Modifier.not(field, STATIC, TRANSIENT, PROTECTED, PRIVATE);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<Getter<T>> gettersOf(T instance) {
        return gettersOf((Class<T>) instance.getClass());

    }

    public static <T> List<Getter<T>> gettersOf(Class<T> clazz) {
        return Singleton.named("Properties.gettersOf." + clazz.getCanonicalName(), () -> {
            Map<String, Getter<T>> gettersByName = new LinkedHashMap<>();
            Fields.byName(clazz).forEach((name, field) -> {
                if (isVisible(field)) {
                    gettersByName.put(name, Member.from(field));
                }
            });

            Methods.byName(clazz).forEach((name, method) -> {
                if (isVisible(method) && method.getParameterCount() == 0 && !method.getReturnType().equals(Void.class)) {
                    Getter<T> getter = Getter.from(method);
                    gettersByName.put(getter.serializeAs(), getter);
                }
            });

            return new ArrayList<>(sortByFieldOrder(gettersByName, clazz).values());
        }).get();
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, Setter<T>> settersOf(T instance) {
        return settersOf((Class<T>) instance.getClass());
    }

    public static <T> Map<String, Setter<T>> settersOf(Class<T> clazz) {
        return Singleton.named("Properties.settersOf." + clazz.getCanonicalName(), () -> {
            Map<String, Setter<T>> settersByName = new LinkedHashMap<>();
            Fields.byName(clazz).forEach((name, field) -> {
                if (isVisible(field)) {
                    settersByName.put(name, Member.from(field));
                }
            });

            Methods.byName(clazz).forEach((name, method) -> {
                if (isVisible(method) && method.getParameterCount() == 1) {
                    Setter<T> setter = Setter.from(method);
                    settersByName.put(setter.deserializeFrom(), setter);
                }
            });

            return settersByName;
        }).get();
    }

    private static <T, PropertyType extends Property<T>> Map<String, PropertyType> sortByFieldOrder(Map<String, PropertyType> propertiesByName, Class<T> clazz) {
        // methods come in random order, but at least we can try to match the fields order
        List<String> fieldsNames = new ArrayList<>(Fields.byName(clazz).keySet());
        return propertiesByName.entrySet().stream()
                .sorted((o1, o2) -> {
                    if (fieldsNames.contains(o1.getKey()) && fieldsNames.contains(o2.getKey())) {
                        return Integer.compare(fieldsNames.indexOf(o1.getKey()), fieldsNames.indexOf(o2.getKey()));
                    } else if (fieldsNames.contains(o1.getKey())) {
                        return -1;
                    } else if (fieldsNames.contains(o2.getKey())) {
                        return 1;
                    }
                    return 0;
                })
                .collect(Maps.collect(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static boolean hasMember(Class<?> clazz, String name) {
        return membersOf(clazz).containsKey(name);
    }
}
