package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.serialization.annotations.TypeName;
import org.jdbi.v3.core.Jdbi;

import javax.persistence.GeneratedValue;
import java.util.List;
import java.util.stream.Collectors;

import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.isAssignableToOneOf;


public class PostgresqlDatasource<T> extends SqlDatasource<T> {

    public PostgresqlDatasource(EntityModel<T> entityModel, Jdbi dbi, String tablename) {
        super(entityModel, dbi, tablename);
    }

    @Override
    protected void extend() {
        if (model().key().member().annotation(GeneratedValue.class).isPresent()) {
            if (model().key().columnDefinition().isEmpty() && isAssignableToOneOf(model().key().member().getType(), Integer.class, Long.class)) {
                ddl().withColumnDefinition(model().key()::equals, "SERIAL PRIMARY KEY");
            }
        }

        ddl()
            .withColumnDefinition(column -> isAssignableTo(column.member().getType(), Byte.class), "SMALLINT")
            .withColumnDefinition(column -> isAssignableTo(column.member().getType(), Double.class), "DOUBLE PRECISION")
            .withColumnDefinition(column -> isAssignableTo(column.member().getType(), Boolean.class), "BOOLEAN");

        model().indices().forEach(index -> {
                if (!index.isPrimary()) {
                    ddl().addCommand("CREATE " + (index.isUnique() ? "UNIQUE " : "")
                        + "INDEX " + ddl().escape(ddl().tablename() + "_" + index.name())
                        + " ON " + ddl().escape(ddl().tablename()) + " (" + index.columnNames(ddl().escape()) + ")");
                }
            }
        );
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        if (pagination.isPaginated()) {
            // LIMIT is not supported on updates, so we have to first query and then patch
            List<Object> keys = list(predicates, pagination).stream().map(model().key().member()::getOn).collect(Collectors.toList());
            predicates.where(model().key().member().getName()).in(keys);
            return super.patch(values, predicates); // no pagination
        }
        return super.patch(values, predicates, pagination);
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        if (pagination.isPaginated()) {
            // LIMIT is not supported on deletes, so we have to first query and then delete the keys
            return delete(list(predicates, pagination).stream().map(model().key().member()::getOn).collect(Collectors.toList()));
        }
        return super.delete(predicates, pagination);
    }

    public static PostgresqlDatasource.Configuration configure() {
        return new PostgresqlDatasource.Configuration();
    }

    @TypeName("postgresql")
    public static class Configuration extends SqlDatasource.Configuration<PostgresqlDatasource.Configuration> {

        public Configuration() {
            withDriverClassName("org.postgresql.Driver");
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

}
