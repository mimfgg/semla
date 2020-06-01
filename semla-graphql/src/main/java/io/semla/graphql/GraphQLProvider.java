package io.semla.graphql;

import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;
import io.semla.datasource.Datasource;
import io.semla.datasource.DatasourceFactory;
import io.semla.inject.Injector;
import io.semla.model.Column;
import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.persistence.EntityManager;
import io.semla.persistence.EntityManagerFactory;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Select;
import io.semla.reflect.Types;
import io.semla.relation.Relation;
import io.semla.util.Arrays;
import io.semla.util.Lists;
import io.semla.util.Plural;
import io.semla.util.Strings;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.persistence.CascadeType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static io.semla.inject.GraphQLModule.*;
import static io.semla.util.Strings.capitalize;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;

public class GraphQLProvider implements Provider<GraphQL> {

    private final Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalQueries = new LinkedHashMap<>();
    private final Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalMutations = new LinkedHashMap<>();
    private final List<String> additionalTypes = new ArrayList<>();

    protected final EntityManagerFactory factory;

    private GraphQL graphQL;
    private int modelCounts;

    @Inject
    public GraphQLProvider(EntityManagerFactory factory,
                           @Named(GRAPHQL_ADDITIONAL_QUERIES) Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalQueries,
                           @Named(GRAPHQL_ADDITIONAL_MUTATIONS) Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalMutations,
                           @Named(GRAPHQL_ADDITIONAL_TYPES) List<String> additionalTypes) {
        this.factory = factory;
        this.additionalQueries.putAll(additionalQueries);
        this.additionalMutations.putAll(additionalMutations);
        this.additionalTypes.addAll(additionalTypes);
    }

    private List<EntityModel<?>> getCurrentEntityModels() {
        return factory
            .injector()
            .getInstance(DatasourceFactory.class)
            .datasources()
            .stream()
            .map(Datasource::model)
            .sorted(Comparator.comparing(Model::singularName))
            .collect(Collectors.toList());
    }

    @Override
    public synchronized GraphQL get() {
        List<EntityModel<?>> models = getCurrentEntityModels();

        if (graphQL == null || models.size() > modelCounts) {
            modelCounts = models.size();
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(getSchema(models));
            GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, buildWiring(models));
            graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        }
        return graphQL;
    }

    public GraphQLProvider addQuery(String query, BiFunction<Injector, DataFetchingEnvironment, ?> handler) {
        additionalQueries.put(query, handler);
        graphQL = null;
        return this;
    }

    public GraphQLProvider addMutation(String query, BiFunction<Injector, DataFetchingEnvironment, ?> handler) {
        additionalMutations.put(query, handler);
        graphQL = null;
        return this;
    }

    public GraphQLProvider addTypes(String... types) {
        additionalTypes.addAll(Lists.fromArray(types));
        graphQL = null;
        return this;
    }

    @SuppressWarnings("unchecked")
    protected <T, R> RuntimeWiring buildWiring(List<EntityModel<?>> models) {
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        TypeRuntimeWiring.Builder queries = newTypeWiring("Query");
        models.forEach(model -> addQueries(model, queries));
        additionalQueries.forEach((query, handler) ->
            queries.dataFetcher(Strings.until(query, '('), environment -> handler.apply(factory.injector(), environment))
        );
        builder.type(queries);
        TypeRuntimeWiring.Builder mutations = newTypeWiring("Mutation");
        models.forEach(model -> addMutations(model, mutations));
        additionalMutations.forEach((mutation, handler) ->
            mutations.dataFetcher(Strings.until(mutation, '('), environment -> handler.apply(factory.injector(), environment))
        );
        builder.type(mutations);
        models.stream().filter(model -> !model.relations().isEmpty()).forEach(model -> {
            TypeRuntimeWiring.Builder relationWiring = newTypeWiring(model.getType().getSimpleName());
            model.relations().stream().map(relation -> (Relation<T, R>) relation)
                .forEach(relation ->
                    relationWiring.dataFetcher(relation.member().getName(), environment -> {
                        GraphQLContext context = environment.getContext();
                        if (!context.hasKey(PersistenceContext.class.getCanonicalName())) {
                            context.put(PersistenceContext.class.getCanonicalName(), factory.newContext());
                        }
                        return relation.getFor(environment.<T>getSource(), context.get(PersistenceContext.class.getCanonicalName()), Include.anyOf(relation));
                    })
                );
            builder.type(relationWiring);
        });
        return builder.build();
    }

    protected <T> void addQueries(EntityModel<T> model, TypeRuntimeWiring.Builder queries) {
        EntityManager<T> entityManager = factory.of(model.getType());
        queries
            .dataFetcher("get" + capitalize(model.singularName()), environment ->
                entityManager.get((Integer) environment.getArgument("id")))
            .dataFetcher("first" + capitalize(model.singularName()), environment ->
                toSelect(entityManager, environment).first())
            .dataFetcher("get" + capitalize(model.pluralName()), environment ->
                entityManager.get(environment.getArgument("ids")).values())
            .dataFetcher("list" + capitalize(model.pluralName()), environment ->
                toSelect(entityManager, environment).list())
            .dataFetcher("count" + capitalize(model.pluralName()), environment ->
                toSelect(entityManager, environment).count());
    }

    protected <T> void addMutations(EntityModel<T> model, TypeRuntimeWiring.Builder mutations) {
        EntityManager<T> entityManager = factory.of(model.getType());
        mutations
            .dataFetcher("create" + capitalize(model.singularName()), environment ->
                entityManager.create(model.newInstanceWithValues(environment.getArgument(model.singularName()))))
            .dataFetcher("create" + capitalize(model.pluralName()), environment ->
                entityManager.create(environment.<List<Map<String, Object>>>getArgument(model.pluralName()).stream().map(model::newInstanceWithValues)))
            .dataFetcher("update" + capitalize(model.singularName()), environment ->
                entityManager.update(model.newInstanceWithValues(environment.getArgument(model.singularName()))))
            .dataFetcher("update" + capitalize(model.pluralName()), environment ->
                entityManager.update(environment.<List<Map<String, Object>>>getArgument(model.pluralName()).stream().map(model::newInstanceWithValues)))
            .dataFetcher("patch" + capitalize(model.pluralName()), environment ->
                toSelect(entityManager, environment).set(environment.getArgument("values")).patch())
            .dataFetcher("delete" + capitalize(model.singularName()), environment ->
                entityManager.delete((Integer) environment.getArgument("id")))
            .dataFetcher("delete" + capitalize(model.pluralName()), environment ->
                entityManager.delete(environment.getArgument("ids")));
    }

    protected <T> Select<T> toSelect(EntityManager<T> entityManager, DataFetchingEnvironment dataFetchingEnvironment) {
        Predicates<T> predicates = Predicates.of(entityManager.model().getType());
        if (dataFetchingEnvironment.containsArgument("where")) {
            Map<String, List<Map<String, Object>>> where = dataFetchingEnvironment.getArgument("where");
            where.forEach((field, predicate) -> predicate.get(0).forEach((p, v) ->
                predicates.where(field, io.semla.query.Predicate.valueOf(p), v)
            ));
        }

        Select<T> where = entityManager.where(predicates);
        if (dataFetchingEnvironment.containsArgument("orderBy")) {
            dataFetchingEnvironment.<Map<String, String>>getArgument("orderBy")
                .forEach((field, sort) -> where.orderedBy(field, Pagination.Sort.valueOf(sort.toUpperCase())));
        }
        if (dataFetchingEnvironment.containsArgument("startAt")) {
            where.startAt(dataFetchingEnvironment.<Integer>getArgument("startAt"));
        }
        if (dataFetchingEnvironment.containsArgument("limitTo")) {
            where.limitTo(dataFetchingEnvironment.<Integer>getArgument("limitTo"));
        }
        return where;
    }

    public String getSchema() {
        return getSchema(getCurrentEntityModels());
    }

    private String getSchema(List<EntityModel<?>> models) {
        StringBuilder builder = new StringBuilder("type Query {\n");
        models.forEach(model -> {
            addGetOneQuery(model, builder);
            addFirstQuery(model, builder);
            addGetManyQuery(model, builder);
            addListQuery(model, builder);
            addCountQuery(model, builder);
        });
        additionalQueries.keySet().forEach(query -> builder.append("    ").append(query).append('\n'));
        builder.append("}\n");

        builder.append("\ntype Mutation {\n");
        models.forEach(model -> {
            addCreate(model, builder);
            addBulkCreate(model, builder);
            if (model.columns().stream().anyMatch(Column::updatable)) {
                addUpdate(model, builder);
                addBulkUpdate(model, builder);
                addPatch(model, builder);
            }
            addDelete(model, builder);
            addBulkDelete(model, builder);
        });
        additionalMutations.keySet().forEach(mutation -> builder.append("    ").append(mutation).append('\n'));
        builder.append("}\n");

        models.forEach(model -> {
            String name = model.getType().getSimpleName();
            addType(model, name, builder);

            Stream.of(model.getType().getDeclaredClasses()).forEach(clazz -> {
                if (clazz.isEnum()) {
                    addEnum(clazz, name + "_" + clazz.getSimpleName(), builder);
                } else {
                    addType(Model.of(clazz), name + "_" + clazz.getSimpleName(), builder);
                }
            });

            addCreateInputType(model, name, builder);
            addPredicatesType(model, name, builder);
            addSortType(model, name, builder);
            addUpdateInputType(model, name, builder);
            addPatchInputType(model, name, builder);
        });

        additionalTypes.forEach(type -> builder.append(type).append("\n"));

        addSemlaTypes(builder);
        return builder.toString();
    }

    // Queries

    protected void addGetOneQuery(EntityModel<?> model, StringBuilder builder) {
        builder.append("    get").append(capitalize(model.singularName())).append("(")
            .append(model.key().member().getName()).append(": ").append(getGraphQLType(model.key().member().getGenericType()))
            .append("!): ").append(model.getType().getSimpleName()).append('\n');
    }

    protected void addFirstQuery(EntityModel<?> model, StringBuilder builder) {
        builder.append("    first").append(capitalize(model.singularName())).append("(")
            .append("where: _").append(capitalize(model.singularName()))
            .append("Predicates, orderBy: _").append(capitalize(model.singularName()))
            .append("Sorts, startAt: Int): ").append(model.getType().getSimpleName()).append('\n');
    }

    protected void addGetManyQuery(EntityModel<?> model, StringBuilder builder) {
        builder.append("    get").append(capitalize(model.pluralName())).append("(")
            .append(Plural.of(model.key().member().getName())).append(": [").append(getGraphQLType(model.key().member().getGenericType())).append("!]!")
            .append("): [").append(model.getType().getSimpleName()).append("!]!\n");
    }

    protected void addListQuery(EntityModel<?> model, StringBuilder builder) {
        builder.append("    list").append(capitalize(model.pluralName())).append("(")
            .append("where: _").append(capitalize(model.singularName()))
            .append("Predicates, orderBy: _").append(capitalize(model.singularName()))
            .append("Sorts, startAt: Int, limitTo: Int): [").append(model.getType().getSimpleName()).append("!]!\n");
    }

    protected void addCountQuery(EntityModel<?> model, StringBuilder builder) {
        builder.append("    count").append(capitalize(model.pluralName())).append("(")
            .append("where: _").append(capitalize(model.singularName()))
            .append("Predicates, orderBy: _").append(capitalize(model.singularName()))
            .append("Sorts, startAt: Int, limitTo: Int): Int!\n");
    }

    // Mutations

    protected void addCreate(EntityModel<?> model, StringBuilder builder) {
        builder.append("    create").append(capitalize(model.singularName())).append("(")
            .append(model.singularName()).append(": _").append(capitalize(model.singularName()))
            .append("Create!): ").append(model.getType().getSimpleName()).append("!\n");
    }

    protected void addBulkCreate(EntityModel<?> model, StringBuilder builder) {
        builder.append("    create").append(capitalize(model.pluralName())).append("(")
            .append(model.pluralName()).append(": [_").append(capitalize(model.singularName()))
            .append("Create!]!): [").append(model.getType().getSimpleName()).append("!]!\n");
    }

    protected void addUpdate(EntityModel<?> model, StringBuilder builder) {
        builder.append("    update").append(capitalize(model.singularName())).append("(")
            .append(model.singularName()).append(": _").append(capitalize(model.singularName()))
            .append("Update!): ").append(model.getType().getSimpleName()).append("!\n");
    }

    protected void addBulkUpdate(EntityModel<?> model, StringBuilder builder) {
        builder.append("    update").append(capitalize(model.pluralName())).append("(")
            .append(model.pluralName()).append(": [_").append(capitalize(model.singularName()))
            .append("Update!]!): [").append(model.getType().getSimpleName()).append("!]!\n");
    }

    protected void addDelete(EntityModel<?> model, StringBuilder builder) {
        builder.append("    delete").append(capitalize(model.singularName())).append("(")
            .append(model.key().member().getName()).append(": ").append(getGraphQLType(model.key().member().getGenericType()))
            .append("!): Boolean\n");
    }

    protected void addBulkDelete(EntityModel<?> model, StringBuilder builder) {
        builder.append("    delete").append(capitalize(model.pluralName())).append("(")
            .append(Plural.of(model.key().member().getName())).append(": [").append(getGraphQLType(model.key().member().getGenericType()))
            .append("!]!): Int!\n");
    }

    protected void addPatch(EntityModel<?> model, StringBuilder builder) {
        builder.append("    patch").append(capitalize(model.pluralName())).append("(")
            .append("values: _").append(model.getType().getSimpleName()).append("Patch!")
            .append(", where: _").append(capitalize(model.singularName()))
            .append("Predicates, orderBy: _").append(capitalize(model.singularName()))
            .append("Sorts, startAt: Int, limitTo: Int): Int!\n");
    }

    protected <T> void addFields(EntityModel<T> model, String name, boolean isUpdate, StringBuilder builder) {
        builder.append(model.members().stream().map(member -> {
                if (model.isRelation(member)) {
                    Relation<T, Object> relation = model.getRelation(member);
                    CascadeType cascadeType = isUpdate ? MERGE : PERSIST;
                    if (relation.defaultIncludeType().should(cascadeType)) {
                        return member.getName() + ": "
                            + getGraphQLType(member.getGenericType(), type -> "_" + type.getSimpleName() + (cascadeType == MERGE ? "Update" : "Create"));
                    }
                }
                if (model.isColumn(member)) {
                    Column<T> column = model.getColumn(member);
                    if (isUpdate ? column.updatable() : column.insertable()) {
                        String graphQLType = getGraphQLType(member.getGenericType(),
                            type -> getGraphQLType(EntityModel.of(Types.rawTypeOf(type)).key().member().getType())
                        );
                        if (member.getType().getDeclaringClass() != null) {
                            graphQLType = name + "_" + graphQLType;
                        }
                        if (!isUpdate && member.isAnnotatedWithOneOf(Arrays.of(Nonnull.class, NotNull.class, Id.class))) {
                            graphQLType += "!";
                        }
                        return member.getName() + ": " + graphQLType;
                    } else {
                        return "";
                    }
                }
                return "";
            }).filter(Strings::notNullOrEmpty).collect(Collectors.joining("\n    "))
        );
    }

    protected <T> void addType(Model<T> model, String name, StringBuilder builder) {
        builder.append("\ntype ").append(name).append(" {\n");
        model.members().forEach(member -> {
            String graphQLType = getGraphQLType(member.getGenericType());
            if (member.getType().getDeclaringClass() != null) {
                graphQLType = name + "_" + graphQLType;
            }
            if (member.isAnnotatedWithOneOf(Arrays.of(Nonnull.class, NotNull.class, Id.class))) {
                graphQLType += "!";
            }
            builder.append("    ").append(member.getName()).append(": ")
                .append(graphQLType)
                .append('\n');
        });
        builder.append("}\n");
    }

    protected void addCreateInputType(EntityModel<?> model, String name, StringBuilder builder) {
        if (model.columns().stream().anyMatch(Column::insertable)) {
            builder.append("\ninput _").append(name).append("Create {\n    ");
            addFields(model, name, false, builder);
            builder.append("\n}\n");
        }
    }

    protected void addPredicatesType(EntityModel<?> model, String name, StringBuilder builder) {
        builder.append("\ninput _").append(name).append("Predicates {\n    ");
        builder.append(model.columns().stream()
            .map(Column::member)
            .map(member -> member.getName() + ": [" + getPredicateType(member.getGenericType()) + "!]")
            .collect(Collectors.joining("\n    "))
        );
        builder.append("\n}\n");
    }

    protected void addSortType(EntityModel<?> model, String name, StringBuilder builder) {
        builder.append("\ninput _").append(name).append("Sorts {\n    ");
        builder.append(model.columns().stream()
            .map(Column::member)
            .map(member -> member.getName() + ": _Sort")
            .collect(Collectors.joining("\n    "))
        );
        builder.append("\n}\n");
    }

    protected void addUpdateInputType(EntityModel<?> model, String name, StringBuilder builder) {
        if (model.columns().stream().anyMatch(Column::updatable)) {
            builder.append("\ninput _").append(name).append("Update {\n    ")
                .append(model.key().member().getName()).append(": ").append(getGraphQLType(model.key().member().getGenericType()))
                .append("!\n    ");
            addFields(model, name, true, builder);
            builder.append("\n}\n");
        }
    }

    protected void addPatchInputType(EntityModel<?> model, String name, StringBuilder builder) {
        if (model.columns().stream().anyMatch(Column::updatable)) {
            builder.append("\ninput _").append(name).append("Patch {\n    ");
            addFields(model, name, true, builder);
            builder.append("\n}\n");
        }
    }

    protected void addEnum(Class<?> clazz, String name, StringBuilder builder) {
        builder.append("\nenum ").append(name).append(" {");
        builder.append(Stream.of(clazz.getEnumConstants()).map(String::valueOf).collect(Collectors.joining(", ")));
        builder.append("}\n");
    }

    protected String getGraphQLType(Type genericType) {
        return getGraphQLType(genericType, Class::getSimpleName);
    }

    protected <T> String getGraphQLType(Type genericType, Function<Class<T>, String> entityHandler) {
        Class<T> type = Types.rawTypeOf(genericType);
        if (Types.isAssignableToOneOf(type, Integer.class, Long.class, Short.class, Byte.class)) {
            return "Int";
        } else if (Types.isAssignableToOneOf(type, Float.class, Double.class)) {
            return "Float";
        } else if (type.isEnum()) {
            return type.getSimpleName();
        } else if (Types.isAssignableTo(type, Collection.class)) {
            return "[" + getGraphQLType(Types.typeArgumentOf(genericType), entityHandler) + "!]";
        } else if (Types.isAssignableTo(type, Boolean.class)) {
            return "Boolean";
        } else if (type.isArray()) {
            return "[" + getGraphQLType(type.getComponentType(), entityHandler) + "!]";
        } else if (Types.isAssignableToOneOf(type, String.class, Date.class, BigInteger.class, BigDecimal.class, Temporal.class, Calendar.class, Character.class)) {
            return "String";
        } else if (type.equals(Optional.class)) {
            return getGraphQLType(Types.typeArgumentOf(genericType), entityHandler);
        } else if (EntityModel.isEntity(type)) {
            return entityHandler.apply(type);
        }
        return type.getSimpleName();
    }

    protected String getPredicateType(Type genericType) {
        Class<?> type = Types.rawTypeOf(genericType);
        if (Types.isAssignableToOneOf(type, Integer.class, Long.class, Short.class, Byte.class)) {
            return "_IntPredicates";
        } else if (Types.isAssignableToOneOf(type, Float.class, Double.class)) {
            return "_FloatPredicates";
        } else if (Types.isAssignableTo(type, Boolean.class)) {
            return "_BooleanPredicates";
        } else if (type.equals(Optional.class)) {
            return getPredicateType(Types.typeArgumentOf(genericType));
        } else if (EntityModel.isEntity(type)) {
            return getPredicateType(EntityModel.of(type).key().member().getType());
        }
        return "_StringPredicates";
    }

    protected void addSemlaTypes(StringBuilder builder) {
        builder.append("\n" +
            "input _IntPredicates {\n" +
            "    is: Int\n" +
            "    not: Int\n" +
            "    in: [Int!]\n" +
            "    notIn: [Int!]\n" +
            "    greaterOrEquals: Int\n" +
            "    greaterThan: Int\n" +
            "    lessOrEquals: Int\n" +
            "    lessThan: Int\n" +
            "}\n\n" +
            "input _FloatPredicates {\n" +
            "    is: Float\n" +
            "    not: Float\n" +
            "    in: [Float!]\n" +
            "    notIn: [Float!]\n" +
            "    greaterOrEquals: Float\n" +
            "    greaterThan: Float\n" +
            "    lessOrEquals: Float\n" +
            "    lessThan: Float\n" +
            "}\n\n" +
            "input _StringPredicates {\n" +
            "    is: String\n" +
            "    not: String\n" +
            "    in: [String!]\n" +
            "    notIn: [String!]\n" +
            "    like: String\n" +
            "    notLike: String\n" +
            "    contains: String\n" +
            "    doesNotContain: String\n" +
            "    containedIn: String\n" +
            "    notContainedIn: String\n" +
            "}\n\n" +
            "input _BooleanPredicates {\n" +
            "    is: Boolean\n" +
            "    not: Boolean\n" +
            "}\n\n" +
            "enum _Sort {\n" +
            "    asc\n" +
            "    desc\n" +
            "}"
        );
    }


}
