package io.semla.datasource;

import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.lang.NonNull;
import io.semla.config.MongoDBDatasourceConfiguration;
import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.query.Pagination;
import io.semla.query.Predicate;
import io.semla.query.Predicates;
import io.semla.query.Values;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.serialization.json.Json;
import io.semla.util.Arrays;
import io.semla.util.Pair;
import io.semla.util.Strings;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import javax.persistence.Embedded;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import static com.mongodb.client.model.Updates.*;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


public class MongoDBDatasource<T> extends Datasource<T> {

    public static String DEFAULT_SEQUENCE_TABLE = "sequences";

    private final MongoCollection<Document> collection;
    private final MongoCollection<Sequence> sequences;

    public MongoDBDatasource(EntityModel<T> model, MongoDatabase mongoDatabase) {
        super(model);
        this.collection = mongoDatabase.getCollection(model.tablename());
        model().indices().stream()
            .filter(index -> !index.isPrimary())
            .forEach(index -> {
                String[] fieldNames = index.columns().stream().map(column -> getFieldName(column.member())).toArray(String[]::new);
                IndexOptions indexOptions = new IndexOptions();
                if (index.isUnique()) {
                    indexOptions.unique(true);
                }
                this.collection.createIndex(Indexes.ascending(fieldNames), indexOptions);
            });
        if (model().key().isGenerated()) {
            CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            sequences = mongoDatabase.getCollection(DEFAULT_SEQUENCE_TABLE, Sequence.class).withCodecRegistry(pojoCodecRegistry);
            if (sequences.countDocuments(eq("_id", model().tablename())) == 0) {
                sequences.insertOne(Sequence.of(model().tablename()));
            }
        } else {
            sequences = null;
        }
    }

    @Override
    public Pair<MongoCollection<Document>, MongoCollection<Sequence>> raw() {
        return Pair.of(collection, sequences);
    }

    @Override
    public Optional<T> get(Object key) {
        return Optional.ofNullable(collection.find(eq(getFieldName(model().key().member()), serializeValue(model().key(), key))).first()).map(this::fromDocument);
    }

    @Override
    public <K> Map<K, T> get(Collection<K> keys) {
        Map<K, T> found = collection.find(in(getFieldName(model().key().member()), serializeKeys(keys)))
            .into(new ArrayList<>()).stream()
            .filter(Objects::nonNull)
            .map(this::fromDocument)
            .collect(Collectors.toMap(entity -> model().key().member().getOn(entity), Function.identity()));
        return keys.stream().collect(LinkedHashMap::new, (map, key) -> map.put(key, found.get(key)), LinkedHashMap::putAll);
    }

    @Override
    public void create(T entity) {
        setPrimaryKeyIfGenerated(entity);
        try {
            collection.insertOne(toDocument(entity));
        } catch (MongoWriteException e) {
            if (e.getCode() == 11000) {
                throw alreadyExists(model().key().member().getOn(entity));
            }
            throw e;
        }
    }

    private void setPrimaryKeyIfGenerated(T entity) {
        if (model().key().isGenerated()) {
            if (model().key().member().isAssignableTo(Long.class)) {
                model().key().member().setOn(entity, (long) getNextSequence().value);
            } else if (model().key().member().isAssignableTo(Integer.class)) {
                model().key().member().setOn(entity, getNextSequence().value);
            }
        }
    }

    private Sequence getNextSequence() {
        return sequences.findOneAndUpdate(eq("_id", model().tablename()), inc("value", 1));
    }

    @Override
    public void create(Collection<T> entities) {
        collection.insertMany(entities.stream()
            .peek(this::setPrimaryKeyIfGenerated)
            .map(this::toDocument)
            .collect(Collectors.toList()));
    }

    @Override
    public void update(T entity) {
        entity = EntityModel.copy(entity);
        Bson filter = matchingKeyOf(entity);
        if (model().version().isPresent()) {
            Member<T> version = model().version().get().member();
            filter = and(filter, eq(version.getName(), version.getOn(entity)));
            version.setOn(entity, version.<Integer>getOn(entity) + 1);
        }
        Document document = collection.findOneAndReplace(filter, toDocument(entity));
        if (document == null) {
            if (model().version().isPresent()) {
                throw new OptimisticLockException("while updating " + Strings.toString(entity));
            } else {
                throw notFound(model().key().member().getOn(entity));
            }
        }
    }

    @Override
    public void update(Collection<T> entities) {
        entities.forEach(this::update);
    }

    @Override
    public boolean delete(Object key) {
        return collection.deleteOne(eq(getFieldName(model().key().member()), serializeValue(model().key(), key))).getDeletedCount() > 0;
    }

    @Override
    public long delete(Collection<?> keys) {
        return collection.deleteMany(in(getFieldName(model().key().member()), serializeKeys(keys))).getDeletedCount();
    }

    private List<Object> serializeKeys(Collection<?> keys) {
        return keys.stream().map(key -> serializeValue(model().key(), key)).collect(Collectors.toList());
    }

    @Override
    public Optional<T> first(Predicates<T> predicates, Pagination<T> pagination) {
        return Optional.ofNullable(paginate(collection.find(toBson(predicates)), pagination).first()).map(this::fromDocument);
    }

    @Override
    public List<T> list(Predicates<T> predicates, Pagination<T> pagination) {
        return paginate(collection.find(toBson(predicates)), pagination).map(this::fromDocument).into(new ArrayList<>());
    }

    @Override
    public long patch(Values<T> values, Predicates<T> predicates, Pagination<T> pagination) {
        Bson update = toBson(values);
        if (pagination.isPaginated()) {
            // there is not pagination on updateMany, so we have to first query and then update;
            predicates.where(model().key().member().getName()).in(
                list(predicates, pagination).stream().map(model().key().member()::getOn).collect(Collectors.toList())
            );
        }
        return collection.updateMany(toBson(predicates), update).getModifiedCount();
    }

    @Override
    public long delete(Predicates<T> predicates, Pagination<T> pagination) {
        if (pagination.isPaginated()) {
            // there is not pagination on deleteMany, so we have to first query and then delete the keys;
            predicates.where(model().key().member().getName()).in(
                list(predicates, pagination).stream().map(model().key().member()::getOn).collect(Collectors.toList())
            );
        }
        return collection.deleteMany(toBson(predicates)).getDeletedCount();
    }

    @Override
    public long count(Predicates<T> predicates) {
        return collection.countDocuments(toBson(predicates));
    }

    @NonNull
    private T fromDocument(Document document) {
        T entity = model().newInstance();
        document.forEach((key, value) -> {
            Member<T> member = getAccessor(key);
            member.setOn(entity, deserializeValue(value, member));
        });
        return entity;
    }

    private Object deserializeValue(Object value, Member<T> member) {
        if (value != null) {
            if (EntityModel.isEntity(member.getType())) {
                value = EntityModel.referenceTo(member.getType(), value);
            } else if (member.annotation(Embedded.class).isPresent()) {
                if (member.isAnnotatedWithOneOf(Arrays.of(OneToMany.class, ManyToMany.class))) {
                    EntityModel<?> model = EntityModel.of(Types.typeArgumentOf(member.getGenericType()));
                    value = ((Collection<?>) value).stream().map(model::newInstanceFromKey).collect(Collectors.toCollection(Types.supplierOf(member.getGenericType())));
                }
            } else if (member.getType().equals(Optional.class)) {
                value = Optional.ofNullable(Strings.parse(String.valueOf(value), Types.typeArgumentOf(member.getGenericType())));
            } else {
                value = Strings.parse(String.valueOf(value), member.getType());
            }
        }
        return value;
    }

    private Document toDocument(T entity) {
        Document document = new Document();
        model().columns().forEach(column -> document.put(getFieldName(column.member()), serializeValue(column, column.member().getOn(entity))));
        return document;
    }

    private Object serializeValue(Column<T> column, Object value) {
        if (value != null) {
            if (EntityModel.isEntity(value)) {
                value = EntityModel.keyOf(value);
            } else if (value instanceof Collection && EntityModel.containsEntities((Collection<?>) value)) {
                value = ((Collection<?>) value).stream().map(EntityModel::keyOf).collect(Collectors.toList());
            } else if (Types.isEqualToOneOf(column.member().getType(), BigInteger.class, BigDecimal.class) ||
                !Types.isAssignableToOneOf(column.member().getType(), Number.class, Boolean.class, String.class)) {
                if (column.member().annotation(Embedded.class).isPresent()) {
                    value = Json.write(value);
                } else {
                    value = Strings.toString(value);
                }
            } else if (column.member().annotation(Embedded.class).isPresent()) {
                if (column.member().isAnnotatedWithOneOf(Arrays.of(OneToMany.class, ManyToMany.class))) {
                    value = ((Collection<?>) value).stream().map(EntityModel::keyOf).collect(Collectors.toList());
                }
                value = Json.write(value);
            }
        }
        return value;
    }

    private Bson matchingKeyOf(T entity) {
        return eq(getFieldName(model().key().member()), serializeValue(model().key(), model().key().member().getOn(entity)));
    }

    private Bson toBson(Values<T> values) {
        List<Bson> updates = values.entrySet().stream()
            .map(value -> set(value.getKey().getName(), serializeValue(model().getColumn(value.getKey()), value.getValue())))
            .collect(Collectors.toList());
        model().version().ifPresent(version -> updates.add(inc(version.member().getName(), 1)));
        return combine(updates.toArray(new Bson[0]));
    }

    private Member<T> getAccessor(String fieldName) {
        if (fieldName.equals("_id")) {
            return model().key().member();
        }
        return model().member(fieldName);
    }

    private String getFieldName(Member<T> member) {
        if (member.getName().equals(model().key().member().getName())) {
            return "_id";
        }
        return member.getName();
    }

    private Bson toBson(Predicates<T> predicates) {
        return predicates.entrySet().stream().map(predicate -> {
            Member<T> member = predicate.getKey();
            return predicate.getValue().entrySet().stream().map(operator -> {
                String fieldName = getFieldName(member);
                Object value = serializeValue(model().getColumn(member), operator.getValue());
                switch (operator.getKey()) {
                    case is:
                        return eq(fieldName, value);
                    case not:
                        return ne(fieldName, value);
                    case in:
                        return in(fieldName, (Collection<?>) value);
                    case notIn:
                        return nin(fieldName, (Collection<?>) value);
                    case greaterOrEquals:
                        return gte(fieldName, value);
                    case greaterThan:
                        return gt(fieldName, value);
                    case lessOrEquals:
                        return lte(fieldName, value);
                    case lessThan:
                        return lt(fieldName, value);
                    case like:
                        return regex(fieldName, String.valueOf(value).replaceAll("%", ".*"));
                    case notLike:
                        return not(regex(fieldName, String.valueOf(value).replaceAll("%", ".*")));
                    case contains:
                        return regex(fieldName, ".*" + value + ".*");
                    case doesNotContain:
                        return not(regex(fieldName, ".*" + value + ".*"));
                    case containedIn:
                    case notContainedIn:
                        logger.warn("running where function against collection {}!", model().pluralName());
                        return where("function() { return \"" + value + "\".indexOf(this." + fieldName + ") " +
                            (operator.getKey() == Predicate.containedIn ? ">" : "==") + " -1; }");
                    default:
                        // unreachable
                        throw new UnsupportedOperationException("unreachable");
                }
            }).reduce(new BsonDocument(), (current, filter) -> and(current, filter));
        }).reduce(new BsonDocument(), (current, filter) -> and(current, filter));
    }

    private FindIterable<Document> paginate(FindIterable<Document> query, Pagination<T> pagination) {
        if (pagination.start() > 0) {
            query.skip(pagination.start());
        }
        if (pagination.limit() != Integer.MAX_VALUE) {
            query.limit(pagination.limit());
        }
        if (pagination.isSorted()) {
            query.sort(orderBy(pagination.sort().entrySet().stream().map(sort -> {
                String fieldName = getFieldName(sort.getKey());
                if (sort.getValue() == null || sort.getValue() == Pagination.Sort.ASC) {
                    return ascending(fieldName);
                }
                return descending(fieldName);
            }).toArray(Bson[]::new)));
        }
        return query;
    }

    public static class Sequence {

        public String id;

        public int value;

        public static Sequence of(String tablename) {
            Sequence sequence = new Sequence();
            sequence.id = tablename;
            sequence.value = 1;
            return sequence;
        }
    }

    public static MongoDBDatasourceConfiguration configure() {
        return new MongoDBDatasourceConfiguration();
    }
}
