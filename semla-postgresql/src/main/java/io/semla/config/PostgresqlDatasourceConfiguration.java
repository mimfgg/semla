package io.semla.config;

import io.semla.datasource.PostgresqlDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.TypeName;

@TypeName("postgresql")
public class PostgresqlDatasourceConfiguration extends SqlDatasourceConfiguration<PostgresqlDatasourceConfiguration> {

    public PostgresqlDatasourceConfiguration() {
        withConnectionTestQuery("SELECT 1");
    }

    @Override
    public <T> PostgresqlDatasource<T> create(EntityModel<T> model) {
        return (PostgresqlDatasource<T>) super.create(model);
    }

    @Override
    public <T> PostgresqlDatasource<T> create(EntityModel<T> model, String tablename) {
        return new PostgresqlDatasource<>(model, jdbi(), tablename);
    }
}
