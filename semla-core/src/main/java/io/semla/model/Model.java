package io.semla.model;

import io.semla.exception.SemlaException;
import io.semla.reflect.Getter;
import io.semla.reflect.Member;
import io.semla.reflect.Properties;
import io.semla.reflect.Types;
import io.semla.serialization.Deserializer;
import io.semla.serialization.json.Json;
import io.semla.util.Plural;
import io.semla.util.Singleton;
import io.semla.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import static io.semla.model.EntityModel.isEntity;

@SuppressWarnings("unchecked")
public class Model<T> {

    private static final ReentrantLock LOCK = new ReentrantLock(true);
    private static final Map<Class<?>, Model<?>> MODELS = new LinkedHashMap<>();
    private static final Map<String, Class<?>> CLASSES_BY_NAME = new LinkedHashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final Singleton<T> defaultInstance = Singleton.lazy(this::newInstance);

    protected final Class<T> clazz;
    protected final String singularName;
    protected final String pluralName;
    protected final Map<String, Member<T>> membersByName;

    protected Model(Class<T> clazz) {
        logger.trace("initializing model for {}", clazz);
        this.clazz = clazz;
        this.singularName = Strings.decapitalize(clazz.getSimpleName());
        this.pluralName = Strings.decapitalize(Plural.of(clazz.getSimpleName()));
        this.membersByName = Properties.membersOf(clazz);
    }

    public Class<T> getType() {
        return clazz;
    }

    public String singularName() {
        return singularName;
    }

    public String pluralName() {
        return pluralName;
    }

    public void merge(T from, T to) {
        members().forEach(member -> {
            Object valueOnFrom = member.getOn(from);
            Object valueOnTo = member.getOn(to);
            if (!Objects.equals(valueOnFrom, valueOnTo)) {
                Object defaultValue = member.getOn(defaultInstance.get());
                if (!Objects.equals(valueOnFrom, defaultValue)) {
                    if (EntityModel.isReference(valueOnFrom) && !Objects.equals(valueOnTo, defaultValue)) {
                        return;
                    }
                    if (Objects.equals(valueOnTo, defaultValue) || EntityModel.isReference(valueOnTo)) {
                        member.setOn(to, valueOnFrom);
                    } else {
                        throw new SemlaException(
                            "couldn't merge already set value to '" + valueOnTo + "' for '" + member + "', was '" + valueOnFrom + "'");
                    }
                }
            }
        });
    }

    public Collection<Member<T>> members() {
        return membersByName.values();
    }

    public Member<T> member(String name) {
        assertHasMember(name);
        return membersByName.get(name);
    }

    public void assertHasMember(String name) {
        if (!hasMember(name)) {
            throw new SemlaException(clazz.getCanonicalName() + " doesn't have any member named '" + name + "'");
        }
    }

    public boolean hasMember(String name) {
        return membersByName.containsKey(name);
    }

    public T newInstance() {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SemlaException("couldn't create a new instance of " + clazz, e);
        }
    }

    public T newInstanceWithValues(Map<String, Object> values) {
        return Json.read(Json.write(values), getType(), Deserializer.UNWRAP_STRINGS);
    }

    @Override
    public String toString() {
        return getDetails().toString();
    }

    protected StringBuilder getDetails() {
        StringBuilder details = new StringBuilder();
        details.append("\n\tclass: ").append(clazz.getCanonicalName());
        details.append("\n\tsingular name: ").append(singularName);
        details.append("\n\tplural name: ").append(pluralName);
        details.append("\n\tmembers: ");
        members().forEach(member ->
            details.append("\n\t\t").append(member.toGenericString()).append(" (default: ").append((Object) member.getOn(defaultInstance.get())).append(")")
        );
        return details;
    }

    public boolean isDefault(Getter<T> getter, T instance) {
        return isDefaultValue(getter, getter.getOn(instance));
    }

    public boolean isDefaultValue(Getter<T> getter, Object value) {
        return Objects.equals(value, getter.getOn(defaultInstance.get()));
    }

    public static <T> Model<T> of(T instance) {
        return of((Class<T>) instance.getClass());
    }

    public static <T> Model<T> of(Class<T> clazz) {
        if (Types.isAssignableTo(clazz, Proxy.class)) {
            clazz = (Class<T>) clazz.getInterfaces()[0];
        }
        Model<T> model = (Model<T>) MODELS.get(clazz);
        if (model == null) {
            try {
                LOCK.lock();
                if (!MODELS.containsKey(clazz)) {
                    model = isEntity(clazz) ? new EntityModel<>(clazz) : new Model<>(clazz);
                    MODELS.put(clazz, model);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return model;
    }

    public static <T> Class<T> getClassBy(String name) {
        return (Class<T>) CLASSES_BY_NAME.computeIfAbsent(name, k -> findClassBy(name));
    }

    @SuppressWarnings({"ThrowableInstanceNotThrown", "ThrowableInstanceNeverThrown"})
    private static <T> Class<T> findClassBy(String name) {
        return MODELS.values().stream()
            .filter(model -> model.getType().getCanonicalName().equalsIgnoreCase(name)
                || model.getType().getSimpleName().equalsIgnoreCase(name)
                || model.singularName().equals(name)
                || model.pluralName().equals(name)
            )
            .map(model -> (Class<T>) model.getType())
            .findFirst()
            .orElseGet(() -> {
                try {
                    return (Class<T>) Class.forName(name);
                } catch (ClassNotFoundException ex) {
                    throw new SemlaException("could not find any class known by the name '" + name + "'");
                }
            });
    }

    public static void clear() {
        MODELS.clear();
        CLASSES_BY_NAME.clear();
    }
}
