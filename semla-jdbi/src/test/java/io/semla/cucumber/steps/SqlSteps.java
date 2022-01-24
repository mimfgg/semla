package io.semla.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.semla.datasource.SqlDatasource;
import io.semla.model.EntityModel;
import io.semla.reflect.Types;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Function;

import static io.semla.cucumber.steps.Patterns.CLASS_NAME;
import static io.semla.cucumber.steps.Patterns.THAT;
import static io.semla.util.Unchecked.unchecked;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlSteps {

    private final ObjectSteps objects;
    private Function<EntityModel<?>, SqlDatasource<?>> sqlDatasourceGenerator;

    public SqlSteps(ObjectSteps objects) {
        this.objects = objects;
    }

    @Given("^" + THAT + CLASS_NAME + " is the default datasource$")
    public void the_default_datasource_is(String name) throws ClassNotFoundException {
        Class<?> clazz = Types.forName(name);
        sqlDatasourceGenerator = entityModel -> unchecked(() -> (SqlDatasource<?>) clazz
            .getConstructor(EntityModel.class, Jdbi.class, String.class)
            .newInstance(entityModel, null, entityModel.tablename()));
    }

    @Then("^the schema of " + CLASS_NAME + " is equal to:$")
    public void its_schema_is_equal_to(String className, String schema) throws ClassNotFoundException {
        className = objects.resolve(className);
        Class<?> clazz = Types.forName(className);
        SqlDatasource<?> sqlDatasource = sqlDatasourceGenerator.apply(EntityModel.of(clazz));
        assertThat(String.join("", sqlDatasource.ddl().create())).isEqualTo(schema);
    }
}
