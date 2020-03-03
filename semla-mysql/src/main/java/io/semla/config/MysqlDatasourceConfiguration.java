package io.semla.config;

import io.semla.datasource.MysqlDatasource;
import io.semla.model.EntityModel;
import io.semla.serialization.annotations.TypeName;

@TypeName("mysql")
public class MysqlDatasourceConfiguration extends SqlDatasourceConfiguration<MysqlDatasourceConfiguration> {

    public MysqlDatasourceConfiguration() {
        withConnectionTestQuery("SELECT 1");
    }

    @Override
    public <T> MysqlDatasource<T> create(EntityModel<T> model) {
        return (MysqlDatasource<T>) super.create(model);
    }

    @Override
    public <T> MysqlDatasource<T> create(EntityModel<T> model, String tablename) {
        return new MysqlDatasource<>(model, jdbi(), tablename);
    }
}
