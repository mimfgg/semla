package io.semla.persistence;

import io.semla.cache.Cache;
import io.semla.model.EntityModel;
import io.semla.model.InstanceContext;
import io.semla.query.*;
import io.semla.relation.IncludeType;
import io.semla.util.Lists;
import io.semla.util.Pair;
import io.semla.util.Strings;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PersistenceContext {

    private final EntityManagerFactory entityManagerFactory;
    private final RelationContext relationContext;
    private final InstanceContext instanceContext;
    private final CachingStrategy cachingStrategy;

    protected PersistenceContext(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
        this.relationContext = new RelationContext();
        this.instanceContext = new InstanceContext();
        this.cachingStrategy = new CachingStrategy();
    }

    @SuppressWarnings("unchecked")
    private <K, T> EntityManager<K, T> entityManagerOf(T entity) {
        return entityManagerFactory.of((Class<T>) entity.getClass());
    }

    public EntityManagerFactory factory() {
        return entityManagerFactory;
    }

    public RelationContext relationContext() {
        return relationContext;
    }

    public InstanceContext entityContext() {
        return instanceContext;
    }

    public CachingStrategy cachingStrategy() {
        return cachingStrategy;
    }

    public <T> Select<T> select(Class<T> clazz) {
        return select(EntityModel.of(clazz));
    }

    public <T> Select<T> select(EntityModel<T> model) {
        return new Select<>(this, model);
    }

    public <T> Create<T> newInstanceOf(Class<T> clazz) {
        return new Create<>(this, EntityModel.of(clazz));
    }

    public <T> Optional<T> get(Object key, Includes<T> includes) {
        return cachingStrategy.ifApplicable(() -> factory().injector().getInstance(Cache.class), () -> Query.get(key, includes).toString(),
            includes.model().getOptionalType(), () -> entityManagerFactory.of(includes.model().getType()).get(this, key, includes)
        );
    }

    public <K, T> Map<K, T> get(Collection<K> keys, Includes<T> includes) {
        return cachingStrategy.ifApplicable(
            () -> factory().injector().getInstance(Cache.class),
            () -> Query.get(keys, includes).toString(),
            includes.model().getMapType(),
            () -> entityManagerFactory.<K, T>of(includes.model().getType()).get(this, keys, includes)
        );
    }

    public <T> long count(Predicates<T> predicates) {
        return cachingStrategy.ifApplicable(() -> factory().injector().getInstance(Cache.class), () -> Query.count(predicates).toString(),
            long.class, () -> entityManagerFactory.of(predicates.model().getType()).count(predicates)
        );
    }

    public <K, T> boolean delete(K key, Includes<T> includes) {
        return entityManagerFactory.<K, T>of(includes.model().getType()).delete(this, key, includes);
    }

    public <T> long delete(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return entityManagerFactory.of(predicates.model().getType()).delete(this, predicates, pagination, includes);
    }

    public <K, T> long delete(Collection<K> keys, Includes<T> includes) {
        return entityManagerFactory.<K, T>of(includes.model().getType()).delete(this, keys, includes);
    }

    public <T> T update(T entity, Includes<T> includes) {
        return entityManagerOf(entity).update(this, entity, includes);
    }

    public <T, CollectionType extends Collection<T>> CollectionType update(CollectionType entities, Includes<T> includes) {
        return entityManagerFactory.of(includes.model().getType()).update(this, entities, includes);
    }

    public <T> long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return entityManagerFactory.of(predicates.model().getType()).patch(values, predicates, pagination);
    }

    public <T> T create(T entity, Includes<T> includes) {
        return entityManagerFactory.of(includes.model().getType()).create(this, entity, includes);
    }

    public <CollectionType extends Collection<T>, T> CollectionType create(CollectionType entities, Includes<T> includes) {
        return entityManagerFactory.of(includes.model().getType()).create(this, entities, includes);
    }

    public <T> Optional<T> first(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return cachingStrategy.ifApplicable(() -> factory().injector().getInstance(Cache.class), () -> Query.first(predicates, pagination, includes).toString(),
            includes.model().getOptionalType(), () -> entityManagerFactory.of(includes.model().getType()).first(this, predicates, pagination, includes)
        );
    }

    public <T> List<T> list(Predicates<T> predicates, Pagination<T> pagination, Includes<T> includes) {
        return cachingStrategy.ifApplicable(() -> factory().injector().getInstance(Cache.class), () -> Query.list(predicates, pagination, includes).toString(),
            includes.model().getListType(), () -> entityManagerFactory.of(includes.model().getType()).list(this, predicates, pagination, includes)
        );
    }

    public <ParentType, ChildType> void createOrUpdate(ChildType child, Include<ParentType, ChildType> include) {
        if (isPersisted(child)) {
            if (include.type().should(IncludeType.UPDATE)) {
                update(child, include.includes());
            }
        } else if (include.type().should(IncludeType.CREATE)) {
            create(child, include.includes());
        }
    }

    public <T> boolean isPersisted(T entity) {
        EntityModel<T> model = EntityModel.of(entity);
        if (!model.key().member().isDefaultOn(entity)) {
            return model.key().isGenerated() || get(model.key().member().<Object>getOn(entity), Includes.of(model)).isPresent();
        }
        return false;
    }

    public <ParentType, ChildType> void createOrUpdate(Collection<ChildType> children, Include<ParentType, ChildType> include) {
        sortPersisted(children)
            .ifLeft(persisted -> !persisted.isEmpty() && include.type().should(IncludeType.UPDATE)).then(persisted -> update(persisted, include.includes()))
            .ifRight(nonPersisted -> !nonPersisted.isEmpty() && include.type().should(IncludeType.CREATE)).then(nonPersisted -> create(nonPersisted, include.includes()));
    }

    public <T> Pair<Collection<T>, Collection<T>> sortPersisted(Collection<T> entities) {
        if (entities.isEmpty()) {
            return Pair.of(Lists.empty(), Lists.empty());
        }
        List<T> nonPersisted = new ArrayList<>();
        List<T> persisted = new ArrayList<>();
        Map<Object, T> keysToCheck = new LinkedHashMap<>();
        EntityModel<T> model = EntityModel.of(entities.iterator().next());
        entities.forEach(entity -> {
            if (!model.key().member().isDefaultOn(entity)) {
                if (model.key().isGenerated()) {
                    persisted.add(entity);
                } else {
                    keysToCheck.put(model.key().member().getOn(entity), entity);
                }
            } else {
                nonPersisted.add(entity);
            }
        });
        if (!keysToCheck.isEmpty()) {
            get(keysToCheck.keySet(), Includes.of(model)).forEach((key, value) -> {
                T entity = keysToCheck.get(key);
                if (value == null) {
                    nonPersisted.add(entity);
                } else {
                    persisted.add(entity);
                }
            });
        }
        return Pair.of(persisted, nonPersisted);
    }

    public <ParentType, ChildType> void fetchOn(ParentType parent, Include<ParentType, ChildType> include) {
        if (relationContext.shouldFetch(include.relation(), parent)) {
            if (log.isTraceEnabled()) {
                log.trace("fetching {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
            }
            include.relation().fetchOn(parent, this, include);
        } else if (log.isTraceEnabled()) {
            log.trace("skipping already fetched {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
        }
    }

    public <ParentType, ChildType> void fetchOn(Collection<ParentType> parents, Include<ParentType, ChildType> include) {
        if (!parents.isEmpty()) {
            List<ParentType> toFetch = parents.stream().filter(Objects::nonNull)
                .filter(parent -> relationContext().shouldFetch(include.relation(), parent)).collect(Collectors.toList());
            if (!toFetch.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.trace("fetching {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(toFetch));
                }
                include.relation().fetchOn(toFetch, this, include);
            } else if (log.isTraceEnabled()) {
                log.trace("skipping already fetched {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parents));
            }
        }
    }

    public <ParentType, ChildType> void createOrUpdateOn(ParentType parent, Include<ParentType, ChildType> include) {
        if (relationContext().shouldCreateOrUpdate(include.relation(), parent)) {
            if (log.isTraceEnabled()) {
                log.trace("creating or updating {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
            }
            include.relation().createOrUpdateOn(parent, this, include);
        } else if (log.isTraceEnabled()) {
            log.trace("skipping already created or updated {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
        }
    }

    public <ParentType, ChildType> void createOrUpdateOn(Collection<ParentType> parents, Include<ParentType, ChildType> include) {
        if (!parents.isEmpty()) {
            List<ParentType> toPersist = parents.stream()
                .filter(parent -> relationContext().shouldCreateOrUpdate(include.relation(), parent))
                .collect(Collectors.toList());
            if (!toPersist.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.trace("creating or updating {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(toPersist));
                }
                include.relation().createOrUpdateOn(toPersist, this, include);
            } else if (log.isTraceEnabled()) {
                log.trace("skipping already created or updated {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parents));
            }
        }
    }

    public <ParentType, ChildType> void deleteOn(ParentType parent, Include<ParentType, ChildType> include) {
        if (relationContext().shouldDelete(include.relation(), parent)) {
            if (log.isTraceEnabled()) {
                log.trace("removing {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
            }
            include.relation().deleteOn(parent, this, include);
        } else if (log.isTraceEnabled()) {
            log.trace("skipping already deleted {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parent));
        }
    }

    public <ParentType, ChildType> void deleteOn(Collection<ParentType> parents, Include<ParentType, ChildType> include) {
        if (!parents.isEmpty()) {
            List<ParentType> toCascade = parents.stream()
                .filter(parent -> relationContext().shouldDelete(include.relation(), parent))
                .collect(Collectors.toList());
            if (!toCascade.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.trace("removing {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(toCascade));
                }
                include.relation().deleteOn(toCascade, this, include);
            } else if (log.isTraceEnabled()) {
                log.trace("skipping already deleted {} {} on {}", include.relation().getClass().getSimpleName(), include.relation().member(), Strings.toString(parents));
            }
        }
    }
}
