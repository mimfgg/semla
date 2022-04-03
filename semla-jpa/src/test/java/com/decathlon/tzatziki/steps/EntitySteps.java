package com.decathlon.tzatziki.steps;

import com.decathlon.tzatziki.utils.Guard;
import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.semla.Semla;
import io.semla.cache.Cache;
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
import io.semla.reflect.TypeReference;
import io.semla.relation.JoinedRelation;
import io.semla.serialization.json.Json;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.decathlon.tzatziki.utils.Guard.GUARD;
import static com.decathlon.tzatziki.utils.Patterns.A_USER;
import static com.decathlon.tzatziki.utils.Patterns.THAT;
import static io.semla.util.Unchecked.unchecked;
import static org.assertj.core.api.Assertions.assertThat;

public class EntitySteps {

    static {
        TypesSteps.addHandler(type -> {
            if (EntityModel.isEntity(type)) {
                try {
                    EntitySteps.datasourceOf(type);
                    EntityModel.of(type).relations().stream()
                        .filter(relation -> relation instanceof JoinedRelation)
                        .map(relation -> (JoinedRelation<?, ?, ?>) relation)
                        .forEach(relation -> EntitySteps.datasourceOf(relation.relationClass()));
                } catch (Exception e) {
                    // if something wrong happens, we want to keep going
                }
            }
        });
    }

    private static final List<Module> modules = new ArrayList<>();
    private static Datasource.Configuration defaultDatasource = InMemoryDatasource::new;
    private static Throwables.UnaryOperator<Binder> defaultCache =
        binder -> binder.bind(Cache.class).to(SoftKeyValueDatasource.configure().asCache());

    private static final Singleton<Semla> semla = Singleton.lazy(() ->
        Semla.configure()
            .withDefaultDatasource(defaultDatasource)
            .withUUIDGenerator(new Supplier<UUID>() {

                private final AtomicInteger counter = new AtomicInteger();

                @Override
                public UUID get() {
                    return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(counter.incrementAndGet()));
                }
            })
            .withModules(modules)
            .withBindings(defaultCache)
            .create()
    );

    private static final List<Runnable> cleanups = Lists.of(
        Model::clear,
        modules::clear,
        semla::reset
    );

    private final ObjectSteps objects;

    public EntitySteps(ObjectSteps objects) {
        this.objects = objects;
    }

    public static EntityManagerFactory factory() {
        return semla.get().getInstance(EntityManagerFactory.class);
    }

    public static <E> E getInstance(Class<E> clazz) {
        return semla.get().getInstance(clazz);
    }

    public static <E> E getInstance(TypeReference<E> typeReference) {
        return semla.get().getInstance(typeReference);
    }

    public <K, T> EntityManager<K, T> entityManagerOf(String entityName) {
        return entityManagerOf(Model.getClassBy(objects.resolve(entityName)));
    }

    public static <K, T> EntityManager<K, T> entityManagerOf(Class<T> clazz) {
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

    public static Datasource.Configuration getDefaultDatasource() {
        return defaultDatasource;
    }

    public static void setDefaultDatasource(Datasource.Configuration defaultDatasource) {
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

    @Before
    public void before() {
        // just to hook in the ObjectSteps
    }

    @After
    public static void cleanup() {
        cleanups.forEach(Runnable::run);
    }

    @When(THAT + GUARD + "the model of ([{}\\w.]+) is generated$")
    public void the_model_is_generated(Guard guard, String name) {
        guard.in(objects, () -> Model.of(unchecked(() -> Class.forName(objects.resolve(name)))));
    }

    @Given("^th(?:is|ose|ese) ([^ ]+) entit(?:ies|y):$")
    public <K, T> void those_entities(String entityName, Object entities) {
        if (entities instanceof DataTable) {
            List<Map<String, Object>> list = ((DataTable) entities).asMaps(String.class, Object.class);
            if (!list.isEmpty()) {
                EntityManager<K, T> manager = entityManagerOf(entityName);
                manager.create(list.stream().map(values -> manager.model().newInstanceWithValues(values)).collect(Collectors.toList()));
            }
        } else {
            to_query_with_payload("create the " + entityName, entities);
        }
    }

    @Then(THAT + A_USER + "((?:fetch|get|list|delete|count|patch) [^:]+)$")
    public void to_query(String query) {
        Query.parse(query).in(newContext());
    }

    @Then(THAT + "((?:fetching|getting|listing|deleting|counting|patching) .*) returns:$")
    @When(THAT + "((?:fetching|getting|listing|deleting|counting|patching) .*) returns (.*)$")
    public void to_query_returns(String queryAsString, Object expected) {
        Query<?, ?> query = Query.parse(queryAsString);
        Object result = query.in(newContext());
        String expectedString = toYaml(query.model(), expected);
        if (result instanceof List && ((List<?>) result).size() == 1 && !Strings.firstNonWhitespaceCharacterIs(expectedString, '-', '[')) {
            result = ((List<?>) result).get(0);
        }
        assertThat(Yaml.write(result)).isEqualTo(expectedString);
    }

    @Then(THAT + A_USER + "((?:create|update|patch) .*):$")
    @Then(THAT + "((?:creating|updating|patching) .*):$")
    public void to_query_with_payload(String query, Object payload) {
        if (payload instanceof DocString) {
            payload = ((DocString) payload).getContent();
        }
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
        if (expected instanceof DocString) {
            expected = ((DocString) expected).getContent();
        }
        if (expected instanceof String) {
            if (Json.isJson((String) expected)) {
                return Yaml.write(Json.read((String) expected));
            }
            return (String) expected;
        }
        List<?> list = ((DataTable) expected).<String, Object>asMaps(String.class, Object.class)
            .stream().map(model::newInstanceWithValues).collect(Collectors.toList());
        if (list.size() == 1) {
            return Yaml.write(list.get(0));
        }
        return Yaml.write(list);
    }

    private String toJson(EntityModel<?> model, Object expected) {
        if (expected instanceof DocString) {
            expected = ((DocString) expected).getContent();
        }
        if (expected instanceof String) {
            if (!Json.isJson((String) expected)) {
                return Json.write(Yaml.read((String) expected));
            }
            return (String) expected;
        }
        List<?> list = ((DataTable) expected).<String, Object>asMaps(String.class, Object.class)
            .stream().map(model::newInstanceWithValues).collect(Collectors.toList());
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
