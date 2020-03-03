package io.semla.cucumber.steps;

import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.semla.Semla;
import io.semla.cache.Cache;
import io.semla.config.DatasourceConfiguration;
import io.semla.datasource.Datasource;
import io.semla.datasource.DatasourceFactory;
import io.semla.datasource.InMemoryDatasource;
import io.semla.datasource.SoftKeyValueDatasource;
import io.semla.inject.Binder;
import io.semla.inject.Module;
import io.semla.model.EntityModel;
import io.semla.model.Model;
import io.semla.persistence.EntityManager;
import io.semla.persistence.EntityManagerFactory;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Query;
import io.semla.serialization.json.Json;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.*;

import javax.enterprise.util.TypeLiteral;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.semla.cucumber.steps.Patterns.A_USER;
import static io.semla.cucumber.steps.Patterns.THAT;
import static org.assertj.core.api.Assertions.assertThat;

public class EntitySteps {

    private static DatasourceConfiguration defaultDatasource = InMemoryDatasource::new;
    private static List<Module> modules = new ArrayList<>();
    private static Throwables.UnaryOperator<Binder> defaultCache =
        binder -> binder.bind(Cache.class).to(SoftKeyValueDatasource.configure().asCache());

    private static final Singleton<Semla> semla = Singleton.lazy(() ->
        Semla.configure()
            .withDefaultDatasource(defaultDatasource)
            .withUUIDGenerator(new Supplier<UUID>() {

                private AtomicInteger counter = new AtomicInteger();

                @Override
                public UUID get() {
                    return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", counter.incrementAndGet()));
                }
            })
            .withModules(modules)
            .withBindings(defaultCache)
            .create()
    );

    private static List<Runnable> cleanups = Lists.of(
        Model::clear,
        modules::clear,
        semla::reset
    );

    private final ObjectSteps objects;
    private final ThrowableSteps throwables;

    public EntitySteps(ObjectSteps objects, ThrowableSteps throwables) {
        this.objects = objects;
        this.throwables = throwables;
    }

    public static EntityManagerFactory factory() {
        return semla.get().getInstance(EntityManagerFactory.class);
    }

    public static <E> E getInstance(Class<E> clazz) {
        return semla.get().getInstance(clazz);
    }

    public static <E> E getInstance(TypeLiteral<E> typeLiteral) {
        return semla.get().getInstance(typeLiteral);
    }

    public <T> EntityManager<T> entityManagerOf(String entityName) {
        return entityManagerOf(Model.getClassBy(objects.resolve(entityName)));
    }

    public static <T> EntityManager<T> entityManagerOf(Class<T> clazz) {
        return factory().of(clazz);
    }

    public static <T, DatasourceType extends Datasource<T>> DatasourceType datasourceOf(Class<T> clazz) {
        return semla.get().getInstance(DatasourceFactory.class).of(clazz);
    }

    public static void registerDatasource(Datasource<?> datasource) {
        semla.get().getInstance(DatasourceFactory.class).registerDatasource(datasource);
    }

    public static PersistenceContext newContext() {
        return factory().newContext();
    }

    public static DatasourceConfiguration getDefaultDatasource() {
        return defaultDatasource;
    }

    public static void setDefaultDatasource(DatasourceConfiguration defaultDatasource) {
        EntitySteps.defaultDatasource = defaultDatasource;
    }

    public static void setDefaultCache(Throwables.UnaryOperator<Binder> defaultCache) {
        EntitySteps.defaultCache = defaultCache;
    }

    public static void addModule(Module module) {
        EntitySteps.modules.add(module);
    }

    public static void addCleanup(Runnable cleanup) {
        cleanups.add(cleanup);
    }

    @After
    public static void cleanup() {
        cleanups.forEach(Runnable::run);
    }

    @When("^" + THAT + "the model of ([{}\\w.]+) is generated$")
    public void the_model_is_generated(String name) {
        throwables.catchThrowable(() -> Model.of(Class.forName(objects.resolve(name))));
    }

    @Given("^th(?:is|ose|ese) ([^ ]+) entit(?:ies|y):$")
    public <T> void those_entities(String entityName, Object entities) {
        if (entities instanceof DataTable) {
            List<Map<String, Object>> list = ((DataTable) entities).asMaps(String.class, Object.class);
            if (!list.isEmpty()) {
                EntityManager<T> manager = entityManagerOf(entityName);
                manager.create(list.stream().map(values -> manager.model().newInstanceWithValues(values)).collect(Collectors.toList()));
            }
        } else {
            to_query_with_payload("create the " + entityName, (String) entities);
        }
    }

    @Then("^" + THAT + A_USER + " ((?:fetch|get|list|delete|count|patch) [^:]+)$")
    public void to_query(String query) {
        Query.parse(query).in(newContext());
    }

    @Then("^" + THAT + A_USER + "((?:fetching|getting|listing|deleting|counting|patching) .*) returns:$")
    @When("^" + THAT + A_USER + "((?:fetching|getting|listing|deleting|counting|patching) .*) returns (.*)$")
    public void to_query_returns(String queryAsString, Object expected) {
        Query<?, ?> query = Query.parse(queryAsString);
        Object result = query.in(newContext());
        String expectedString = toYaml(query.model(), expected);
        if (result instanceof List && ((List<?>) result).size() == 1 && !Strings.firstNonWhitespaceCharacterIs(expectedString, '-', '[')) {
            result = ((List<?>) result).get(0);
        }
        assertThat(Yaml.write(result)).isEqualTo(expectedString);
    }

    @Then("^" + THAT + A_USER + "((?:create|creating|update|updating|patch|patching) .*):$")
    public void to_query_with_payload(String query, Object payload) {
        if (query.endsWith("those")) {
            Yaml.<Map<String, Object>>read((String) payload, Map.class).forEach((entityClass, entity) ->
                to_query_with_payload("create those " + entityClass, Json.write(entity)));
        } else {
            if (!query.endsWith("with")) {
                query += " ->";
            }
            Query.parse(query + " " + toJson(parseModel(query), payload)).in(newContext());
        }
    }

    private String toYaml(EntityModel<?> model, Object expected) {
        if (expected instanceof String) {
            if (Json.isJson((String) expected)) {
                return Yaml.write(Json.read((String) expected));
            }
            return (String) expected;
        }
        List<?> list = ((DataTable) expected).asMaps(String.class, Object.class).stream().map(model::newInstanceWithValues).collect(Collectors.toList());
        if (list.size() == 1) {
            return Yaml.write(list.get(0));
        }
        return Yaml.write(list);
    }

    private String toJson(EntityModel<?> model, Object expected) {
        if (expected instanceof String) {
            if (!Json.isJson((String) expected)) {
                return Json.write(Yaml.read((String) expected));
            }
            return (String) expected;
        }
        List<?> list = ((DataTable) expected).asMaps(String.class, Object.class).stream().map(model::newInstanceWithValues).collect(Collectors.toList());
        if (list.size() == 1) {
            return Json.write(list.get(0));
        }
        return Json.write(list);
    }

    private static <T> EntityModel<T> parseModel(String query) {
        List<String> tokens = Splitter.on(' ').omitEmptyStrings().split(query).toList();
        String queryType = tokens.remove(0);
        if (tokens.get(0).equals("all")) {
            tokens.remove(0);
        }

        if (Strings.equalsOneOf(tokens.get(0), "the", "this", "that", "those", "these")) {
            tokens.remove(0);
        }
        if (queryType.startsWith("fetch") && tokens.get(0).equals("first")) {
            tokens.remove(0);
        }
        return EntityModel.of(tokens.get(0));
    }
}
