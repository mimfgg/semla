package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.reflect.Properties;
import io.semla.reflect.Setter;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.semla.util.Strings.emptyIfNull;

public abstract class KeyValueDatasource<T> extends Datasource<T> {

    private final String prefix;

    public KeyValueDatasource(EntityModel<T> model, String keyspace) {
        super(model);
        this.prefix = (keyspace != null ? keyspace + ":" : "") + model.tablename() + ":";
    }

    @Override
    public final Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final long delete(Predicates<T> predicates, Pagination<T> pagination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final long count(Predicates<T> predicates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void update(T entity) {
        create(entity);
    }

    @Override
    public final void update(Collection<T> entities) {
        create(entities);
    }

    protected void generateKeyIfDefault(T entity) {
        if (model().key().member().isDefaultOn(entity)) {
            if (model().key().member().isAssignableTo(Integer.class)) {
                model().key().member().setOn(entity, getNextAutoIncrementedPK());
            } else if (model().key().member().isAssignableTo(Long.class)) {
                model().key().member().setOn(entity, (long) getNextAutoIncrementedPK());
            }
        }
    }

    protected abstract Integer getNextAutoIncrementedPK();

    protected String prefix(Object key) {
        return emptyIfNull(prefix) + model().getType().getCanonicalName() + "::" + key;
    }

    protected String prefixedKeyOf(T entity) {
        return prefix(model().key().member().getOn(entity));
    }


    public abstract static class Configuration<SelfType extends io.semla.datasource.KeyValueDatasource.Configuration<?>> implements Datasource.Configuration {

        private String keyspace;

        @Serialize
        public String keyspace() {
            return keyspace;
        }

        @Deserialize
        @SuppressWarnings("unchecked")
        public SelfType withKeyspace(String keyspace) {
            this.keyspace = keyspace;
            return (SelfType) this;
        }
    }
}
