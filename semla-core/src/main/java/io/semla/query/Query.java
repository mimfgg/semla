package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.persistence.PersistenceContext;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.serialization.json.Json;
import io.semla.util.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.of;
import static io.semla.util.Singleton.lazy;

public class Query<T, ReturnType> {

    private final EntityModel<T> model;
    private final Function<PersistenceContext, ReturnType> function;
    private final Supplier<String> toString;

    public Query(EntityModel<T> model, Function<PersistenceContext, ReturnType> function, Supplier<String> toString) {
        this.model = model;
        this.function = function;
        this.toString = toString;
    }

    public EntityModel<T> model() {
        return model;
    }

    public ReturnType in(PersistenceContext context) {
        return function.apply(context);
    }

    @Override
    public String toString() {
        return toString.get();
    }

    public static <T> Query<T, Optional<T>> get(Class<T> clazz, Object key) {
        return get(clazz, key, Includes.defaultEagersOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Optional<T>> get(Class<T> clazz, Object key, UnaryOperator<Includes<T>> includes) {
        return get(key, includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, Optional<T>> get(Object key, Includes<T> includes) {
        return new Query<>(includes.model(), context -> context.get(key, includes), lazy(() ->
            String.format(
                "get the %s where %s is %s%s",
                includes.model().singularName(),
                includes.model().key().member().getName(),
                key,
                Strings.prefixIfNotNullOrEmpty(" including its ", includes.toString())
            )
        ));
    }

    public static <K, T> Query<T, Map<K, T>> get(Class<T> clazz, Collection<K> keys) {
        return get(clazz, keys, Includes.defaultEagersOf(EntityModel.of(clazz))::addTo);
    }

    public static <K, T> Query<T, Map<K, T>> get(Class<T> clazz, Collection<K> keys, UnaryOperator<Includes<T>> includes) {
        return get(keys, includes.apply(Includes.of(clazz)));
    }

    public static <K, T> Query<T, Map<K, T>> get(Collection<K> keys, Includes<T> includes) {
        return new Query<>(includes.model(), context -> context.get(keys, includes), lazy(() ->
            String.format(
                "get the %s where %s in %s%s",
                includes.model().pluralName(),
                includes.model().key().member().getName(),
                Json.write(keys),
                Strings.prefixIfNotNullOrEmpty(" including their ", includes.toString())
            )
        ));
    }

    public static <T> Query<T, Optional<T>> first(Class<T> clazz) {
        return first(clazz, UnaryOperator.identity());
    }

    public static <T> Query<T, Optional<T>> first(Class<T> clazz, UnaryOperator<Predicates<T>> predicates) {
        return first(clazz, predicates, UnaryOperator.identity());
    }

    public static <T> Query<T, Optional<T>> first(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination) {
        return first(clazz, predicates, pagination, Includes.defaultEagersOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Optional<T>> first(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination, UnaryOperator<Includes<T>> includes) {
        return first(predicates.apply(Predicates.of(clazz)), pagination.apply(Pagination.of(clazz)), includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, Optional<T>> first(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return new Query<>(predicates.model(), context -> context.first(predicates, pagination, includes), lazy(() ->
            String.format("fetch the first %s%s%s%s",
                predicates.model().singularName(),
                Strings.prefixIfNotNullOrEmpty(" where ", predicates.toString()),
                Strings.prefixIfNotNullOrEmpty(" ", pagination.toString()),
                Strings.prefixIfNotNullOrEmpty(" including its ", includes.toString())
            )));
    }

    public static <T> Query<T, List<T>> list(Class<T> clazz) {
        return list(clazz, UnaryOperator.identity());
    }

    public static <T> Query<T, List<T>> list(Class<T> clazz, UnaryOperator<Predicates<T>> predicates) {
        return list(clazz, predicates, UnaryOperator.identity());
    }

    public static <T> Query<T, List<T>> list(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination) {
        return list(clazz, predicates, pagination, Includes.defaultEagersOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, List<T>> list(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination, UnaryOperator<Includes<T>> includes) {
        return list(predicates.apply(Predicates.of(clazz)), pagination.apply(Pagination.of(clazz)), includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, List<T>> list(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return new Query<>(predicates.model(), context -> context.list(predicates, pagination, includes), lazy(() ->
            String.format("list all the %s%s%s%s",
                predicates.model().pluralName(),
                Strings.prefixIfNotNullOrEmpty(" where ", predicates.toString()),
                Strings.prefixIfNotNullOrEmpty(" ", pagination.toString()),
                Strings.prefixIfNotNullOrEmpty(" including their ", includes.toString())
            )));
    }

    public static <T> Query<T, Long> delete(Class<T> clazz) {
        return delete(clazz, UnaryOperator.identity());
    }

    public static <T> Query<T, Boolean> delete(Class<T> clazz, Object key) {
        return delete(clazz, key, Includes.defaultRemovesOrDeleteOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Boolean> delete(Class<T> clazz, Object key, UnaryOperator<Includes<T>> includes) {
        return delete(key, includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, Boolean> delete(Object key, Includes<T> includes) {
        return new Query<>(includes.model(), context -> context.delete(key, includes), lazy(() ->
            String.format("delete the %s where %s is %s%s",
                includes.model().singularName(),
                includes.model().key().member().getName(),
                key,
                Strings.prefixIfNotNullOrEmpty(" including its ", includes.toString())
            )));
    }

    public static <T> Query<T, Long> delete(Class<T> clazz, Collection<?> keys) {
        return delete(clazz, keys, Includes.defaultRemovesOrDeleteOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Long> delete(Class<T> clazz, Collection<?> keys, UnaryOperator<Includes<T>> includes) {
        return delete(keys, includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, Long> delete(Collection<?> keys, Includes<T> includes) {
        return new Query<>(includes.model(), context -> context.delete(keys, includes), lazy(() ->
            String.format("delete the %s where %s in %s%s",
                includes.model().pluralName(),
                includes.model().key().member().getName(),
                Json.write(keys),
                Strings.prefixIfNotNullOrEmpty(" including their ", includes.toString())
            )));
    }


    public static <T> Query<T, Long> delete(Class<T> clazz, UnaryOperator<Predicates<T>> predicates) {
        return delete(clazz, predicates, UnaryOperator.identity(), Includes.defaultRemovesOrDeleteOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Long> delete(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination) {
        return delete(clazz, predicates, pagination, Includes.defaultRemovesOrDeleteOf(EntityModel.of(clazz))::addTo);
    }

    public static <T> Query<T, Long> delete(Class<T> clazz, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination, UnaryOperator<Includes<T>> includes) {
        return delete(predicates.apply(Predicates.of(clazz)), pagination.apply(Pagination.of(clazz)), includes.apply(Includes.of(clazz)));
    }

    public static <T> Query<T, Long> delete(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return new Query<>(predicates.model(), context -> context.delete(predicates, pagination, includes), lazy(() ->
            String.format("delete the %s%s%s%s",
                predicates.model().pluralName(),
                Strings.prefixIfNotNullOrEmpty(" where ", predicates.toString()),
                Strings.prefixIfNotNullOrEmpty(" ", pagination.toString()),
                Strings.prefixIfNotNullOrEmpty(" including their ", includes.toString())
            )));
    }

    public static <T> Query<T, Long> count(Class<T> clazz) {
        return count(clazz, UnaryOperator.identity());
    }

    public static <T> Query<T, Long> count(Class<T> clazz, UnaryOperator<Predicates<T>> predicates) {
        return count(predicates.apply(Predicates.of(clazz)));
    }

    public static <T> Query<T, Long> count(Predicates<T> predicates) {
        return new Query<>(predicates.model(), context -> context.count(predicates), lazy(() ->
            String.format("count the %s%s",
                predicates.model().pluralName(),
                Strings.prefixIfNotNullOrEmpty(" where ", predicates.toString())
            )
        ));
    }

    public static <T> Query<T, T> create(T entity) {
        return create(entity, Includes.defaultPersistsOrMergesOf(EntityModel.of(entity))::addTo);
    }

    public static <T> Query<T, T> create(T entity, UnaryOperator<Includes<T>> includes) {
        return create(entity, includes.apply(Includes.of(EntityModel.of(entity))));
    }

    public static <T> Query<T, T> create(T entity, Includes<T> includes) {
        return new Query<>(EntityModel.of(entity), context -> context.create(entity, includes), lazy(() ->
            String.format(
                "create the %s -> %s",
                EntityModel.of(entity).singularName(),
                Json.write(entity)
            )
        ));
    }

    @SafeVarargs
    public static <T> Query<T, List<T>> create(T entity, T... entities) {
        return create(Lists.of(entity, entities));
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> create(CollectionType entities) {
        return create(entities, Includes.defaultPersistsOrMergesOf(EntityModel.of(entities))::addTo);
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> create(CollectionType entities, UnaryOperator<Includes<T>> includes) {
        return create(entities, includes.apply(Includes.of(EntityModel.of(entities))));
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> create(CollectionType entities, Includes<T> includes) {
        EntityModel<T> model = EntityModel.of(entities);
        return new Query<>(model, context -> context.create(entities, includes), lazy(() ->
            String.format("create the %s -> %s",
                model.pluralName(),
                Json.write(entities)
            )
        ));
    }

    public static <T> Query<T, T> update(T entity) {
        return update(entity, Includes.defaultPersistsOrMergesOf(EntityModel.of(entity))::addTo);
    }

    public static <T> Query<T, T> update(T entity, UnaryOperator<Includes<T>> includes) {
        return update(entity, includes.apply(of(EntityModel.of(entity))));
    }

    public static <T> Query<T, T> update(T entity, Includes<T> includes) {
        EntityModel<T> model = EntityModel.of(entity);
        return new Query<>(model, context -> context.update(entity, includes), lazy(() ->
            String.format("update the %s -> %s",
                model.singularName(),
                Json.write(entity)
            )
        ));
    }

    @SafeVarargs
    public static <T> Query<T, List<T>> update(T entity, T... entities) {
        return update(Lists.of(entity, entities));
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> update(CollectionType entities) {
        return update(entities, Includes.defaultPersistsOrMergesOf(EntityModel.of(entities))::addTo);
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> update(CollectionType entities, UnaryOperator<Includes<T>> includes) {
        return update(entities, includes.apply(of(EntityModel.of(entities))));
    }

    public static <T, CollectionType extends Collection<T>> Query<T, CollectionType> update(CollectionType entities, Includes<T> includes) {
        EntityModel<T> model = EntityModel.of(entities);
        return new Query<>(model, context -> context.update(entities, includes), lazy(() ->
            String.format("update the %s -> %s",
                model.pluralName(),
                Json.write(entities)
            )
        ));
    }

    public static <T> Query<T, Long> patch(Values<T> values, UnaryOperator<Predicates<T>> predicates) {
        return patch(values, predicates, UnaryOperator.identity());
    }

    public static <T> Query<T, Long> patch(Values<T> values, UnaryOperator<Predicates<T>> predicates, UnaryOperator<Pagination<T>> pagination) {
        return patch(values, predicates.apply(Predicates.of(values.model())), pagination.apply(Pagination.of(values.model())));
    }

    public static <T> Query<T, Long> patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return new Query<>(predicates.model(), context -> context.patch(values, predicates, pagination), lazy(() ->
            String.format("patch the %s%s%s with %s",
                predicates.model().pluralName(),
                Strings.prefixIfNotNullOrEmpty(" where ", predicates.toString()),
                Strings.prefixIfNotNullOrEmpty(" ", pagination.toString()),
                Json.write(Maps.map(values, Member::getName, v -> v))
            )
        ));
    }

    private enum Part {none, predicates, includes, pagination, payload, end}

    @SuppressWarnings("unchecked")
    public static <T, ReturnType> Query<T, ReturnType> parse(String queryAsString) {
        List<String> tokens = Splitter.on(' ').omitEmptyStrings().split(queryAsString).toList();
        String queryType = tokens.remove(0);
        String type;
        if (tokens.get(0).equals("all")) {
            tokens.remove(0);
        }

        if (Strings.equalsOneOf(tokens.get(0), "the", "this", "that", "those", "these")) {
            tokens.remove(0);
        }
        if (queryType.startsWith("fetch") && tokens.get(0).equals("first")) {
            tokens.remove(0);
        }

        type = tokens.remove(0);

        Class<T> clazz = Model.getClassBy(type);
        EntityModel<T> model = EntityModel.of(clazz);

        Predicates<T> predicates = Predicates.of(model);
        Pagination<T> pagination = Pagination.of(model);
        Includes<T> includes = Includes.of(model);

        String payload = null;

        List<Pair<Integer, Part>> parts = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("where")) {
                parts.add(Pair.of(i, Part.predicates));
            } else if (token.equals("including")) {
                parts.add(Pair.of(i, Part.includes));
            } else if ((token.equals("ordered") && tokens.get(i + 1).equals("by")) || token.equals("start") || token.equals("limit")) {
                parts.add(Pair.of(i, Part.pagination));
            } else if (Strings.equalsOneOf(token, "->", "with")) {
                parts.add(Pair.of(i, Part.payload));
            }
        }
        parts.add(Pair.of(tokens.size(), Part.end));

        if (parts.get(0).getKey() > 0) {
            throw new IllegalArgumentException("unexpected tokens: " + String.join(" ", tokens.subList(0, parts.get(0).key())));
        }

        for (int i = 0; i < parts.size() - 1; i++) {
            Pair<Integer, Part> currentPart = parts.get(i);
            Pair<Integer, Part> nextPart = parts.get(i + 1);
            String content = String.join(" ", tokens.subList(currentPart.key(), nextPart.key()));
            switch (currentPart.value()) {
                case predicates:
                    predicates.parse(content);
                    break;
                case pagination:
                    pagination.parse(content);
                    break;
                case includes:
                    includes.include(content);
                    break;
                case payload:
                    payload = content.replaceFirst("^(->|with) ", "");
                    break;
            }
        }

        switch (queryType) {
            case "count":
            case "counting":
                return (Query<T, ReturnType>) count(predicates);
            case "delete":
            case "deleting":
                if (includes.isEmpty()) {
                    Includes.defaultRemovesOrDeleteOf(model).addTo(includes);
                }
                if (predicates.isKeyOnly()) {
                    Object key = predicates.toKey();
                    if (key instanceof Collection) {
                        return (Query<T, ReturnType>) delete((Collection<?>) key, includes);
                    }
                    return (Query<T, ReturnType>) delete(key, includes);
                }
                return (Query<T, ReturnType>) delete(predicates, pagination, includes);
            case "create":
            case "creating":
                if (payload == null) {
                    throw new IllegalStateException("payload is required for a create");
                }
                if (includes.isEmpty()) {
                    Includes.defaultPersistsOrMergesOf(model).addTo(includes);
                }
                if (type.equals(model.singularName())) {
                    return (Query<T, ReturnType>) create(Json.read(payload, model.getType()), includes::addTo);
                }
                return (Query<T, ReturnType>) create((List<T>) Json.read(payload, Types.parameterized(List.class, model.getType())), includes::addTo);
            case "update":
            case "updating":
                if (payload == null) {
                    throw new IllegalStateException("payload is required for an update");
                }
                if (includes.isEmpty()) {
                    Includes.defaultPersistsOrMergesOf(model).addTo(includes);
                }
                if (type.equals(model.singularName())) {
                    return (Query<T, ReturnType>) update(Json.read(payload, model.getType()), includes::addTo);
                }
                return (Query<T, ReturnType>) update((List<T>) Json.read(payload, Types.parameterized(List.class, model.getType())), includes::addTo);
            case "patch":
            case "patching":
                return (Query<T, ReturnType>) patch(new Values<>(model).with(Json.read(payload, Map.class)), predicates, pagination);
            case "get":
            case "getting":
                if (includes.isEmpty()) {
                    Includes.defaultEagersOf(model).addTo(includes);
                }
                Object key = predicates.toKey();
                if (key instanceof Collection) {
                    return (Query<T, ReturnType>) get((Collection<?>) key, includes);
                }
                return (Query<T, ReturnType>) get(key, includes);
            case "fetch":
            case "fetching":
                if (includes.isEmpty()) {
                    Includes.defaultEagersOf(model).addTo(includes);
                }
                return (Query<T, ReturnType>) first(predicates, pagination, includes);
            case "list":
            case "listing":
                if (includes.isEmpty()) {
                    Includes.defaultEagersOf(model).addTo(includes);
                }
                return (Query<T, ReturnType>) list(predicates, pagination, includes);
            default:
                throw new IllegalArgumentException("unknown query type: " + queryType);
        }
    }
}
