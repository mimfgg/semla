package io.semla.config;

import io.semla.datasource.MemcachedDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.TypeName;
import io.semla.util.Lists;
import io.semla.util.Singleton;
import io.semla.util.Splitter;
import net.spy.memcached.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

import static io.semla.util.Unchecked.unchecked;

@TypeName("memcached")
public class MemcachedDatasourceConfiguration extends KeyspacedDatasourceConfiguration<MemcachedDatasourceConfiguration> {

    public static final int DEFAULT_PORT = 11211;

    private List<String> hosts = Lists.of("localhost");

    private Singleton<MemcachedClient> client = Singleton.lazy(() -> {
        ConnectionFactory cf = new ConnectionFactoryBuilder()
            .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
            .setDaemon(true)
            .setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT)
            .setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
            .setFailureMode(FailureMode.Cancel)
            .build();
        return unchecked(() -> new MemcachedClient(cf,
                hosts.stream().map(host -> {
                    if (host.matches(".*:[0-9]{1,5}")) {
                        return Splitter.on(':').split(host).map(splitted -> new InetSocketAddress(splitted.get(0), Integer.parseInt(splitted.get(1))));
                    } else {
                        return new InetSocketAddress(host, DEFAULT_PORT);
                    }
                }).collect(Collectors.toList())
            )
        );
    });

    @Serialize
    public List<String> hosts() {
        return hosts;
    }

    @Deserialize
    public MemcachedDatasourceConfiguration withHosts(String... hosts) {
        this.hosts = Lists.fromArray(hosts);
        return this;
    }

    @Override
    public <T> MemcachedDatasource<T> create(EntityModel<T> entityModel) {
        return new MemcachedDatasource<>(entityModel, client.get(), keyspace());
    }

    @Override
    public void close() {
        client().shutdown();
        client.reset();
    }

    public MemcachedClient client() {
        return client.get();
    }
}
