package io.semla.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.semla.datasource.SemlaJdbiConfig;
import io.semla.datasource.SqlDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;
import io.semla.serialization.annotations.When;
import io.semla.util.Singleton;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlLogger;

import java.time.Duration;

@SuppressWarnings("unchecked")
public abstract class SqlDatasourceConfiguration<SelfType extends SqlDatasourceConfiguration<?>> implements DatasourceConfiguration {

    private boolean autoCreateTable;
    private HikariConfig hikariConfig = new HikariConfig();
    private Singleton<Jdbi> jdbi = Singleton.lazy(this::createJdbi);

    @Serialize(When.NOT_DEFAULT)
    public boolean autoCreateTable() {
        return autoCreateTable;
    }

    @SuppressWarnings("unchecked")
    public SelfType withAutoCreateTable(boolean autoCreateTable) {
        this.autoCreateTable = autoCreateTable;
        return (SelfType) this;
    }

    @Serialize
    public String driverClassName() {
        return hikariConfig.getDriverClassName();
    }

    @Deserialize
    public SelfType withDriverClassName(String driver) {
        hikariConfig.setDriverClassName(driver);
        return (SelfType) this;
    }

    @Serialize
    public String username() {
        return hikariConfig.getUsername();
    }

    @Deserialize
    public SelfType withUsername(String username) {
        hikariConfig.setUsername(username);
        return (SelfType) this;
    }

    @Serialize
    public String password() {
        return hikariConfig.getPassword();
    }

    @Deserialize
    public SelfType withPassword(String password) {
        hikariConfig.setPassword(password);
        return (SelfType) this;
    }

    @Serialize
    public String jdbcUrl() {
        return hikariConfig.getJdbcUrl();
    }

    @Deserialize
    public SelfType withJdbcUrl(String jdbcUrl) {
        hikariConfig.setJdbcUrl(jdbcUrl);
        return (SelfType) this;
    }

    @Serialize
    public String connectionTestQuery() {
        return hikariConfig.getConnectionTestQuery();
    }

    @Deserialize
    public SelfType withConnectionTestQuery(String connectionTestQuery) {
        hikariConfig.setConnectionTestQuery(connectionTestQuery);
        return (SelfType) this;
    }

    @Serialize
    public int maximumPoolSize() {
        return hikariConfig.getMaximumPoolSize();
    }

    @Deserialize
    public SelfType withMaximumPoolSize(int maximumPoolSize) {
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        return (SelfType) this;
    }

    @Serialize
    public Duration idleTimeout() {
        return Duration.ofMillis(hikariConfig.getIdleTimeout());
    }

    @Deserialize
    public SelfType withIdleTimeout(Duration idleTimeout) {
        hikariConfig.setIdleTimeout(idleTimeout.toMillis());
        return (SelfType) this;
    }

    public HikariConfig hikariConfig() {
        return hikariConfig;
    }

    public Jdbi jdbi() {
        return jdbi.get();
    }

    protected Jdbi createJdbi() {
        Jdbi jdbi = Jdbi.create(new HikariDataSource(hikariConfig));
        jdbi.getConfig().get(SemlaJdbiConfig.class).autoCreateTable = autoCreateTable;
        return jdbi;
    }

    @Override
    public <T> SqlDatasource<T> create(EntityModel<T> model) {
        return create(model, model.tablename());
    }

    @Override
    public void close() {
        jdbi.get().useHandle(Handle::close);
        jdbi.reset();
    }

    public abstract <T> SqlDatasource<T> create(EntityModel<T> model, String tablename);

}
