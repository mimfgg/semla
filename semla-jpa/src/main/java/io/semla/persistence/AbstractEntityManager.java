package io.semla.persistence;

import io.semla.datasource.Datasource;
import io.semla.model.EntityModel;
import io.semla.persistence.annotations.StrictIndices;
import io.semla.query.Includes;
import io.semla.query.Query;
import io.semla.reflect.Methods;
import io.semla.reflect.TypeReference;
import io.semla.reflect.Types;
import io.semla.relation.InverseOneToOneRelation;
import io.semla.relation.JoinedRelation;
import io.semla.relation.OneToManyRelation;
import io.semla.relation.Relation;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.ImmutableMap;
import io.semla.util.Lists;
import io.semla.util.Maps;
import io.semla.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.query.Includes.*;

public abstract class AbstractEntityManager<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final Map<Class<? extends Annotation>, List<Consumer<T>>> listeners = new LinkedHashMap<>();
    protected final Datasource<T> datasource;
    protected final EntityManagerFactory entityManagerFactory;
    protected final boolean strictIndices;
    protected final Validator validator;

    protected AbstractEntityManager(Datasource<T> datasource, EntityManagerFactory entityManagerFactory) {
        this.datasource = datasource;
        this.entityManagerFactory = entityManagerFactory;
        strictIndices = datasource.model().getType().isAnnotationPresent(StrictIndices.class);
        validator = entityManagerFactory.injector().getInstance(Validator.class);
        Stream.of(PrePersist.class, PostPersist.class, PostLoad.class, PreUpdate.class, PostUpdate.class, PreRemove.class, PostRemove.class)
                .forEach(annotation -> {
                    if (model().getType().isAnnotationPresent(EntityListeners.class)) {
                        listeners.put(annotation, Stream.of((Class<?>[]) model().getType().getAnnotation(EntityListeners.class).value())
                                .map(entityManagerFactory.injector()::getInstance)
                                .flatMap(listener -> Methods.findAnnotatedWith(listener.getClass(), annotation)
                                        .map(methodInvocator -> (Consumer<T>) (e -> methodInvocator.invoke(listener, e))))
                                .collect(Collectors.toList()));
                    } else {
                        List<Consumer<T>> matchingListeners = Methods.findAnnotatedWith(model().getType(), annotation)
                                .map(methodInvocator -> (Consumer<T>) methodInvocator::invoke).collect(Collectors.toList());
                        if (!matchingListeners.isEmpty()) {
                            listeners.put(annotation, matchingListeners);
                        }
                    }
                });
    }

    protected PersistenceContext newContext() {
        return new PersistenceContext(entityManagerFactory);
    }

    public EntityModel<T> model() {
        return datasource.model();
    }

    public Optional<T> get(Object key) {
        return get(newContext(), key, defaultEagersOf(model()));
    }

    protected Optional<T> get(PersistenceContext context, Object key, Includes<T> includes) {
        return execute(() -> Query.get(key, includes), () ->
                datasource.get(key)
                        .map(context.entityContext()::remapOrCache)
                        .map(entity -> includes.fetchOn(entity, context))
                        .map(entity -> invokeListener(entity, PostLoad.class))
        );
    }

    public final Map<Object, T> get(Object key, Object... keys) {
        return get(Lists.of(key, keys));
    }

    public <K> Map<K, T> get(Collection<K> keys) {
        return get(newContext(), keys, defaultEagersOf(model()));
    }

    protected <K> Map<K, T> get(PersistenceContext context, Collection<K> keys, Includes<T> includes) {
        return execute(() -> Query.get(keys, includes), () -> {
            Map<K, T> entitiesByKey = datasource.get(keys).entrySet().stream()
                    .collect(Maps.collect(Map.Entry::getKey, e -> context.entityContext().remapOrCache(e.getValue())));
            if (!entitiesByKey.isEmpty()) {
                includes.fetchOn(entitiesByKey.values(), context);
                entitiesByKey.values().forEach(entity -> invokeListener(entity, PostLoad.class));
            }
            return ImmutableMap.copyOf(entitiesByKey);
        });
    }

    public T create(T entity) {
        return create(newContext(), entity, defaultPersistsOrMergesOf(model()));
    }

    protected T create(PersistenceContext context, T entity, Includes<T> includes) {
        return execute(() -> Query.create(entity, includes), () -> {
            prePersist(entity);
            datasource.create(entity);
            context.entityContext().remapOrCache(entity);
            includes.createOrUpdateOn(entity, context);
            invokeListener(entity, PostPersist.class);
            return entity;
        });
    }

    private void prePersist(T entity) {
        entityManagerFactory.injector().inject(entity);
        if (model().key().isGenerated()) {
            if (model().key().member().getType().equals(UUID.class)) {
                model().key().member().setOn(entity, entityManagerFactory.injector().getInstance(new TypeReference<Supplier<UUID>>() {}).get());
            }
        } else if (model().key().member().isDefaultOn(entity)) {
            throw new PersistenceException("entity has no primary key set:\n" + Yaml.write(entity));
        }
        model().version().ifPresent(version -> version.member().setOn(entity, 1));
        invokeListener(entity, PrePersist.class);
        validate(entity);
    }

    private void validate(T entity) {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(entity);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @SafeVarargs
    public final List<T> create(T first, T... rest) {
        return create(Lists.of(first, rest));
    }

    public List<T> create(Stream<T> stream) {
        return create(stream.collect(Collectors.toList()));
    }

    public <CollectionType extends Collection<T>> CollectionType create(CollectionType entities) {
        return create(newContext(), entities, defaultPersistsOrMergesOf(model()));
    }

    protected <CollectionType extends Collection<T>> CollectionType create(PersistenceContext context, CollectionType entities, Includes<T> includes) {
        return execute(() -> Query.create(entities, includes), () -> {
            entities.forEach(this::prePersist);
            datasource.create(entities);
            includes.createOrUpdateOn(entities, context);
            entities.forEach(entity -> invokeListener(entity, PostPersist.class));
            return entities;
        });
    }

    public T update(T entity) {
        return update(newContext(), entity, defaultPersistsOrMergesOf(model()));
    }

    protected T update(PersistenceContext context, T entity, Includes<T> includes) {
        return execute(() -> Query.update(entity, includes), () -> {
            invokeListener(entity, PreUpdate.class);
            validate(entity);
            datasource.update(entity);
            includes.createOrUpdateOn(entity, context);
            model().version().ifPresent(version -> version.member().setOn(entity, version.member().<Integer>getOn(entity) + 1));
            invokeListener(entity, PostUpdate.class);
            return entity;
        });
    }

    @SafeVarargs
    public final List<T> update(T first, T... rest) {
        return update(Lists.of(first, rest));
    }

    public List<T> update(Stream<T> stream) {
        return update(stream.collect(Collectors.toList()));
    }

    public <CollectionType extends Collection<T>> CollectionType update(CollectionType entities) {
        return update(newContext(), entities, defaultPersistsOrMergesOf(model()));
    }

    protected <CollectionType extends Collection<T>> CollectionType update(PersistenceContext context, CollectionType entities, Includes<T> includes) {
        return execute(() -> Query.update(entities, includes), () -> {
            entities.forEach(entity -> invokeListener(entity, PreUpdate.class));
            datasource.update(entities);
            includes.createOrUpdateOn(entities, context);
            model().version().ifPresent(version ->
                    entities.forEach(entity -> version.member().setOn(entity, version.member().<Integer>getOn(entity) + 1))
            );
            entities.forEach(entity -> invokeListener(entity, PostUpdate.class));
            return entities;
        });
    }

    public boolean delete(Object key) {
        return delete(newContext(), key, defaultRemovesOrDeleteOf(model()));
    }

    protected boolean delete(PersistenceContext context, Object key, Includes<T> includes) {
        return execute(() -> Query.delete(key, includes), () -> {
            model().relations().forEach(relation -> addDetachIfMissing(includes, relation));
            if (listeners.containsKey(PreRemove.class) || listeners.containsKey(PostRemove.class) || !includes.relations().isEmpty()) {
                T entity = get(context, key, Includes.of(model())).orElseThrow(() -> new EntityNotFoundException("entity not found for key " + key));
                entity = invokeListener(entity, PreRemove.class);
                includes.deleteOn(entity, context);
                boolean delete = datasource.delete(key);
                invokeListener(entity, PostRemove.class);
                return delete;
            } else {
                return datasource.delete(key);
            }
        });
    }

    protected <R> void addDetachIfMissing(Includes<T> includes, Relation<T, R> relation) {
        if (Types.isAssignableToOneOf(relation.getClass(), InverseOneToOneRelation.class, JoinedRelation.class, OneToManyRelation.class)
                && !includes.relations().containsKey(relation)) {
            includes.include(relation);
        }
    }

    public long delete(Object key, Object... keys) {
        return delete(Lists.of(key, keys));
    }

    public long delete(Collection<?> keys) {
        return delete(newContext(), keys, defaultRemovesOrDeleteOf(model()));
    }

    protected long delete(PersistenceContext context, Collection<?> keys, Includes<T> includes) {
        return execute(() -> Query.delete(keys, includes), () -> {
            if (!includes.relations().isEmpty()) {
                includes.deleteOn(get(context, keys, Includes.of(model())).values(), context);
            }
            return datasource.delete(keys);
        });
    }

    public long count() {
        return execute(() -> Query.count(model().getType()), datasource::count);
    }

    protected T invokeListener(T entity, Class<? extends Annotation> annotation) {
        List<Consumer<T>> entityListeners = listeners.get(annotation);
        if (entityListeners != null && !entityListeners.isEmpty()) {
            entityListeners.forEach(consumer -> consumer.accept(entity));
        }
        return entity;
    }

    protected <R> R execute(Supplier<Query<T, ?>> query, Supplier<R> supplier) {
        if (logger.isDebugEnabled()) {
            R result = null;
            Exception exception = null;
            StringBuilder trace = new StringBuilder("executing: " + query.get());
            long start = System.nanoTime();
            try {
                result = supplier.get();
                return result;
            } catch (Exception e) {
                exception = e;
                throw e;
            } finally {
                long elapsed = System.nanoTime() - start;
                trace.append(" took ").append(elapsed / 1_000_000.0).append("ms");
                if (exception != null) {
                    trace.append(" and threw a ").append(exception);
                    logger.error(trace.toString(), exception);
                } else {
                    trace.append(" and returned ").append(Strings.toString(result));
                    logger.debug(trace.toString());
                }
            }

        }
        return supplier.get();
    }
}
