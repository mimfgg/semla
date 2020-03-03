package io.semla.datasource;

import io.semla.model.EntityModel;

import java.time.Duration;
import java.util.Collection;

public abstract class EphemeralKeyValueDatasource<T> extends KeyValueDatasource<T> {

    public EphemeralKeyValueDatasource(EntityModel<T> model, String keyspace) {
        super(model, keyspace);
    }

    public abstract void set(T entity, Duration ttl);

    public abstract void set(Collection<T> entities, Duration ttl);

}
