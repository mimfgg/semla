package io.semla.config;

import io.semla.datasource.HsqlDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.TypeName;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;

@TypeName("hsql")
public class HsqlDatasourceConfiguration extends SqlDatasourceConfiguration<HsqlDatasourceConfiguration> {

    public HsqlDatasourceConfiguration() {
        withDriverClassName("org.hsqldb.jdbcDriver");
        withUsername("SA");
        withPassword("");
        withConnectionTestQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        withMaximumPoolSize(1);
    }

    @Override
    public <T> HsqlDatasource<T> create(EntityModel<T> model) {
        return (HsqlDatasource<T>) super.create(model);
    }

    public <T> HsqlDatasource<T> create(EntityModel<T> model, String tablename) {
        return new HsqlDatasource<>(model, jdbi(), tablename);
    }

    @Override
    protected Jdbi createJdbi() {
        return super.createJdbi().installPlugin(new H2DatabasePlugin());
    }
}
