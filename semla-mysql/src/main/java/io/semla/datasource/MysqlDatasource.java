package io.semla.datasource;

import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.serialization.annotations.TypeName;
import org.jdbi.v3.core.Jdbi;

import javax.persistence.GeneratedValue;
import java.util.Map;

import static io.semla.query.Predicate.is;
import static io.semla.query.Predicate.not;
import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.isAssignableToOneOf;


public class MysqlDatasource<T> extends SqlDatasource<T> {

    public MysqlDatasource(EntityModel<T> entityModel, Jdbi dbi, String tablename) {
        super(entityModel, dbi, tablename);
    }

    @Override
    protected void extend() {
        ddl().escapeWith(name -> "`" + name + "`");
        if (model().key().member().annotation(GeneratedValue.class).isPresent()) {
            if (model().key().columnDefinition().isEmpty() && isAssignableToOneOf(model().key().member().getType(), Integer.class, Long.class)) {
                String columnDefinition = ddl().getColumnDefinition(model().key())
                    .orElseThrow(() -> new IllegalStateException("no definition for column " + model().key()));
                columnDefinition += " AUTO_INCREMENT NOT NULL";
                ddl().withColumnDefinition(model().key()::equals, columnDefinition);
            }
        }
        model().indices().forEach(index -> {
                if (!index.isPrimary()) {
                    ddl().withConstraint((index.isUnique() ? "UNIQUE " : "")
                        + "INDEX " + ddl().escape(index.name())
                        + " (" + index.columnNames(ddl().escape()) + ")");
                }
            }
        );

        ddl().withConstraint("PRIMARY KEY (" + ddl().escape(model().key().name()) + ")");

        withPredicateHandler(is, (s, c, p, o, v) -> {
            s.append(ddl().escape(c.name()));
            if (o == null) {
                s.append(" IS NULL");
            } else {
                if (isAssignableTo(o.getClass(), Float.class)) {
                    s.append(" LIKE ");
                } else {
                    s.append(" = ");
                }
                appendPlaceholder(s, c, o, v);
            }
        });

        withPredicateHandler(not, (s, c, p, o, v) -> {
            s.append(ddl().escape(c.name()));
            if (o == null) {
                s.append(" IS NOT NULL");
            } else {
                if (isAssignableTo(o.getClass(), Float.class)) {
                    s.append(" NOT LIKE ");
                } else {
                    s.append(" != ");
                }
                appendPlaceholder(s, c, o, v);
            }
        });
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        if (pagination.isSorted() || pagination.isPaginated()) {
            // Mysql doesn't support sorting or offset on deletes
            // we use a double nested query:
            // DELETE FROM `table` WHERE id IN (select id from (select id FROM `table` <predicates> <pagination>) x)
            return (long) raw().withHandle(handle -> {
                String key = ddl().escape(model().key().name());
                String table = ddl().escape(ddl().tablename());
                StringBuilder sql = new StringBuilder(
                    "DELETE FROM %s WHERE %s IN (SELECT %s FROM (SELECT %s FROM %s ".formatted(table, key, key, key, table));
                Map<String, Object> values = addPredicates(sql, predicates, pagination);
                sql.append(") x)");
                return query(handle::createUpdate, sql, values).execute();
            });
        }
        return super.delete(predicates, pagination);
    }

    public static MysqlDatasource.Configuration configure() {
        return new MysqlDatasource.Configuration();
    }

    @TypeName("mysql")
    public static class Configuration extends SqlDatasource.Configuration<MysqlDatasource.Configuration> {

        public Configuration() {
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

}
