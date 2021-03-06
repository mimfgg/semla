package io.semla.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.semla.Semla;
import io.semla.inject.GraphQLModule;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.Maps;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphQLSupplierTest {

    @Test
    public void addAdditionalMethodsDuringInitialization() {
        Semla semla = Semla.configure()
            .withModules(new GraphQLModule()
                .withQuery("fibonacci(input: Int!) : Int!", (injector, environment) ->
                    fibonacci(environment.getArgument("input"))
                )
                .withTypes(
                    "input Request { text: String! } ",
                    "type Response { text: String! } "
                )
                .withMutation("ping(request: Request!): Response!", (injector, environment) ->
                    Maps.of(
                        "text",
                        "hello " + environment.<Map<String, String>>getArgument("request").get("text")
                    )
                )
            )
            .create();

        GraphQLSupplier graphQLSupplier = semla.getInstance(GraphQLSupplier.class);
        assertFibonacci(graphQLSupplier);
        assertPing(graphQLSupplier);
    }

    @Test
    public void addAdditionalMethodsLaterOn() {
        Semla semla = Semla.configure()
            .withModules(new GraphQLModule())
            .create();

        GraphQLSupplier graphQLSupplier = semla.getInstance(GraphQLSupplier.class);

        graphQLSupplier.addQuery("fibonacci(input: Int!) : Int!", (injector, environment) ->
            fibonacci(environment.getArgument("input"))
        );

        assertFibonacci(graphQLSupplier);

        graphQLSupplier
            .addTypes(
                "input Request { text: String! } ",
                "type Response { text: String! } "
            )
            .addMutation("ping(request: Request!): Response!", (injector, environment) ->
                Maps.of(
                    "text",
                    "hello " + environment.<Map<String, String>>getArgument("request").get("text")
                )
            );

        assertPing(graphQLSupplier);
    }

    private void assertFibonacci(GraphQLSupplier graphQLSupplier) {
        GraphQL graphQL = graphQLSupplier.get();
        ExecutionResult executionResult = graphQL.execute("query { fibonacci(input: 5) }");
        assertThat(executionResult.getErrors()).isEmpty();
        assertThat(Yaml.write(executionResult.<Map<String, ?>>getData())).isEqualTo("fibonacci: 5");
    }

    private static int fibonacci(int n) {
        return n <= 1 ? n : fibonacci(n - 1) + fibonacci(n - 2);
    }

    private void assertPing(GraphQLSupplier graphQLSupplier) {
        GraphQL graphQL = graphQLSupplier.get();
        ExecutionResult executionResult = graphQL.execute("mutation { ping(request: {text: \"world\"}){ text } }");
        assertThat(executionResult.getErrors()).isEmpty();
        assertThat(Yaml.write(executionResult.<Map<String, ?>>getData())).isEqualTo("ping:\n  text: hello world");
    }
}
