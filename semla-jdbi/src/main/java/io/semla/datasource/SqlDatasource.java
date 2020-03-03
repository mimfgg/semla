package io.semla.datasource;

import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicate;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.reflect.Member;
import io.semla.reflect.Methods;
import io.semla.serialization.json.Json;
import io.semla.util.Arrays;
import io.semla.util.ImmutableMap;
import io.semla.util.Lists;
import io.semla.util.Throwables;
import io.semla.util.function.PentaConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.semla.model.EntityModel.isEntity;
import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.typeArgumentOf;
import static io.semla.util.Unchecked.unchecked;


public abstract class SqlDatasource<T> extends Datasource<T> {

    private static Map<Class<?>, Throwables.BiFunction<ResultSet, String, ?>> PRIMITIVE_READERS =
        ImmutableMap.<Class<?>, Throwables.BiFunction<ResultSet, String, ?>>builder()
            .put(byte.class, ResultSet::getByte)
            .put(short.class, ResultSet::getShort)
            .put(int.class, ResultSet::getInt)
            .put(long.class, ResultSet::getLong)
            .put(float.class, ResultSet::getFloat)
            .put(double.class, ResultSet::getDouble)
            .put(boolean.class, ResultSet::getBoolean)
            .build();

    private static final int MAX_CHUNCK_SIZE = 1000;
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private final Map<Column<T>, Throwables.Function<ResultSet, ?>> mappers = new LinkedHashMap<>();
    private final Map<Column<T>, Function<Object, Object>> binders = new LinkedHashMap<>();
    private final Map<Predicate, PentaConsumer<StringBuilder, Column<T>, Predicate, Object, Map<String, Object>>> predicateHandlers = new EnumMap<>(Predicate.class);
    private final String[] generatedColumns;
    private final Jdbi dbi;
    private final SqlDDL<T> ddl;
    private Calendar defaultCalendar = Calendar.getInstance(TimeZone.getDefault());

    public SqlDatasource(EntityModel<T> entityModel, Jdbi dbi, String tablename) {
        super(entityModel);
        this.dbi = dbi;
        if (dbi != null && logger.isTraceEnabled()) {
            this.dbi.setSqlLogger(new SqlLogger() {
                @Override
                public void logAfterExecution(StatementContext context) {
                    logger.trace("[" + context.getRenderedSql() + "]" + context.getBinding() + " took " + context.getElapsedTime(ChronoUnit.MILLIS) + "ms");
                }
            });
        }
        this.ddl = new SqlDDL<>(entityModel, tablename);
        model().columns().forEach(column -> {
            Class<?> type = column.member().getType();
            if (type.isArray() && !isAssignableTo(type.getComponentType(), Byte.class)) {
                mappers.put(column, resultSet -> Json.read(resultSet.getString(column.name()), column.member().getGenericType()));
                binders.put(column, Json::write);
            } else if (column.member().annotation(Embedded.class).isPresent()) {
                mappers.put(column, resultSet -> Json.read(resultSet.getString(column.name()), column.member().getGenericType()));
                binders.put(column, value -> {
                    if (column.member().isAnnotatedWithOneOf(Arrays.of(OneToMany.class, ManyToMany.class))) {
                        value = ((Collection<?>) value).stream().map(EntityModel::keyOf).collect(Collectors.toList());
                    }
                    return Json.write(value);
                });
            } else if (isEntity(type)) {
                mappers.put(column, resultSet -> {
                        Class<?> keyType = EntityModel.of(type).key().member().getType();
                        Object keyValue;
                        if (keyType.isPrimitive()) {
                            keyValue = PRIMITIVE_READERS.get(keyType).apply(resultSet, column.name());
                        } else {
                            keyValue = resultSet.getObject(column.name(), keyType);
                        }
                        return keyValue != null ? EntityModel.referenceTo(type, keyValue) : null;
                    }
                );
                binders.put(column, EntityModel::keyOf);
            } else if (type.equals(Optional.class)) {
                mappers.put(column, resultSet -> Optional.of(resultSet.getObject(column.name(), typeArgumentOf(column.member().getGenericType()))));
                binders.put(column, value -> ((Optional<?>) value).orElse(null));
            } else if (type.equals(BigInteger.class)) {
                mappers.put(column, resultSet -> BigInteger.valueOf(resultSet.getLong(column.name())));
                binders.put(column, value -> ((BigInteger) value).longValue());
            } else if (type.equals(BigDecimal.class)) {
                mappers.put(column, resultSet -> resultSet.getBigDecimal(column.name()).stripTrailingZeros());
            } else if (isAssignableTo(type, Calendar.class)) {
                mappers.put(column, resultSet -> getTemporal(column, resultSet, time -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(time);
                    return calendar;
                }));
                binders.put(column, value -> new Date(((Calendar) value).getTimeInMillis()));
            } else if (isAssignableTo(type, Character.class)) {
                mappers.put(column, resultSet -> {
                    String value = resultSet.getString(column.name());
                    return value != null ? value.charAt(0) : null;
                });
            } else if (type.isPrimitive()) {
                mappers.put(column, resultSet -> PRIMITIVE_READERS.get(type).apply(resultSet, column.name()));
            } else if (type.equals(Date.class)) {
                mappers.put(column, resultSet -> getTemporal(column, resultSet, Date::new));
            } else if (type.equals(Instant.class)) {
                mappers.put(column, resultSet -> {
                    Date timestamp = resultSet.getTimestamp(column.name(), defaultCalendar);
                    return timestamp != null ? Instant.ofEpochMilli(timestamp.getTime()) : null;
                });
            } else if (type.isEnum()) {
                if (column.member().annotation(Enumerated.class).filter(enumerated -> enumerated.value() == EnumType.ORDINAL).isPresent()) {
                    mappers.put(column, resultSet -> type.getEnumConstants()[resultSet.getInt(column.name())]);
                    binders.put(column, value -> ((Enum) value).ordinal());
                } else {
                    mappers.put(column, resultSet -> Methods.invoke(type, "valueOf", resultSet.getString(column.name())));
                    binders.put(column, value -> ((Enum) value).name());
                }
            } else if (type.equals(UUID.class)) {
                mappers.put(column, resultSet -> Optional.of(resultSet.getString(column.name())).map(UUID::fromString).orElse(null));
                binders.put(column, String::valueOf);
            }
        });
        generatedColumns = model().columns().stream()
            .filter(column -> column.isGenerated() && !column.member().getType().equals(UUID.class))
            .map(Column::name).toArray(String[]::new);
        extend();
        if (dbi != null && dbi.getConfig(SemlaJdbiConfig.class).autoCreateTable) {
            try {
                dbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM " + ddl().escape(ddl().tablename())))
                    .mapTo(Long.class)
                    .findOne();
            } catch (Exception e) {
                dbi.withHandle(handle -> ddl().create().stream().map(command -> handle.execute(command)).reduce(Integer::sum));
            }
        }
    }

    protected abstract void extend();

    @Override
    public Jdbi raw() {
        return dbi;
    }

    public SqlDDL<T> ddl() {
        return ddl;
    }

    @Override
    public Optional<T> get(Object key) {
        return first(model().key().is(key), Pagination.of(model().getType()));
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        if (!keys.isEmpty()) {
            Map<K, T> entitiesByKey = new LinkedHashMap<>();
            keys.forEach(key -> entitiesByKey.put(key, null));
            list(model().key().in(keys), Pagination.of(model().getType()).limitTo(keys.size()))
                .forEach(entity -> entitiesByKey.put(model().key().member().getOn(entity), entity));
            return entitiesByKey;
        } else {
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void create(T entity) {
        dbi.withHandle(handle -> {
            if (!model().key().member().isDefaultOn(entity)) {
                Object key = model().key().member().getOn(entity);
                get(key).ifPresent(current -> {
                    throw alreadyExists(key);
                });
            }
            model().version().ifPresent(version -> version.member().setOn(entity, 1));
            Update update = bind(handle.createUpdate(ddl().insert()), model().columns().stream().filter(Column::insertable), entity);
            if (generatedColumns.length > 0) {
                update.executeAndReturnGeneratedKeys(generatedColumns).mapToMap().findFirst()
                    .ifPresent(generatedKeys -> assignGeneratedValues(entity, Lists.from(generatedKeys.values())));
            } else {
                int inserted = update.execute();
                if (inserted == 0) {
                    throw new PersistenceException("couldn't insert " + Json.write(entity));
                }
            }
            return true;
        });
    }

    @Override
    public void create(Collection<T> entities) {
        Lists.chunk(entities, MAX_CHUNCK_SIZE).forEach(chunck ->
            dbi.withHandle(handle -> {
                PreparedBatch preparedBatch = handle.prepareBatch(ddl().insert());
                chunck.forEach(entity -> bind(preparedBatch, model().columns().stream().filter(Column::insertable), entity).add());
                if (generatedColumns.length > 0) {
                    List<Map<String, Object>> generatedKeys = preparedBatch.executeAndReturnGeneratedKeys(generatedColumns).mapToMap().list();
                    IntStream.range(0, chunck.size()).forEach(i -> assignGeneratedValues(chunck.get(i), Lists.from(generatedKeys.get(i).values())));
                } else {
                    preparedBatch.execute();
                }
                return true;
            })
        );
    }

    @Override
    public void update(T entity) {
        dbi.withHandle(handle -> {
            Update update = bind(handle.createUpdate(ddl().update()), model().columns().stream().filter(Column::updatable), entity);
            Object key = binders.getOrDefault(model().key(), Function.identity()).apply(model().key().member().getOn(entity));
            update.bindByType(model().key().name(), key, key.getClass());
            model().version().ifPresent(version ->
                update.bindByType(version.name(), version.member().<Integer>getOn(entity), Integer.class)
            );
            long updated = update.execute();
            if (updated == 0) {
                if (model().version().isPresent()) {
                    throw new OptimisticLockException("while updating " + entity);
                } else {
                    throw notFound(key);
                }
            }
            return true;
        });
    }

    @Override
    public void update(Collection<T> entities) {
        Lists.chunk(entities, MAX_CHUNCK_SIZE).forEach(chunck ->
            dbi.withHandle(handle -> {
                PreparedBatch preparedBatch = handle.prepareBatch(ddl().update());
                chunck.forEach(entity -> {
                    Object key = binders.getOrDefault(model().key(), Function.identity()).apply(model().key().member().getOn(entity));
                    bind(preparedBatch, model().columns().stream().filter(Column::updatable), entity)
                        .bindByType(model().key().name(), key, key.getClass());
                    model().version().ifPresent(version ->
                        preparedBatch.bindByType(version.name(), version.member().<Integer>getOn(entity), Integer.class)
                    );
                    preparedBatch.add();
                });
                preparedBatch.execute();
                return true;
            })
        );
    }

    @Override
    public boolean delete(Object key) {
        return delete(model().key().is(key)) > 0;
    }

    @Override
    public long delete(Collection<?> keys) {
        if (keys.isEmpty()) {
            return 0L;
        }
        return delete(model().key().in(keys));
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return dbi.withHandle(handle ->
            query(handle::createQuery, new StringBuilder("SELECT * FROM " + ddl().escape(ddl().tablename())), predicates, pagination)
                .map(this::mapRow).findFirst()
        );
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        return dbi.withHandle(handle -> query(handle::createQuery, new StringBuilder("SELECT * FROM " + ddl().escape(ddl().tablename())), predicates, pagination)
            .map(this::mapRow).list());
    }


    protected <SqlStatementType extends SqlStatement<SqlStatementType>> SqlStatementType bind(SqlStatementType sqlStatement,
                                                                                              Stream<Column<T>> columns,
                                                                                              T entity) {
        columns.forEach(column -> {
            Object value = column.member().getOn(entity);
            if (value != null) {
                value = binders.getOrDefault(column, Function.identity()).apply(value);
            }
            Class<?> type = value != null ? value.getClass() : column.member().getType();
            sqlStatement.bindByType(column.name(), value, type);
        });
        return sqlStatement;
    }

    protected void assignGeneratedValues(T entity, List<Object> generatedValues) {
        // we cannot use the names, but can rely on the orders
        for (int j = 0; j < generatedValues.size(); j++) {
            Object generatedValue = generatedValues.get(j);
            String name = generatedColumns[j];
            Member<T> key = model().member(name);
            Class<?> keyType = key.getType();
            if (isAssignableTo(generatedValue.getClass(), Long.class) && isAssignableTo(keyType, Integer.class)) {
                generatedValue = ((Long) generatedValue).intValue();
            } else if (isAssignableTo(generatedValue.getClass(), Integer.class) && isAssignableTo(keyType, Long.class)) {
                generatedValue = ((Integer) generatedValue).longValue();
            } else if (generatedValue instanceof BigInteger) {
                if (isAssignableTo(keyType, Integer.class)) {
                    generatedValue = ((BigInteger) generatedValue).intValue();
                } else if (isAssignableTo(keyType, Long.class)) {
                    generatedValue = ((BigInteger) generatedValue).longValue();
                }
            }
            key.setOn(entity, generatedValue);
        }
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        return (long) dbi.withHandle(handle -> {
            StringBuilder sql = new StringBuilder("UPDATE " + ddl().escape(ddl().tablename()) + " SET ");
            values.keySet().stream()
                .map(member -> model().getColumn(member).name())
                .forEach(name -> sql.append(ddl().escape(name)).append(" = ").append(":").append(name).append(", "));
            sql.delete(sql.length() - 2, sql.length());
            model().version().ifPresent(version -> {
                String name = version.name();
                sql.append(", ").append(ddl().escape(name)).append(" = ").append(ddl().escape(name)).append(" + 1");
            });
            Update update = query(handle::createUpdate, sql, predicates, pagination);
            values.forEach((member, value) -> {
                Column<T> column = model().getColumn(member);
                update.bind(column.name(), binders.getOrDefault(column, Function.identity()).apply(value));
            });
            return update.execute();
        });
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        return (long) dbi.withHandle(handle -> query(handle::createUpdate, new StringBuilder("DELETE FROM " + ddl().escape(ddl().tablename())), predicates, pagination).execute());
    }

    @Override
    public long count(Predicates<T> predicates) {
        return dbi.withHandle(handle -> query(handle::createQuery, new StringBuilder("SELECT COUNT(*) FROM " + ddl().escape(ddl().tablename())), predicates,
            Pagination.of(model().getType()))
            .mapTo(Long.class).findOne().orElse(0L));
    }

    protected T mapRow(ResultSet resultSet, StatementContext ctx) {
        T entity = model().newInstance();
        model().columns().forEach(column -> {
                Object value = unchecked(() -> mappers.computeIfAbsent(column, c -> r -> r.getObject(c.name(), c.member().getType())).apply(resultSet));
                column.member().setOn(entity, value);
            }
        );
        return entity;
    }

    protected <SqlStatementType extends SqlStatement<SqlStatementType>> SqlStatementType query(Function<String, SqlStatementType> statementContructor,
                                                                                               StringBuilder sql, Predicates<T> predicates,
                                                                                               Pagination<T> pagination) {
        return query(statementContructor, sql, addPredicates(sql, predicates, pagination));
    }

    protected <SqlStatementType extends SqlStatement<SqlStatementType>> SqlStatementType query(Function<String, SqlStatementType> statementContructor,
                                                                                               StringBuilder sql,
                                                                                               Map<String, Object> values) {
        SqlStatementType query = statementContructor.apply(sql.toString());
        values.forEach((alias, value) -> query.bindByType(alias, value, value != null ? value.getClass() : Object.class));
        return query;
    }

    protected Map<String, Object> addPredicates(StringBuilder sql, Predicates<T> predicates, Pagination<T> pagination) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (!predicates.isEmpty()) {
            sql.append(" WHERE ");
            predicates.forEach((field, operators) -> {
                    Column<T> column = model().getColumn(field);
                    operators.forEach((predicate, value) -> {
                        appendPredicate(sql, column, predicate, value, values);
                        sql.append(" AND ");
                    });
                }
            );
            sql.delete(sql.length() - 5, sql.length());
        }

        if (pagination.isSorted()) {
            sql.append(" ORDER BY ");
            pagination.sort().forEach((member, sort) -> {
                sql.append(ddl().escape(model().getColumn(member).name()));
                if (sort != null) {
                    sql.append(" ").append(sort.name());
                }
                sql.append(", ");
            });
            sql.delete(sql.length() - 2, sql.length());
        }

        if (pagination.limit() < Integer.MAX_VALUE || pagination.start() > 0) {
            addPagination(sql, pagination);
        }
        return values;
    }

    protected void addPagination(StringBuilder sql, Pagination<T> pagination) {
        sql.append(" LIMIT ").append(pagination.limit());
        if (pagination.start() > 0) {
            sql.append(" OFFSET ").append(pagination.start());
        }
    }

    public void withPredicateHandler(Predicate predicate, PentaConsumer<StringBuilder, Column<T>, Predicate, Object, Map<String, Object>> consumer) {
        predicateHandlers.put(predicate, consumer);
    }

    private void appendPredicate(StringBuilder sql, Column<T> column, Predicate predicate, Object value, Map<String, Object> values) {
        predicateHandlers.computeIfAbsent(predicate, newPredicate -> {
            switch (newPredicate) {
                case notIn:
                case in:
                    return (s, c, p, o, v) -> {
                        s.append(ddl().escape(c.name())).append(" ");
                        if (p == Predicate.notIn) {
                            s.append("NOT ");
                        }
                        s.append("IN (");
                        Collection<?> collection = (Collection<?>) o;
                        if (!collection.isEmpty()) {
                            collection.forEach(element -> {
                                String columnName = c.name() + v.size();
                                s.append(':').append(columnName).append(", ");
                                v.put(columnName, binders.getOrDefault(c, Function.identity()).apply(element));
                            });
                            s.delete(s.length() - 2, s.length());
                        }
                        s.append(')');
                    };
                case is:
                    return (s, c, p, o, v) -> {
                        s.append(ddl().escape(c.name()));
                        if (o == null) {
                            s.append(" IS NULL");
                        } else {
                            appendPlaceholder(s.append(" = "), c, o, v);
                        }
                    };
                case not:
                    return (s, c, p, o, v) -> {
                        s.append(ddl().escape(c.name()));
                        if (o == null) {
                            s.append(" IS NOT NULL");
                        } else {
                            appendPlaceholder(s.append(" != "), c, o, v);
                        }
                    };
                case greaterOrEquals:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" >= "), c, o, v);
                case greaterThan:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" > "), c, o, v);
                case lessOrEquals:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" <= "), c, o, v);
                case lessThan:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" < "), c, o, v);
                case like:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" LIKE "), c, o, v);
                case contains:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" LIKE "), c, "%" + o + "%", v);
                case notLike:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" NOT LIKE "), c, o, v);
                case doesNotContain:
                    return (s, c, p, o, v) -> appendPlaceholder(s.append(ddl().escape(c.name())).append(" NOT LIKE "), c, "%" + o + "%", v);
                case containedIn:
                case notContainedIn:
                    return (s, c, p, o, v) -> {
                        appendPlaceholder(s, c, o, v);
                        if (newPredicate == Predicate.notContainedIn) {
                            s.append(" NOT");
                        }
                        s.append(" LIKE CONCAT('%', ").append(ddl().escape(c.name())).append(",'%')");
                    };
                default:
                    throw new UnsupportedOperationException(predicate.toString());
            }
        }).accept(sql, column, predicate, value, values);
    }

    protected void appendPlaceholder(StringBuilder sql, Column<T> column, Object value, Map<String, Object> values) {
        String placeholder = column.name() + values.size();
        values.put(placeholder, binders.getOrDefault(column, Function.identity()).apply(value));
        sql.append(':').append(placeholder);
    }

    private <DateType> DateType getTemporal(Column<T> column, ResultSet resultSet, Function<Long, DateType> mapper) throws SQLException {
        Date date = null;
        if (column.member().annotation(Temporal.class).isPresent()) {
            switch (column.member().annotation(Temporal.class).get().value()) {
                case TIME:
                    date = resultSet.getTime(column.name());
                    break;
                case DATE:
                    date = resultSet.getDate(column.name(), UTC);
                    break;
            }
        }
        if (date == null) {
            date = resultSet.getTimestamp(column.name(), defaultCalendar);
        }
        return date != null ? mapper.apply(date.getTime()) : null;
    }
}
