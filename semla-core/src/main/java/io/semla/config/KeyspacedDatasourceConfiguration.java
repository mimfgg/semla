package io.semla.config;

import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;

public abstract class KeyspacedDatasourceConfiguration<SelfType extends KeyspacedDatasourceConfiguration<?>> implements DatasourceConfiguration {

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
