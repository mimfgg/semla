package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class MasterSlaveDatasource<T> extends Datasource<T> {

    private final Datasource<T> master;
    private final List<Datasource<T>> slaves;
    private final Random random = new Random();

    public MasterSlaveDatasource(EntityModel<T> model, Datasource<T> master, List<Datasource<T>> slaves) {
        super(model);
        this.master = master;
        this.slaves = slaves;
    }

    @Override
    public Pair<Datasource<T>, List<Datasource<T>>> raw() {
        return Pair.of(master, slaves);
    }

    @Override
    public Optional<T> get(Object key) {
        return slave().get(key);
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        return slave().get(keys);
    }

    @Override
    public void create(T entity) {
        master.create(entity);
    }

    @Override
    public void create(Collection<T> entities) {
        master.create(entities);
    }

    @Override
    public void update(T entity) {
        master.update(entity);
    }

    @Override
    public void update(Collection<T> entities) {
        master.update(entities);
    }

    @Override
    public boolean delete(Object key) {
        return master.delete(key);
    }

    @Override
    public long delete(Collection<?> keys) {
        return master.delete(keys);
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return slave().first(predicates, pagination);
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        return slave().list(predicates, pagination);
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return master.patch(values, predicates, pagination);
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        return master.delete(predicates, pagination);
    }

    @Override
    public long count(Predicates<T> predicates) {
        return slave().count(predicates);
    }

    private Datasource<T> slave() {
        return slaves.get(random.nextInt(slaves.size()));
    }

    public static MasterSlaveDatasource.Configuration configure() {
        return new MasterSlaveDatasource.Configuration();
    }

    @TypeName("master-slave")
    public static class Configuration implements Datasource.Configuration {

        private Datasource.Configuration master;
        private final List<Datasource.Configuration> slaves = new ArrayList<>();

        @Serialize
        public Datasource.Configuration master() {
            return master;
        }

        @Deserialize
        public MasterSlaveDatasource.Configuration withMaster(Datasource.Configuration master) {
            this.master = master;
            return this;
        }

        @Serialize
        public List<Datasource.Configuration> slaves() {
            return slaves;
        }

        @Deserialize
        public MasterSlaveDatasource.Configuration withSlaves(Datasource.Configuration... slaves) {
            this.slaves.addAll(Arrays.asList(slaves));
            return this;
        }

        @Override
        public <T> MasterSlaveDatasource<T> create(EntityModel<T> model) {
            return new MasterSlaveDatasource<>(model,
                    master.create(model),
                    slaves.stream().map(conf -> conf.create(model)).collect(Collectors.toList())
            );
        }

        @Override
        public void close() {
            master.close();
            slaves.forEach(Datasource.Configuration::close);
        }
    }
}
