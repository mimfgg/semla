package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.reflect.Types;
import io.semla.relation.IncludeType;
import io.semla.relation.IncludeTypes;
import io.semla.relation.Relation;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.Strings;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Includes<T> {

    private final EntityModel<T> model;
    private final Map<Relation<T, ?>, Include<T, ?>> relations = new LinkedHashMap<>();

    private Includes(EntityModel<T> model) {
        this.model = model;
    }

    public EntityModel<T> model() {
        return model;
    }

    public Map<Relation<T, ?>, Include<T, ?>> relations() {
        return relations;
    }

    public Includes<T> include(String include) {
        include = include.replaceAll("including (?:its|their) (.*)", "$1");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < include.length(); i++) {
            char c = include.charAt(i);
            switch (c) {
                case ' ':
                    break;
                case ',':
                    if (sb.length() == 0) {
                        throw new IllegalArgumentException("unexpected ',' encountered in \"" + include + "\"");
                    }
                    relations.computeIfAbsent(model.getRelation(sb.toString()), Include::anyOf);
                    sb = new StringBuilder();
                    break;
                case '{':
                    int closingBracket = Strings.getClosingBracketIndex(include, i, '{', '}');
                    if (closingBracket > -1) {
                        relations.computeIfAbsent(model.getRelation(sb.toString()), Include::anyOf)
                            .includes().include(include.substring(i + 1, closingBracket));
                        sb = new StringBuilder();
                        i = closingBracket;
                        break;
                    } else {
                        throw new IllegalStateException("no } is closing { in \"" + include + "\"");
                    }
                case '[':
                    closingBracket = Strings.getClosingBracketIndex(include, i, '[', ']');
                    if (closingBracket > -1) {
                        IncludeType[] includeTypes = Yaml.defaultDeserializer()
                            .<List<IncludeType>>read(include.substring(i, closingBracket), Types.parameterized(List.class, IncludeType.class))
                            .toArray(new IncludeType[0]);
                        relations.computeIfAbsent(model.getRelation(sb.toString()), relation -> new Include<>(relation, new IncludeTypes(includeTypes)));
                        i = closingBracket;
                        break;
                    } else {
                        throw new IllegalStateException("no ] is closing [ in \"" + include + "\"");
                    }
                default:
                    if (!Character.isLetter(c)) {
                        throw new IllegalArgumentException("unexpected '" + c + "' encountered in \"" + include + "\"");
                    }
                    sb.append(c);
                    break;
            }
        }
        if (sb.length() > 0) {
            relations.computeIfAbsent(model.getRelation(sb.toString()), Include::anyOf);
        }
        return this;
    }

    public <R> Includes<T> include(Relation<T, R> relation) {
        relations.put(relation, new Include<>(relation, IncludeTypes.none(), Includes.of(relation.childModel())));
        return this;
    }

    public <R> Includes<T> include(Relation<T, R> relation, IncludeTypes includeTypes, Includes<R> includes) {
        relations.put(relation, new Include<>(relation, includeTypes, includes));
        return this;
    }

    public <R> Includes<T> include(String relationName, Function<Include<T, R>, Include<T, R>> function) {
        Relation<T, R> relation = model.getRelation(relationName);
        return include(relation, Include.anyOf(relation), function);
    }

    public <R> Includes<T> include(Relation<T, R> relation, Include<T, R> include, Function<Include<T, R>, Include<T, R>> function) {
        relations.put(relation, function.apply(include));
        return this;
    }

    public Includes<T> addAll(List<Include<T, ?>> includes) {
        includes.forEach(include -> this.relations.put(include.relation, include));
        return this;
    }

    public boolean isEmpty() {
        return relations.isEmpty();
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    protected StringBuilder appendTo(StringBuilder builder) {
        relations.values().forEach(include -> include.appendTo(builder));
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder;
    }

    public Include<T, ?> get(String fieldName) {
        return get(model.getRelation(fieldName));
    }

    public Include<T, ?> get(Relation<T, ?> relation) {
        return relations.get(relation);
    }

    public Includes<T> none() {
        relations.clear();
        return this;
    }

    public T fetchOn(T entity, PersistenceContext context) {
        relations.values().forEach(include -> context.fetchOn(entity, include));
        return entity;
    }

    public <CollectionType extends Collection<T>> CollectionType fetchOn(CollectionType entities, PersistenceContext context) {
        relations.values().forEach(include -> context.fetchOn(entities, include));
        return entities;
    }

    public T createOrUpdateOn(T entity, PersistenceContext context) {
        relations.values().forEach(include -> context.createOrUpdateOn(entity, include));
        return entity;
    }

    public <CollectionType extends Collection<T>> CollectionType createOrUpdateOn(CollectionType entities, PersistenceContext context) {
        relations.values().forEach(include -> context.createOrUpdateOn(entities, include));
        return entities;
    }

    public T deleteOn(T entity, PersistenceContext context) {
        relations.values().forEach(include -> context.deleteOn(entity, include));
        return entity;
    }

    public <CollectionType extends Collection<T>> CollectionType deleteOn(CollectionType entities, PersistenceContext context) {
        relations.values().forEach(include -> context.deleteOn(entities, include));
        return entities;
    }

    public Includes<T> addTo(Includes<T> includes) {
        includes.relations().putAll(this.relations);
        return includes;
    }

    public Includes<T> override(Includes<T> ignore) {
        return this;
    }

    public static <T> Includes<T> of(Class<T> clazz) {
        return of(EntityModel.of(clazz));
    }

    public static <T> Includes<T> of(EntityModel<T> model) {
        return new Includes<>(model);
    }

    public static <T> Includes<T> defaultEagersOf(EntityModel<T> model) {
        return matching(model, IncludeType.FETCH);
    }

    public static <T> Includes<T> defaultPersistsOrMergesOf(EntityModel<T> model) {
        return matching(model, IncludeType.CREATE, IncludeType.UPDATE);
    }

    public static <T> Includes<T> defaultRemovesOrDeleteOf(EntityModel<T> model) {
        return matching(model, IncludeType.DELETE, IncludeType.DELETE_ORPHANS);
    }

    public static <T> Includes<T> matching(EntityModel<T> model, IncludeType... includeTypes) {
        return matching(model, new IncludeTypes(includeTypes));
    }

    public static <T> Includes<T> matching(EntityModel<T> model, IncludeTypes includeType) {
        return matching(model, includeType, new LinkedHashSet<>());
    }

    public static <T> Includes<T> matching(EntityModel<T> model, IncludeTypes includeType, Set<Relation<?, ?>> knownRelations) {
        return new Includes<>(model).addAll(model.relations().stream()
            .filter(relation -> knownRelations.add(relation) && relation.defaultIncludeType().matchesAnyOf(includeType))
            .map(relation -> new Include<>(relation, relation.defaultIncludeType(), knownRelations))
            .collect(Collectors.toList()));
    }
}
