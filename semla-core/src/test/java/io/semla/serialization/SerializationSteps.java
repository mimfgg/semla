package io.semla.serialization;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.semla.config.InMemoryDatasourceConfiguration;
import io.semla.cucumber.steps.ThrowableSteps;
import io.semla.reflect.Types;
import io.semla.serialization.json.Json;
import io.semla.serialization.json.JsonSerializer;
import io.semla.serialization.yaml.Yaml;
import org.assertj.core.api.Assertions;

public class SerializationSteps {

    static {
        Types.registerSubTypes(InMemoryDatasourceConfiguration.class);
    }

    private final ThrowableSteps throwableSteps;
    private String content;
    private Object object;

    public SerializationSteps(ThrowableSteps throwableSteps) {
        this.throwableSteps = throwableSteps;
    }

    @When("^the following json gets parsed as a (.*):$")
    public void the_following_json_gets_parsed_as_a(String type, String content) {
        this.content = content;
        throwableSteps.catchThrowable(() -> {
            object = Json.read(content, Class.forName(type), Deserializer.IGNORE_UNKNOWN_PROPERTIES);
        });
    }

    @When("^the following yaml gets parsed as a (.*):$")
    public void the_following_yaml_gets_parsed_as_a(String type, String content) {
        this.content = content;
        throwableSteps.catchThrowable(() -> object = Yaml.read(content, Class.forName(type), Deserializer.IGNORE_UNKNOWN_PROPERTIES));
    }

    @Then("^it gets (pretty )?serialized as this json:$")
    public void it_gets_serialized_as_this_json(String pretty, String content) {
        throwableSteps.assertNothingWasThrown();
        Assertions.assertThat(Json.write(object, pretty != null ? new Serializer.Option[]{JsonSerializer.PRETTY} : new Serializer.Option[0]))
            .isEqualTo(content);
    }

    @And("^it gets serialized as this yaml:$")
    public void it_gets_serialized_as_this_yaml(String content) {
        throwableSteps.assertNothingWasThrown();
        Assertions.assertThat(Yaml.write(object)).isEqualTo(content);
    }

    @And("^it gets serialized as the same yaml$")
    public void it_gets_serialized_as_the_same_yaml() {
        it_gets_serialized_as_this_yaml(content);
    }
}
