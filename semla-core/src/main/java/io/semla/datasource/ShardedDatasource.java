package io.semla.datasource;

import io.semla.config.ShardedDatasourceConfiguration;
import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.reflect.Types;
import io.semla.serialization.annotations.TypeInfo;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Maps;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShardedDatasource<T> extends Datasource<T> {

    protected final ShardingStrategy shardingStrategy;
    protected final List<Datasource<T>> datasources;
    protected final boolean rebalacing;

    public ShardedDatasource(EntityModel<T> model, ShardingStrategy shardingStrategy, boolean rebalacing, List<Datasource<T>> datasources) {
        super(model);
        this.shardingStrategy = shardingStrategy;
        this.rebalacing = rebalacing;
        this.datasources = datasources;
    }

    @Override
    public List<Datasource<T>> raw() {
        return datasources;
    }

    @Override
    public Optional<T> get(Object key) {
        Optional<T> entity = forKey(key).get(key);
        if (rebalacing && !entity.isPresent()) {
            AtomicReference<Datasource<T>> toPurge = new AtomicReference<>();
            entity = map(datasource -> {
                Optional<T> entityToRelocate = datasource.get(key);
                if (entityToRelocate.isPresent()) {
                    toPurge.set(datasource);
                }
                return entityToRelocate;
            })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
            if (entity.isPresent()) {
                create(entity.get());
                toPurge.get().delete(key);
            }
        }
        return entity;
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<K, T> entitiesByKey = map(keys, Datasource::get)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(Maps.collect());

        if (rebalacing) {
            List<K> misses = entitiesByKey.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            if (!misses.isEmpty()) {
                Map<Datasource<T>, Collection<K>> toPurge = new LinkedHashMap<>();
                List<T> toRelocate = new ArrayList<>();

                map(datasource -> {
                    Map<K, T> hits = datasource.get(misses).entrySet().stream().filter(e -> e.getValue() != null).collect(Maps.collect());
                    if (!hits.isEmpty()) {
                        toPurge.put(datasource, hits.keySet());
                        toRelocate.addAll(hits.values());
                    }
                    return hits;
                }).forEach(entitiesByKey::putAll);

                if (!toRelocate.isEmpty()) {
                    create(toRelocate);
                }

                if (!toPurge.isEmpty()) {
                    toPurge.forEach(Datasource::delete);
                }
            }
        }
        return entitiesByKey;
    }

    @Override
    public void create(T entity) {
        forEntity(entity).create(entity);
    }

    @Override
    public void create(Collection<T> entities) {
        foreach(entities, Datasource::create);
    }

    @Override
    public void update(T entity) {
        forEntity(entity).update(entity);
    }

    @Override
    public void update(Collection<T> entities) {
        foreach(entities, Datasource::update);
    }

    @Override
    public boolean delete(Object key) {
        return forKey(key).delete(key);
    }

    @Override
    public long delete(Collection<?> keys) {
        return map(keys, Datasource::delete).reduce(Long::sum).orElse(0L);
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return pagination.paginate(
            map(datasource -> {
                Optional<T> entity = datasource.first(predicates, pagination(pagination));
                if (rebalacing && entity.isPresent()) {
                    Datasource<T> correctDatasource = forEntity(entity.get());
                    if (!correctDatasource.equals(datasource)) {
                        correctDatasource.create(entity.get());
                        datasource.delete((Object) model().key().member().getOn(entity.get()));
                    }
                }
                return entity;
            }).filter(Optional::isPresent).map(Optional::get)
        ).findFirst();
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        Map<T, Datasource<T>> wronglyLocatedEntities = new LinkedHashMap<>();
        Map<Datasource<T>, Collection<T>> entitiesToRelocate = new LinkedHashMap<>();
        List<T> entities = pagination
            .paginate(
                map(datasource -> {
                    List<T> hits = datasource.list(predicates, pagination(pagination));
                    if (rebalacing) {
                        hits.forEach(entity -> {
                            Datasource<T> correctDatasource = forEntity(entity);
                            if (!correctDatasource.equals(datasource)) {
                                wronglyLocatedEntities.put(entity, datasource);
                                entitiesToRelocate.computeIfAbsent(correctDatasource, c -> new ArrayList<>()).add(entity);
                            }
                        });
                    }
                    return hits;
                }).flatMap(Collection::stream))
            .collect(Collectors.toList());

        if (!wronglyLocatedEntities.isEmpty()) {
            entitiesToRelocate.forEach(Datasource::create);
            wronglyLocatedEntities.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue))
                .forEach((datasource, entries) -> datasource.delete(entries.stream()
                    .map(Map.Entry::getKey)
                    .map(t -> model().key().member().getOn(t))
                    .collect(Collectors.toList())));
        }
        return entities;
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        if (!pagination.isPaginated()) {
            return map(datasource -> datasource.patch(values, predicates, pagination))
                .reduce(Long::sum)
                .orElse(0L);
        } else {
            List<T> entities = list(predicates, pagination).stream().map(values::apply).collect(Collectors.toList());
            update(entities);
            return entities.size();
        }
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        if (!pagination.isPaginated()) {
            return map(datasource -> datasource.delete(predicates, pagination))
                .reduce(Long::sum)
                .orElse(0L);
        } else {
            List<Object> keys = list(predicates, pagination).stream().map(model().key().member()::getOn).collect(Collectors.toList());
            return map(datasources -> datasources.delete(keys)).reduce(Long::sum)
                .orElse(0L);
        }
    }

    @Override
    public long count(Predicates<T> predicates) {
        return map(datasource -> datasource.count(predicates))
            .reduce(Long::sum)
            .orElse(0L);
    }


    protected <E> Stream<E> map(Function<Datasource<T>, E> streamFunction) {
        return datasources.parallelStream().map(streamFunction);
    }

    protected <K, E> Stream<E> map(Collection<K> keys, BiFunction<Datasource<T>, Collection<K>, E> streamFunction) {
        return keys.stream()
            .collect(Collectors.groupingBy(this::forKey))
            .entrySet()
            .parallelStream()
            .map(shard -> streamFunction.apply(shard.getKey(), shard.getValue()));
    }

    protected void foreach(Collection<T> entities, BiConsumer<Datasource<T>, Collection<T>> streamFunction) {
        entities.stream()
            .collect(Collectors.groupingBy(this::forEntity))
            .entrySet()
            .parallelStream()
            .forEach(shard -> streamFunction.accept(shard.getKey(), shard.getValue()));
    }

    protected Datasource<T> forEntity(T entity) {
        return shardingStrategy.selectFor(model().key().member().getOn(entity), datasources);
    }

    protected Datasource<T> forKey(Object key) {
        return shardingStrategy.selectFor(key, datasources);
    }

    protected Pagination<T> pagination(Pagination<T> pagination) {
        return pagination.copy()
            .startAt(Math.max(0, pagination.start() / datasources.size()))
            .limitTo(pagination.limit() < Integer.MAX_VALUE
                ? pagination.limit() * datasources.size()
                : Integer.MAX_VALUE
            );
    }

    @TypeInfo
    public interface ShardingStrategy {

        <T> Datasource<T> selectFor(Object key, List<Datasource<T>> datasources);

    }

    @TypeName("keyed")
    public static class KeyedShardingStrategy implements ShardingStrategy {

        @Override
        public <T> Datasource<T> selectFor(Object key, List<Datasource<T>> datasources) {
            if (key == null) {
                throw new IllegalArgumentException("Cannot shard on a generated key!");
            }
            if (Types.isAssignableTo(key.getClass(), Integer.class)) {
                return datasources.get(((int) key - 1) % datasources.size());
            }
            return selectFor((int) key.toString().charAt(0), datasources);
        }
    }

    public static ShardedDatasourceConfiguration configure() {
        return new ShardedDatasourceConfiguration();
    }
}
