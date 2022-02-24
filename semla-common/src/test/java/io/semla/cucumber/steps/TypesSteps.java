package io.semla.cucumber.steps;

import com.decathlon.tzatziki.steps.ObjectSteps;
import com.decathlon.tzatziki.utils.Guard;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.semla.reflect.Types;
import io.semla.util.Strings;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.decathlon.tzatziki.utils.Guard.GUARD;
import static com.decathlon.tzatziki.utils.Patterns.*;

public class TypesSteps {

    private static int RUN_NUMBER;
    private static final List<Consumer<Class<?>>> CLASS_HANDLERS = new ArrayList<>();
    private Function<String, String> sourceModifier = source -> source;

    private final ObjectSteps objectSteps;

    public TypesSteps(ObjectSteps objectSteps) {
        this.objectSteps = objectSteps;
    }

    public static void addHandler(Consumer<Class<?>> handler) {
        CLASS_HANDLERS.add(handler);
    }

    public static int getRunNumber() {
        return RUN_NUMBER;
    }

    @Before
    public void before() {
        RUN_NUMBER++;
        objectSteps.add("run", "run" + RUN_NUMBER);
    }

    @Given("^source prepend \"([^\"]*)\"$")
    public void source_prepend(String value) {
        sourceModifier = sourceModifier.compose(source -> value.replaceAll("\\\\n", "\n") + "\n" + source);
    }

    @Given("^source prepend:$")
    public void source_prepend_block(String value) {
        sourceModifier = sourceModifier.compose(source -> value.replaceAll("\\\\n", "\n") + "\n" + source);
    }

    @Given(THAT + A + "types?(?: (?:is|are) compiled)?:$")
    public void put(String value) {
        Types.compileFromSources(
            split(value).stream()
                .map(String::trim)
                .map(sourceModifier)
                .map(objectSteps::resolve)
                .toArray(String[]::new)
        ).forEach(clazz -> CLASS_HANDLERS.forEach(handler -> handler.accept(clazz)));
    }

    private List<String> split(String value) {
        int start = 0;
        List<String> splitted = new ArrayList<>();
        while (true) {
            int stop = Strings.getClosingBracketIndex(value, start, '{', '}');
            if (stop > start) {
                splitted.add(value.substring(start, ++stop));
                start = stop;
            } else {
                break;
            }
        }
        return splitted;
    }

    @Given(THAT + GUARD + A_USER + "registers? " + TYPE + " as a subtype")
    public void that_we_register_child_as_a_subtype(Guard guard, Type type) {
        guard.in(objectSteps, () -> Types.registerSubType(Types.rawTypeOf(type)));
    }

}
