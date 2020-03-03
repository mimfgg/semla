package io.semla.cucumber.steps;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import io.semla.model.EntityModel;
import io.semla.reflect.Types;
import io.semla.relation.JoinedRelation;
import io.semla.util.Strings;
import org.apache.commons.lang3.ClassUtils;
import org.junit.Assert;
import se.redmind.utils.Fields;
import se.redmind.utils.Methods;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.semla.cucumber.steps.Mapper.deserialize;
import static io.semla.cucumber.steps.Mapper.serialize;
import static io.semla.cucumber.steps.Patterns.A;
import static io.semla.cucumber.steps.Patterns.VARIABLE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


@SuppressWarnings("unchecked")
public class ObjectSteps {

    private static int RUN_NUMBER;
    private static final Pattern LIST = Pattern.compile(VARIABLE + "\\[([0-9]+)]");
    private static final Pattern CLASS = Pattern.compile("class(?:es)?");
    private final Map<String, Object> objects = new LinkedHashMap<>();
    private Function<String, String> sourceModifier = source -> source;

    public static int getRunNumber() {
        return RUN_NUMBER;
    }

    @Before
    public void before() {
        RUN_NUMBER++;
        put("run", "run" + RUN_NUMBER);
    }

    @Given("^source prepend \"([^\"]*)\"$")
    public void source_prepend(String value) {
        sourceModifier = sourceModifier.compose(source -> value.replaceAll("\\\\n", "\n") + "\n" + source);
    }

    @Given("^source prepend:$")
    public void source_prepend_block(String value) {
        sourceModifier = sourceModifier.compose(source -> value.replaceAll("\\\\n", "\n") + "\n" + source);
    }

    @Given("^" + A + VARIABLE + "(?: is|are)?:$")
    public void put(String name, Object value) {
        if (value instanceof String) {
            value = resolve((String) value);
        }
        if (CLASS.matcher(name).matches() && value instanceof String) {
            Types.compileFromSources(
                Stream.of(((String) value).split("\n---\r?\n"))
                    .map(String::trim)
                    .map(sourceModifier)
                    .map(this::resolve)
                    .toArray(String[]::new)
            ).stream()
                .filter(EntityModel::isEntity)
                .forEach(type -> {
                    try {
                        EntitySteps.datasourceOf(type);
                        EntityModel.of(type).relations().stream()
                            .filter(relation -> relation instanceof JoinedRelation)
                            .map(relation -> (JoinedRelation<?, ?, ?>) relation)
                            .forEach(relation -> EntitySteps.datasourceOf(relation.relationClass()));
                    } catch (Exception e) {
                        // if something wrong happens, we want to keep going
                    }
                });
        } else {
            if (!name.contains(".")) {
                // simple object
                if (objects.containsKey(name)) {
                    Object currentValue = objects.get(name);
                    assertThat("you are attemping to override an already existing value with an object of another type: "
                            + name + " of type " + currentValue.getClass() + " with " + value.getClass(),
                        value.getClass().toString(), is(currentValue.getClass().toString()));
                }
                objects.put(name, value);
            } else {
                // property of an object
                try {
                    int split = name.lastIndexOf(".");
                    Object host = get(name.substring(0, split));
                    String property = name.substring(split + 1);
                    Field field = Fields.getField(host.getClass(), property);
                    if (!ClassUtils.isAssignable(value.getClass(), field.getType())) {
                        if (value instanceof String) {
                            value = deserialize((String) value, field.getType());
                        } else {
                            Assert.fail("cannot convert a " + value.getClass() + " into a " + field.getType());
                        }
                    }
                    field.set(host, value);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    public <T> T get(String name) {
        return get(name, false);
    }

    public <T> T get(String name, boolean acceptNull) {
        Object object = objects.get(name);
        if (object == null) {
            if (name.matches("it") && !objects.isEmpty()) {
                Map.Entry<String, Object> tail = Fields.getValue(objects, "tail");
                object = tail.getValue();
            } else if (name.contains(".")) {
                // let's look for a property of an object or an entry of a map
                int split = name.indexOf(".");
                object = getProperty(get(name.substring(0, split), true), name.substring(split + 1, name.length()));
            }
        }
        if (!acceptNull) {
            Assert.assertNotNull("there is no object named '" + name + "'", object);
        }
        if (object instanceof String) {
            object = resolve((String) object);
        }
        return (T) object;
    }

    private Object getProperty(Object object, String property) {
        if (object != null && property != null) {
            String subProperty = null;
            if (property.contains(".")) {
                int split = property.indexOf(".");
                subProperty = property.substring(split + 1, property.length());
                property = property.substring(0, split);
            }
            if (object instanceof Map) {
                object = ((Map<String, ?>) object).get(property);
            } else if (object instanceof String) {
                object = deserialize((String) object, Map.class).get(property);
            } else {
                Integer index = null;
                Matcher listItem = LIST.matcher(property);
                if (listItem.matches()) {
                    // we want to remove the [id] from the property and keep the index for later use
                    index = Integer.parseInt(listItem.group(2));
                    property = listItem.group(1);
                }

                object = findPropertyValue(object, property);

                if (index != null) {
                    // this is a list
                    Assert.assertTrue(object instanceof Collection);
                    Assert.assertTrue(((Collection) object).size() > index);
                    if (!(object instanceof List)) {
                        object = new ArrayList<>((Collection) object);
                    }
                    object = ((List) object).get(index);
                }
            }
            if (subProperty != null) {
                object = getProperty(object, subProperty);
            }
        }
        return object;
    }

    private static Object findPropertyValue(Object object, String property) {
        String capitalizedProperty = Strings.capitalize(property);
        if (Methods.findMethod(object.getClass(), property) != null) {
            return Methods.invoke(object, property);
        } else if (Methods.findMethod(object.getClass(), "get" + capitalizedProperty) != null) {
            return Methods.invoke(object, "get" + capitalizedProperty);
        } else if (ClassUtils.isAssignable(object.getClass(), Boolean.class) && Methods.findMethod(object.getClass(), "is" + capitalizedProperty) != null) {
            return Methods.invoke(object, "is" + capitalizedProperty);
        } else if (Fields.getFieldsByNameOf(object.getClass()).containsKey(property)) {
            return Fields.getValue(object, property);
        } else {
            throw new AssertionError("didn't find any property matching '" + property + "'");
        }
    }

    public String resolve(String value) {
        if (value != null) {
            int start = value.lastIndexOf("{{");
            while (start > -1) {
                int end = Strings.getClosingBracketIndex(value, start, '{', '}') - 1;
                if (end > -2) {
                    String alias = value.substring(start + 2, end);
                    String resolved;
                    Object object = get(alias);
                    if (object instanceof String) {
                        resolved = (String) object;
                    } else {
                        resolved = serialize(object);
                    }
                    value = value.substring(0, start) + resolved + value.substring(end + 2);
                    start = value.lastIndexOf("{{");
                } else {
                    // unclosed brackets...
                    start = -1;
                }
            }
        }
        return value;
    }
}
