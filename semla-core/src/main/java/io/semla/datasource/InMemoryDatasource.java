package io.semla.datasource;

import io.semla.config.InMemoryDatasourceConfiguration;
import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.reflect.Types;
import io.semla.serialization.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.OptimisticLockException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryDatasource<T> extends Datasource<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Object, T> entities = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Consumer<T> primaryKeyGenerator;

    public InMemoryDatasource(EntityModel<T> model) {
        super(model);
        primaryKeyGenerator = createColumnGenerator(model.key());
    }

    @Override
    public Map<Object, T> raw() {
        return entities;
    }

    @Override
    public Optional<T> get(Object key) {
        return Optional.ofNullable(entities.get(key)).map(EntityModel::copy);
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<K, T> entitiesByKey = new LinkedHashMap<>();
        keys.forEach(key -> entitiesByKey.put(key, EntityModel.copy(entities.get(key))));
        return entitiesByKey;
    }

    @Override
    public void create(T entity) {
        primaryKeyGenerator.accept(entity);
        Object key = model().key().member().getOn(entity);
        get(key).ifPresent(current -> {
            throw alreadyExists(key);
        });
        entities.put(key, EntityModel.copy(entity));
    }

    @Override
    public void create(Collection<T> entities) {
        entities.forEach(this::create);
    }

    @Override
    public void update(T entity) {
        Object key = model().key().member().getOn(entity);
        if (!entities.containsKey(key)) {
            throw notFound(key);
        }
        T copy = EntityModel.copy(entity);
        if (model().version().isPresent()) {
            Column<T> version = model().version().get();
            T current = entities.get(key);
            int assumedVersion = version.member().<Integer>getOn(entity);
            if (version.member().<Integer>getOn(current) != assumedVersion) {
                throw new OptimisticLockException("when updating " + Json.write(entity));
            }
            version.member().setOn(copy, version.member().<Integer>getOn(copy) + 1);
        }
        entities.put(key, copy);
    }


    @Override
    public void update(Collection<T> entities) {
        entities.forEach(this::update);
    }

    @Override
    public boolean delete(Object key) {
        return entities.remove(key) != null;
    }

    @Override
    public long delete(Collection<?> keys) {
        return keys.stream().map(this::delete).map(r -> r ? 1L : 0).reduce(Long::sum).orElse(0L);
    }

    @Override
    public long count(Predicates<T> predicates) {
        return entities.values().stream().filter(predicates::matches).count();
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return stream(predicates, pagination).findFirst();
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        return stream(predicates, pagination).collect(Collectors.toList());
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return filter(predicates, pagination)
            .peek(entity -> {
                values.forEach((key, value) -> key.setOn(entity, value));
                model().version().ifPresent(version -> version.member().setOn(entity, version.member().<Integer>getOn(entity) + 1));
            })
            .count();
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        return filter(predicates, pagination)
            .collect(Collectors.toList()).stream()
            .map(entity -> entities.remove(model().key().member().getOn(entity)) != null ? 1 : 0)
            .reduce(0, Integer::sum);
    }

    private Stream<T> stream(Predicates<T> predicates, Pagination<T> pagination) {
        return filter(predicates, pagination).map(EntityModel::copy);
    }

    private Stream<T> filter(Predicates<T> predicates, Pagination<T> pagination) {
        return pagination.paginate(predicates.filter(entities.values()));
    }

    private Consumer<T> createColumnGenerator(Column<T> column) {
        if (column.isGenerated()) {
            if (Types.isAssignableTo(column.member().getType(), Integer.class)) {
                return new Consumer<T>() {
                    private final AtomicInteger id = new AtomicInteger();

                    @Override
                    public void accept(T entity) {
                        column.member().setOn(entity, id.incrementAndGet());
                    }
                };
            } else if (Types.isAssignableTo(column.member().getType(), Long.class)) {
                return new Consumer<T>() {
                    private final AtomicLong id = new AtomicLong();

                    @Override
                    public void accept(T entity) {
                        column.member().setOn(entity, id.incrementAndGet());
                    }
                };
            }
        }
        return entity -> {};
    }

    public static InMemoryDatasourceConfiguration configure() {
        return new InMemoryDatasourceConfiguration();
    }
}
