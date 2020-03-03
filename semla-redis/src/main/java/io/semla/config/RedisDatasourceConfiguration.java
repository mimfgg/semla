package io.semla.config;

import io.semla.datasource.Datasource;
import io.semla.datasource.RedisDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Singleton;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@TypeName("redis")
public class RedisDatasourceConfiguration extends KeyspacedDatasourceConfiguration<RedisDatasourceConfiguration> {

    public static final int DEFAULT_PORT = 6379;
    private JedisPoolConfig config = new JedisPoolConfig();
    private String host = "localhost";
    private Integer port = DEFAULT_PORT;

    private Singleton<JedisPool> client = Singleton.lazy(() -> new JedisPool(config, host, port));

    public JedisPool client() {
        return client.get();
    }

    @Serialize
    public int minIdle() {
        return config.getMinIdle();
    }

    @Deserialize
    public RedisDatasourceConfiguration withMinIdle(int minIdle) {
        config.setMinIdle(minIdle);
        return this;
    }

    @Serialize
    public int maxIdle() {
        return config.getMaxIdle();
    }

    @Deserialize
    public RedisDatasourceConfiguration withMaxIdle(int maxIdle) {
        config.setMaxIdle(maxIdle);
        return this;
    }

    @Serialize
    public long maxWaitMillis() {
        return config.getMaxWaitMillis();
    }

    @Deserialize
    public RedisDatasourceConfiguration withMaxWaitMillis(long maxWaitMillis) {
        config.setMaxWaitMillis(maxWaitMillis);
        return this;
    }

    @Serialize
    public int maxTotal() {
        return config.getMaxTotal();
    }

    @Deserialize
    public RedisDatasourceConfiguration withMaxTotal(int maxTotal) {
        config.setMaxTotal(maxTotal);
        return this;
    }

    @Serialize
    public String host() {
        return host;
    }

    @Deserialize
    public RedisDatasourceConfiguration withHost(String host) {
        this.host = host;
        return this;
    }

    @Serialize
    public Integer port() {
        return port;
    }

    public JedisPoolConfig config() {
        return config;
    }

    @Deserialize
    public RedisDatasourceConfiguration withPort(Integer port) {
        this.port = port;
        return this;
    }

    @Override
    public <T> RedisDatasource<T> create(EntityModel<T> entityModel) {
        return new RedisDatasource<>(entityModel, client(), keyspace());
    }

    @Override
    public void close() {
        client().close();
        client.reset();
    }
}
