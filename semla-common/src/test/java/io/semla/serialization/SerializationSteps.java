package io.semla.serialization;

import com.decathlon.tzatziki.steps.ObjectSteps;
import com.decathlon.tzatziki.utils.Patterns;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.semla.serialization.json.Json;
import io.semla.serialization.json.JsonSerializer;
import io.semla.serialization.yaml.Yaml;
import org.assertj.core.api.Assertions;

public class SerializationSteps {

    private final ObjectSteps objectSteps;

    public SerializationSteps( ObjectSteps objectSteps) {
        this.objectSteps = objectSteps;
    }

    @Before
    public void before() {
    }

    @Then(Patterns.THAT + Patterns.VARIABLE + " gets (pretty )?serialized as this json:$")
    public void json_gets_serialized_as_this_json(String name, boolean pretty, String content) {
        if (pretty) {
            Assertions.assertThat(Json.write(objectSteps.get(name), JsonSerializer.PRETTY)).isEqualTo(content);
        } else {
            Assertions.assertThat(Json.write(objectSteps.get(name))).isEqualTo(content);
        }
    }

    @And(Patterns.THAT + Patterns.VARIABLE + " gets serialized as this yaml:$")
    public void it_gets_serialized_as_this_yaml(String name, String content) {
        Assertions.assertThat(Yaml.write(objectSteps.get(name))).isEqualTo(content);
    }
}
