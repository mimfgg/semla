package io.semla.cucumber.steps;

import graphql.ExecutionResult;
import graphql.GraphQL;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.semla.graphql.GraphQLSupplier;
import io.semla.inject.GraphQLModule;
import io.semla.serialization.yaml.Yaml;

import static io.semla.cucumber.steps.Patterns.CLASS_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GraphqlSteps {

    private final TypesSteps objects;
    private ExecutionResult result;

    public GraphqlSteps(TypesSteps objects) {
        this.objects = objects;
    }

    @Before
    public void before() {
        EntitySteps.addModule(new GraphQLModule());
    }

    @SuppressWarnings("unchecked")
    @Then("^the graphql schema of " + CLASS_NAME + " is equal to:$")
    public <T> void its_schema_is_equal_to(String className, String schema) throws ClassNotFoundException {
        Class<T> clazz = (Class<T>) Class.forName(objects.resolve(className));
        EntitySteps.datasourceOf(clazz);
        the_schema_is_equal_to(schema);
    }

    @Then("^the graphql schema is equal to:$")
    public <T> void the_schema_is_equal_to(String expected) {
        assertThatCode(() -> EntitySteps.getInstance(GraphQL.class)).doesNotThrowAnyException();
        String schema = EntitySteps.getInstance(GraphQLSupplier.class).getSchema();
        assertThat(schema).isEqualTo(expected);
    }

    @When("^(?:that )?we query graphql with:$")
    public void we_query_graphql_with(String query) {
        result = EntitySteps.getInstance(GraphQL.class).execute(query);
        if (!result.getErrors().isEmpty()) {
            throw new AssertionError(Yaml.write(result.getErrors()));
        }
    }

    @Then("^we receive:$")
    public void we_receive(String expected) {
        assertThat(Yaml.write(result.getData())).isEqualTo(expected);
    }
}
