package io.semla.datasource;

import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.util.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.semla.model.EntityModel.isEntity;
import static io.semla.reflect.Types.*;
import static io.semla.util.Singleton.lazy;
import static java.lang.System.lineSeparator;


public class SqlDDL<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<Column<T>, String> columnDefinitions = new LinkedHashMap<>();
    private final List<String> constraints = new ArrayList<>();
    private final EntityModel<T> model;
    private final String tablename;
    private final Singleton<String> insert;
    private final Singleton<String> update;
    private final List<String> commands = new ArrayList<>();
    private Function<String, String> escape = name -> "\"" + name + "\"";

    public SqlDDL(EntityModel<T> model, String tablename) {
        this.model = model;
        this.tablename = tablename;
        this.insert = lazy(this::generateInsertQuery);
        this.update = lazy(this::generateUpdateQuery);
        model.columns().forEach(column ->
            getColumnDefinition(column).ifPresent(columnDefinition -> columnDefinitions.put(column, columnDefinition))
        );
    }

    public SqlDDL<T> escapeWith(Function<String, String> escape) {
        this.escape = escape;
        Singleton.resetAll(insert, update);
        return this;
    }

    protected Function<String, String> escape() {
        return escape;
    }

    public String escape(String name) {
        return escape.apply(name);
    }

    public String tablename() {
        return tablename;
    }

    public String insert() {
        return insert.get();
    }

    protected String generateInsertQuery() {
        StringBuilder insert = new StringBuilder("INSERT INTO " + escape(tablename));
        insert.append(" (");
        model.columns().stream()
            .filter(Column::insertable)
            .forEach(column -> insert.append(escape(column.name())).append(", "));
        model.version().ifPresent(version -> insert.append(escape(version.name())).append(", "));
        insert.delete(insert.length() - 2, insert.length());
        insert.append(") VALUES (");
        model.columns().stream()
            .filter(Column::insertable)
            .forEach(column -> insert.append(":").append(column.name()).append(", "));
        model.version().ifPresent(version -> insert.append("1, "));
        insert.delete(insert.length() - 2, insert.length());
        insert.append(")");
        return insert.toString();
    }

    public String update() {
        return update.get();
    }

    protected String generateUpdateQuery() {
        StringBuilder update = new StringBuilder("UPDATE " + escape(tablename) + " SET ");
        model.columns().stream()
            .filter(Column::updatable)
            .forEach(column -> update.append(escape(column.name())).append(" = ").append(":").append(column.name()).append(", "));
        update.delete(update.length() - 2, update.length());
        model.version().ifPresent(version -> update.append(", ").append(escape(version.name())).append(" = ").append(escape(version.name())).append(" + 1"));
        update.append(" WHERE ").append(escape(model.key().name())).append(" = :").append(model.key().name());
        model.version().ifPresent(version -> update.append(" AND ").append(escape(version.name())).append(" = :").append(version.name()));
        return update.toString();
    }


    public List<String> create() {
        StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(escape(tablename)).append(" (").append(lineSeparator());

        model.columns().forEach(column ->
            Optional.ofNullable(columnDefinitions.get(column)).ifPresent(type -> {
                ddl.append("  ").append(escape(column.name())).append(" ").append(type);
                if (!column.nullable()) {
                    ddl.append(" NOT NULL");
                }
                ddl.append(",").append(lineSeparator());
            })
        );

        constraints.forEach(constraint -> ddl.append("  ").append(constraint).append(",").append(lineSeparator()));

        for (int i = ddl.length() - 1; i > 0; i--) {
            char c = ddl.charAt(i);
            if (!Character.isWhitespace(c)) {
                if (c == ',') {
                    ddl.deleteCharAt(i);
                }
                break;
            }
        }

        ddl.append(");");

        return Stream.concat(Stream.of(ddl.toString()), commands.stream().map(command -> "\n" + command + ";")).collect(Collectors.toList());
    }

    public SqlDDL<T> withColumnDefinition(Predicate<Column<T>> columnPredicate, String sqlType) {
        model.columns().stream().filter(columnPredicate).forEach(column -> columnDefinitions.put(column, sqlType));
        return this;
    }

    public SqlDDL<T> withConstraint(String constraint) {
        constraints.add(constraint);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected Optional<String> getColumnDefinition(Column<T> column) {
        return Optional.ofNullable(column.columnDefinition().orElseGet(() -> {
            Class<?> type = column.member().getType();
            if (type.equals(Optional.class)) {
                type = rawTypeArgumentOf(column.member().getGenericType());
            }
            if (isAssignableTo(type, Byte.class)) {
                return "TINYINT";
            } else if (isAssignableTo(type, Boolean.class)) {
                return "BIT(1)";
            } else if (isAssignableTo(type, Character.class)) {
                return "CHAR";
            } else if (isAssignableTo(type, Short.class)) {
                return "SMALLINT";
            } else if (isAssignableTo(type, Integer.class)) {
                return "INT" + (column.scale() != 0 ? "(" + column.scale() + ")" : "");
            } else if (isAssignableTo(type, Float.class)) {
                return "FLOAT";
            } else if (isAssignableTo(type, Long.class)) {
                return "BIGINT";
            } else if (isAssignableTo(type, Double.class)) {
                return "DOUBLE";
            } else if (type.equals(BigDecimal.class)) {
                return "DECIMAL(" + (column.scale() != 0 ? column.scale() : 65) + "," + (column.precision() != 0 ? column.precision() : 30) + ")";
            } else if (type.equals(BigInteger.class)) {
                return "BIGINT";
            } else if (type.equals(String.class)) {
                return "VARCHAR(" + column.length() + ")";
            } else if (column.member().annotation(Embedded.class).isPresent()) {
                return "TEXT";
            } else if (type.isArray()) {
                if (isAssignableTo(type.getComponentType(), Byte.class)) {
                    return "BINARY";
                } else {
                    return "VARCHAR(255)";
                }
            } else if (isEntity(type)) {
                return getColumnDefinition(EntityModel.of((Class<T>) type).key()).orElseThrow(IllegalStateException::new);
            } else if (type.isEnum()) {
                return column.member().annotation(Enumerated.class)
                    .filter(enumerated -> enumerated.value() == EnumType.ORDINAL).map(enumerated -> "INT")
                    .orElse("VARCHAR(255)");
            } else if (type.equals(java.sql.Date.class)) {
                return "DATE";
            } else if (type.equals(java.sql.Time.class)) {
                return "TIME";
            } else if (type.equals(java.sql.Timestamp.class)) {
                return "TIMESTAMP(3)";
            } else if (isAssignableToOneOf(type, Date.class, Calendar.class)) {
                return column.member().annotation(Temporal.class)
                    .filter(temporal -> temporal.value() != TemporalType.TIMESTAMP)
                    .map(temporal -> temporal.value().name())
                    .orElse("TIMESTAMP(3)");
            } else if (type.equals(Duration.class)) {
                return "BIGINT";
            } else if (type.equals(Instant.class)) {
                return "TIMESTAMP(3)";
            } else if (type.equals(LocalDateTime.class)) {
                return "TIMESTAMP(3)";
            } else if (type.equals(LocalDate.class)) {
                return "DATE";
            } else if (type.equals(LocalTime.class)) {
                return "TIME";
            } else if (type.equals(OffsetDateTime.class)) {
                return "TIMESTAMP(3)";
            } else if (type.equals(OffsetTime.class)) {
                return "TIME";
            } else if (type.equals(ZonedDateTime.class)) {
                return "TIMESTAMP(3)";
            } else if (type.equals(UUID.class)) {
                return "VARCHAR(36)"; //"BINARY(16)";
            }
            logger.warn("skipping column " + column);
            return null;
        }));
    }

    public SqlDDL<T> addCommand(String command) {
        commands.add(command);
        return this;
    }
}
