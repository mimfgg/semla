package com.decathlon.tzatziki.steps;

import com.decathlon.tzatziki.steps.EntitySteps;
import com.decathlon.tzatziki.utils.Types;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.semla.graphql.GraphQLSupplier;
import io.semla.inject.GraphQLModule;
import io.semla.serialization.yaml.Yaml;

import java.lang.reflect.Type;

import static com.decathlon.tzatziki.utils.Patterns.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GraphqlSteps {

    private ExecutionResult result;

    @Before
    public void before() {
        EntitySteps.addModule(new GraphQLModule());
    }

    @Then("^the graphql schema of " + TYPE + " is equal to:$")
    public <T> void its_schema_is_equal_to(Type type, String schema) throws ClassNotFoundException {
        EntitySteps.datasourceOf(Types.rawTypeOf(type));
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
