package io.semla.inject;

import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import io.semla.graphql.GraphQLSupplier;
import io.semla.reflect.TypeReference;
import io.semla.util.Lists;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class GraphQLModule implements Module {

    public static final String GRAPHQL_ADDITIONAL_QUERIES = "GraphQLProvider.additionalQueries";
    public static final String GRAPHQL_ADDITIONAL_MUTATIONS = "GraphQLProvider.additionalMutations";
    public static final String GRAPHQL_ADDITIONAL_TYPES = "GraphQLProvider.additionalTypes";

    private final Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalQueries = new LinkedHashMap<>();
    private final Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>> additionalMutations = new LinkedHashMap<>();
    private final List<String> additionalTypes = new ArrayList<>();

    @Override
    public void configure(Binder binder) {
        binder
            .bind(new TypeReference<Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>>>() {}).named(GRAPHQL_ADDITIONAL_QUERIES).to(additionalQueries)
            .bind(new TypeReference<Map<String, BiFunction<Injector, DataFetchingEnvironment, ?>>>() {}).named(GRAPHQL_ADDITIONAL_MUTATIONS).to(additionalMutations)
            .bind(new TypeReference<List<String>>() {}).named(GRAPHQL_ADDITIONAL_TYPES).to(additionalTypes)
            .bind(GraphQL.class).toSupplier(GraphQLSupplier.class);
    }

    public GraphQLModule withQuery(String query, BiFunction<Injector, DataFetchingEnvironment, ?> handler) {
        additionalQueries.put(query, handler);
        return this;
    }

    public GraphQLModule withTypes(String... types) {
        additionalTypes.addAll(Lists.fromArray(types));
        return this;
    }

    public GraphQLModule withMutation(String query, BiFunction<Injector, DataFetchingEnvironment, ?> handler) {
        additionalMutations.put(query, handler);
        return this;
    }
}
