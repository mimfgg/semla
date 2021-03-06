package io.semla.relation;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.*;
import io.semla.persistence.EntityManager;
import io.semla.persistence.Families;
import io.semla.query.Includes;
import io.semla.util.Javassist;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;


public class RelationsTest {

    private FruitManager fruits;
    private GenusManager genuses;
    private Families families;

    @Before
    public void before() {
        families = EntitySteps.getInstance(Families.class);
        Family musaceae = families.newFamily().name("Musaceae").create();
        Family rosaceae = families.newFamily().name("Rosaceae").create();

        genuses = EntitySteps.getInstance(GenusManager.class);
        Genus musa = genuses.newGenus().name("musa").family(musaceae).create();
        Genus malus = genuses.newGenus().name("malus").family(rosaceae).create();
        Genus prunus = genuses.newGenus().name("prunus").family(rosaceae).create();

        fruits = EntitySteps.getInstance(FruitManager.class);
        fruits.newFruit().name("banana").price(1).genus(musa).create();
        fruits.newFruit().name("apple").price(2).genus(malus).create();
        fruits.newFruit().name("peach").price(5).genus(prunus).create();
    }

    @Test
    public void includeAParent() {
        Fruit apple = fruits.where().name().is("apple").first(include -> include.genus()).get();
        Assertions.assertThat(apple.genus).isNotNull();
        Assertions.assertThat(apple.genus.name).isEqualTo("malus");
    }

    @Test
    public void includeAParentAndAChild() {
        Genus malus = genuses.where().name().is("malus").first(genus -> genus.family(), genus -> genus.fruits()).get();
        Assertions.assertThat(malus.family).isNotNull();
        Assertions.assertThat(malus.family.name).isEqualTo("Rosaceae");
        Assertions.assertThat(malus.fruits.size()).isEqualTo(1);
        Assertions.assertThat(malus.fruits.get(0).name).isEqualTo("apple");
    }

    @Test
    public void includeShouldNotLoopForever() {
        Genus malus = genuses.where().name().is("malus").first(genus -> genus.fruits(fruits -> fruits.genus(genus2 -> genus2.fruits(fruits2 -> fruits2.genus())))).get();
        Assertions.assertThat(malus.fruits).isNotNull().isNotEmpty();
        Assertions.assertThat(malus.fruits.get(0).genus).isNotNull();
        List<Genus> list = genuses.where().name().is("malus").list(genus -> genus.fruits(fruits -> fruits.genus(genus2 -> genus2.fruits(fruits2 -> fruits2.genus()))));
        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    public void includeASecondDegreeParent() {
        Fruit apple = fruits.where().name().is("apple").first(fruit -> fruit.genus(genus -> genus.family())).get();
        Assertions.assertThat(apple.genus).isNotNull()
        ;
        Assertions.assertThat(apple.genus.family).isNotNull();
        Assertions.assertThat(apple.genus.family.name).isEqualTo("Rosaceae");
    }

    @Test
    public void includeMultipleSecondDegreeParent() {
        Fruit apple = fruits.where().name().is("apple").first(fruit -> fruit.genus(genus -> genus.family(), genus -> genus.fruits())).get();
        Assertions.assertThat(apple.genus).isNotNull();
        Assertions.assertThat(apple.genus.family).isNotNull();
        Assertions.assertThat(apple.genus.family.name).isEqualTo("Rosaceae");
        Assertions.assertThat(apple.genus.fruits.size()).isEqualTo(1);
    }

    @Test
    public void includeASecondDegreeParentAndAMatchingGrandChild() {
        Fruit apple = fruits.where().name().is("apple").first(fruit ->
            fruit.genus(genus ->
                genus.family(family ->
                    family.genuses(allGenuses ->
                        allGenuses.fruits())))).get();
        Assertions.assertThat(apple.genus).isNotNull();
        Assertions.assertThat(apple.genus.family).isNotNull();
        Assertions.assertThat(apple.genus.family.name).isEqualTo("Rosaceae");
        Assertions.assertThat(apple.genus.family.genuses.get(0).fruits.get(0).id).isEqualTo(apple.id);
        Assertions.assertThat(apple.genus.family.genuses.get(1).fruits.get(0).id).isEqualTo(3);
    }

    @Test
    public void embeddedObject() {
        Information information = new Information();
        information.put("something", "value");
        families.newFamily().name("test").information(information).create();
        Family family = families.where().id().is(3).first().get();
        Assertions.assertThat(family.information.get("something")).isEqualTo("value");
    }

    @Test
    public void cascadePersist() {
        // adding one to an already existing entity
        Genus prunus = genuses.where().id().is(3).first(genus -> genus.fruits()).get();
        Fruit plum = new Fruit();
        plum.name = "plum";
        plum.price = 10;
        prunus.fruits.add(plum);
        genuses.update(prunus);
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.id().desc()).first().get().name).isEqualTo("plum");

        // persisting with another new object
        Genus pyrus = new Genus();
        pyrus.name = "pyrus";
        pyrus.family = families.where().name().is("Rosaceae").first().get();
        pyrus.fruits = new ArrayList<>();
        Fruit pear = new Fruit();
        pear.name = "pear";
        pear.price = 4;
        pyrus.fruits.add(pear);
        genuses.create(pyrus);
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.id().desc()).first().get().name).isEqualTo("pear");
    }

    @Test
    public void cascadeMerge() {
        // One
        Genus prunus = genuses.where().id().is(3).first(genus -> genus.fruits()).get();
        prunus.fruits.get(0).name = "plum";
        genuses.update(prunus);
        assertThat(fruits.where().id().is(3).first().get().name).isEqualTo("plum");

        // Many
        genuses.update(genuses.list(genus -> genus.fruits()).stream().peek(genus -> genus.fruits.forEach(fruit -> fruit.name = "test")));
        assertThat(fruits.where().name().is("test").count()).isEqualTo(3);

        // Not cascaded
        families.update(families.list(family -> family.genuses()).stream().peek(family -> family.genuses.forEach(genus -> genus.name = "test")));
        assertThat(genuses.where().name().is("test").count()).isEqualTo(0);
    }

    @Test
    public void cascadeRemove() {
        assertThat(families.where().name().is("Musaceae").delete()).isEqualTo(1);
        assertThat(genuses.where().name().is("musa").first()).isEmpty();
        Assertions.assertThat(families.delete(2)).isTrue();
        assertThat(genuses.where().name().is("malus").first()).isEmpty();
    }

    @Test
    public void rawManualCascade() {
        EntityManager<Family> familyManager = EntitySteps.entityManagerOf(Family.class);
        // preventing the cascade remove to kick in
        assertThat(familyManager.where("name").is("Musaceae").delete(Includes::none)).isEqualTo(1);
        assertThat(genuses.where().name().is("musa").first().get().family).isNull();
    }

    @Test
    public void manualCascadeRemove() {
        assertThat(families.where().name().is("Musaceae").delete(Families.Includes::none)).isEqualTo(1);
        assertThat(genuses.where().name().is("musa").first().get().family).isNull();
        assertThat(genuses.where().name().is("musa").delete()).isEqualTo(1);
        assertThat(fruits.where().name().is("banana").count()).isEqualTo(0);
        assertThat(fruits.where().name().is("peach").delete(fruit -> fruit.genus())).isEqualTo(1);
        assertThat(genuses.where().name().is("prunus").count()).isEqualTo(0);
    }

    @Test
    public void safeJoinTableNameGenerations() {
        assertThat(JoinTables.generateName(families.model().member("genuses"), families.model())).isEqualTo("io.semla.model.FamilyGenus");
        Javassist.getOrCreate("io.semla.model.FamilyGenus", UnaryOperator.identity());
        assertThat(JoinTables.generateName(families.model().member("genuses"), families.model())).isEqualTo("io.semla.model.Family_Genus");
        Javassist.getOrCreate("io.semla.model.Family_Genus", UnaryOperator.identity());
        assertThat(JoinTables.generateName(families.model().member("genuses"), families.model())).isEqualTo("io.semla.model.Family_Genuses");

        assertThat(JoinTables.generateName(genuses.model().member("family"), genuses.model())).isEqualTo("io.semla.model.GenusFamily");
        Javassist.getOrCreate("io.semla.model.GenusFamily", UnaryOperator.identity());
        assertThat(JoinTables.generateName(genuses.model().member("family"), genuses.model())).isEqualTo("io.semla.model.Genus_Family");
        Javassist.getOrCreate("io.semla.model.Genus_Family", UnaryOperator.identity());
        assertThat(JoinTables.generateName(genuses.model().member("family"), genuses.model())).isEqualTo("io.semla.model.Genus__Family");
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }
}



