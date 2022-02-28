package com.decathlon.tzatziki.steps;

import com.decathlon.tzatziki.steps.ObjectSteps;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.semla.datasource.SqlDatasource;
import io.semla.model.EntityModel;
import io.semla.reflect.Types;
import org.jdbi.v3.core.Jdbi;

import java.lang.reflect.Type;
import java.util.function.Function;

import static com.decathlon.tzatziki.utils.Patterns.THAT;
import static com.decathlon.tzatziki.utils.Patterns.TYPE;
import static io.semla.util.Unchecked.unchecked;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlSteps {

    private final ObjectSteps objects;
    private Function<EntityModel<?>, SqlDatasource<?>> sqlDatasourceGenerator;

    public SqlSteps(ObjectSteps objects) {
        this.objects = objects;
    }

    @Given(THAT + TYPE + " is the default datasource$")
    public void the_default_datasource_is(Type type) throws ClassNotFoundException {
        sqlDatasourceGenerator = entityModel -> unchecked(() -> (SqlDatasource<?>) Types.rawTypeOf(type)
            .getConstructor(EntityModel.class, Jdbi.class, String.class)
            .newInstance(entityModel, null, entityModel.tablename()));
    }

    @Then("^the schema of " + TYPE + " is equal to:$")
    public void its_schema_is_equal_to(String className, String schema) throws ClassNotFoundException {
        className = objects.resolve(className);
        Class<?> clazz = Class.forName(className);
        SqlDatasource<?> sqlDatasource = sqlDatasourceGenerator.apply(EntityModel.of(clazz));
        assertThat(String.join("", sqlDatasource.ddl().create())).isEqualTo(schema);
    }
}
