package io.semla.config;

import com.mongodb.MongoClient;
import io.semla.datasource.MongoDBDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Singleton;

@TypeName("mongodb")
public class MongoDBDatasourceConfiguration implements DatasourceConfiguration {

    public static final int DEFAULT_PORT = 27017;
    private String host = "localhost";
    private Integer port = DEFAULT_PORT;
    private String database = "default";

    private Singleton<MongoClient> client = Singleton.lazy(() -> new MongoClient(host, port));

    public MongoClient client() {
        return client.get();
    }

    @Serialize
    public String host() {
        return host;
    }

    @Deserialize
    public MongoDBDatasourceConfiguration withHost(String host) {
        this.host = host;
        return this;
    }

    @Serialize
    public Integer port() {
        return port;
    }

    @Deserialize
    public MongoDBDatasourceConfiguration withPort(Integer port) {
        this.port = port;
        return this;
    }

    @Serialize
    public String database() {
        return database;
    }

    @Deserialize
    public MongoDBDatasourceConfiguration withDatabase(String database) {
        this.database = database;
        return this;
    }

    @Override
    public <T> MongoDBDatasource<T> create(EntityModel<T> model) {
        return new MongoDBDatasource<>(model, client().getDatabase(database));
    }

    @Override
    public void close() {
        client.get().close();
        client.reset();
    }
}
