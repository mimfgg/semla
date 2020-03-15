package io.semla.model;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.persistence.annotations.Indexed;
import io.semla.persistence.annotations.Indices;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.relation.*;
import io.semla.util.Arrays;
import io.semla.util.*;

import javax.persistence.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.reflect.Types.typeArgumentOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("unchecked")
public class EntityModel<T> extends Model<T> {

    private final Column<T> key;
    private final Map<String, Column<T>> columnsByFieldName;
    private final List<Relation<T, ?>> relations;
    private final Column<T> version;
    private final List<Index<T>> indices;
    private final Type optionalType;
    private final Type listType;
    private final Type mapType;
    private String tablename;

    protected EntityModel(Class<T> clazz) {
        super(clazz);

        tablename = Optional.ofNullable(clazz.getAnnotation(Table.class))
            .filter(table -> !table.name().equals(""))
            .map(Table::name)
            .orElseGet(() -> Strings.toSnakeCase(clazz.getSimpleName()));

        Map<String, Column<T>> columnsByAccessor = new LinkedHashMap<>();
        Map<String, Index<T>> indicesByName = new LinkedHashMap<>();

        // first we look for the @Id member, if the fields are not properly ordered, the relations initialization can fail.
        List<Member<T>> ids = members().stream()
            .filter(member -> member.annotation(Id.class).isPresent())
            .collect(Collectors.toList());
        if (ids.size() == 1) {
            // simple primary key, things have only one id.
            key = new Column<>(ids.get(0));
        } else if (ids.size() > 1) {
            // we don't support composite keys
            throw new InvalidPersitenceAnnotationException("multiple @Id columns (composite keys) are not supported on " + clazz);
        } else {
            // nor embeddedIds
            members().stream()
                .filter(field -> field.annotation(EmbeddedId.class).isPresent())
                .findFirst().ifPresent(key -> {
                throw new InvalidPersitenceAnnotationException("@EmbeddedId (nested composite keys) are not supported on " + clazz);
            });
            throw new InvalidPersitenceAnnotationException("@Id is missing for on " + clazz);
        }
        indicesByName.put(key.member().getName(), new Index<>(key.member().getName(), true, true, key));
        columnsByAccessor.put(key.member().getName(), key);

        // then the columns and relationContext
        Column<T> version = null;
        List<Relation<T, ?>> relations = new ArrayList<>();
        for (Member<T> member : members()) {
            if (member.equals(key.member())) {
                continue;
            }

            Column<T> column = new Column<>(member);
            Index<T> index = null;

            if (member.annotation(Indexed.class).isPresent()) {
                Indexed indexed = member.annotation(Indexed.class).get();
                index = new Index<>(Optional.of(indexed.name()).filter(Strings::notNullOrEmpty)
                    .orElse(Strings.toSnakeCase(member.getName()) + "_idx"), indexed.unique(), false, column);
            }

            if (member.annotation(Version.class).isPresent()) {
                version = column;
            } else if (member.annotation(OneToOne.class).isPresent()) {
                NToOneRelation<T, ?> oneToOneRelation = OneToOneRelation.of(member, member.annotation(OneToOne.class).get(), this, member.getType());
                relations.add(oneToOneRelation);
                if (oneToOneRelation instanceof ForeignNToOneRelation) {
                    if (index == null) {
                        index = new Index<>(column);
                    }
                } else {
                    continue;
                }
            } else if (member.annotation(OneToMany.class).isPresent()) {
                if (member.annotation(Embedded.class).isPresent()) {
                    relations.add(new EmbeddedToManyRelation<>(member, member.annotation(OneToMany.class).get(), this, typeArgumentOf(member.getGenericType())));
                } else {
                    relations.add(new OneToManyRelation<>(member, member.annotation(OneToMany.class).get(), this, typeArgumentOf(member.getGenericType())));
                    continue;
                }
            } else if (member.annotation(ManyToOne.class).isPresent()) {
                relations.add(new ManyToOneRelation<>(member, member.annotation(ManyToOne.class).get(), this, member.getType()));
                if (index == null) {
                    index = new Index<>(column);
                }
            } else if (member.annotation(ManyToMany.class).isPresent()) {
                relations.add(new ManyToManyRelation<>(member, member.annotation(ManyToMany.class).get(), this, typeArgumentOf(member.getGenericType())));
                continue;
            }

            if (column.unique() && index == null) {
                index = new Index<>(column.name() + "_idx", true, false, column);
            }

            columnsByAccessor.put(member.getName(), column);

            if (index != null) {
                if (indicesByName.containsKey(index.name())) {
                    throw new IllegalStateException("duplicated index name on " + clazz);
                }
                indicesByName.put(index.name(), index);
            }
        }

        // Indices defined on the type
        if (clazz.isAnnotationPresent(Indices.class)) {
            Stream.of(clazz.getAnnotation(Indices.class).value())
                .map(index -> Pair.of(
                    Optional.of(index.name()).filter(Strings::notNullOrEmpty)
                        .orElse(Stream.of(index.properties())
                            .map(columnsByAccessor::get)
                            .map(Column::name)
                            .map(Strings::toSnakeCase)
                            .collect(Collectors.joining("_")) + "_idx"),
                    index
                ))
                .filter(index -> !indicesByName.containsKey(index.getKey()))
                .forEach(index ->
                    indicesByName.put(index.getKey(),
                        new Index<>(index.getKey(), index.getValue().unique(), false, Stream.of(index.getValue().properties()).map(columnsByAccessor::get).toArray(Column[]::new))
                    )
                );
        }

        this.columnsByFieldName = ImmutableMap.copyOf(columnsByAccessor);
        this.relations = ImmutableList.copyOf(relations);
        this.version = version;
        this.indices = ImmutableList.copyOf(indicesByName.values());
        this.optionalType = Types.parameterized(Optional.class, getType());
        this.listType = Types.parameterized(List.class, getType());
        this.mapType = Types.parameterized(Map.class, key().member().getType(), getType());

        if (logger.isTraceEnabled()) {
            logger.trace("summary: " + toString());
        }
    }

    public String tablename() {
        return tablename;
    }

    public Column<T> key() {
        return key;
    }

    public Optional<Column<T>> version() {
        return Optional.ofNullable(version);
    }

    public List<Index<T>> indices() {
        return indices;
    }

    public boolean isIndexed(Member<T> member) {
        return indices.stream().map(index -> index.columns().stream().map(Column::member)).flatMap(identity()).collect(toSet()).contains(member);
    }

    public Collection<Column<T>> columns() {
        return columnsByFieldName.values();
    }

    public Column<T> getColumn(Member<T> member) {
        return columnsByFieldName.get(member.getName());
    }

    public boolean isColumn(Member<T> member) {
        return columns().stream().anyMatch(c -> c.member().equals(member));
    }

    public List<Relation<T, ?>> relations() {
        return relations;
    }

    public <R> Relation<T, R> getRelation(Member<T> member) {
        return (Relation<T, R>) relations.stream()
            .filter(r -> r.member().equals(member))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(member + " is not a relation on " + getType()));
    }

    public boolean isRelation(Member<T> member) {
        return relations.stream().anyMatch(r -> r.member().equals(member));
    }

    public <R> Relation<T, R> getRelation(String fieldName) {
        return (Relation<T, R>) relations.stream()
            .filter(r -> r.member().getName().equals(fieldName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("couldn't find relation " + fieldName + " on " + getType()));
    }

    public T newInstanceFromKey(Object key) {
        T reference = newInstance();
        key().member().setOn(reference, key);
        return reference;
    }

    @Override
    protected StringBuilder getDetails() {
        StringBuilder details = super.getDetails();
        details.append("\n\tkey:\n\t\t").append(key);
        if (!columnsByFieldName.isEmpty()) {
            details.append("\n\tcolumns:");
            columnsByFieldName.values().forEach(column -> details.append("\n\t\t").append(column));
        }
        if (!relations.isEmpty()) {
            details.append("\n\trelations:");
            relations.forEach(relation -> details.append("\n\t\t").append(relation));
        }
        return details;
    }

    public String toKeyString(T instance) {
        if (key().member().isDefaultOn(instance)) {
            return getType().getCanonicalName() + "@" + Integer.toHexString(instance.hashCode());
        }
        return getType().getCanonicalName() + "::" + key().member().getOn(instance);
    }

    public Type getOptionalType() {
        return optionalType;
    }

    public Type getListType() {
        return listType;
    }

    public Type getMapType() {
        return mapType;
    }

    public static <T> EntityModel<T> of(String name) {
        return of(getClassBy(name));
    }

    public static <T> EntityModel<T> of(T instance) {
        return (EntityModel<T>) Model.of(instance);
    }

    public static <T> EntityModel<T> of(Collection<T> entities) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("collection was empty!");
        }
        return of((Class<T>) entities.iterator().next().getClass());
    }

    public static <T> EntityModel<T> of(Class<T> clazz) {
        return (EntityModel<T>) Model.of(clazz);
    }

    public static <T> boolean isNotNullOrReference(T instance) {
        return instance != null && !isReference(instance);
    }

    public static <T> boolean isReference(T instance) {
        if (instance instanceof Collection) {
            return EntityModel.containsOnlyReferences((Collection<?>) instance);
        }
        if (!isEntity(instance)) {
            return false;
        }
        EntityModel<T> model = EntityModel.of(instance);
        if (!model.key().member().isDefaultOn(instance)) {
            return model.members().stream()
                .filter(member -> !member.equals(model.key().member()))
                .allMatch(member -> member.isDefaultOn(instance));
        }
        return false;
    }

    public static <T> T referenceTo(T instance) {
        EntityModel<T> model = EntityModel.of(instance);
        return referenceTo(model, model.key().member().getOn(instance));
    }

    public static <K, T> T referenceTo(Class<T> clazz, K key) {
        return referenceTo(EntityModel.of(clazz), key);
    }

    public static <K, T> T referenceTo(EntityModel<T> model, K key) {
        if (key == null || model.isDefaultValue(model.key().member(), key)) {
            return null;
        }
        return model.newInstanceFromKey(key);
    }

    public static boolean isEntity(Type type) {
        return isEntity(Types.rawTypeOf(type));
    }

    public static boolean isEntity(Object instance) {
        return instance != null && isEntity(instance.getClass());
    }

    public static <T> boolean isEntity(Class<T> clazz) {
        return clazz.isAnnotationPresent(Entity.class) || (Types.hasSuperClass(clazz) && isEntity(clazz.getSuperclass()));
    }

    public static Object keyOf(Object instance) {
        if (instance == null) {
            return null;
        }
        return of(instance).key().member().getOn(instance);
    }

    public static boolean containsEntities(Collection<?> values) {
        return values.stream().findFirst().filter(EntityModel::isEntity).isPresent();
    }

    public static boolean containsOnlyReferences(Collection<?> values) {
        return values.stream().allMatch(EntityModel::isReference);
    }

    public static <T> T copy(T instance) {
        T copy = null;
        if (instance != null) {
            Model<T> model = Model.of(instance);
            copy = model.newInstance();
            for (Member<T> member : model.members()) {
                Object value = member.getOn(instance);
                if (value != null) {
                    if (isEntity(member.getGenericType())) {
                        if (!member.annotation(JoinTable.class).isPresent()) { // foreign keys should not be copied if they are hosted in a joinTable
                            member.setOn(copy, EntityModel.referenceTo(value));
                        }
                    } else if (member.annotation(Embedded.class).isPresent() && member.isAnnotatedWithOneOf(Arrays.of(OneToMany.class, ManyToMany.class))) {
                        member.setOn(copy,
                            ((Collection<?>) value).stream().map(EntityModel::referenceTo)
                                .collect(Collectors.toCollection(Types.supplierOf(member.getGenericType()))));
                    } else if (member.annotation(Embedded.class).isPresent() || !member.isAssignableToOneOf(Collection.class, Map.class)) {
                        member.setOn(copy, member.getOn(instance));
                    }
                }
            }
        }
        return copy;
    }

}
