package io.semla.persistence;

import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.model.*;
import io.semla.reflect.Methods;
import io.semla.serialization.json.Json;
import io.semla.util.Lists;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static io.semla.serialization.json.JsonSerializer.PRETTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TypedEntityManagerTest {

    @Test
    public void typedManagerTest() {
        Families families = EntitySteps.getInstance(Families.class);
        Family musaceae = families.newFamily().name("Musaceae").create();
        Family rosaceae = families.newFamily().name("Rosaceae").create();

        GenusManager genuses = EntitySteps.getInstance(GenusManager.class);
        Genus musa = genuses.newGenus().name("musa").family(musaceae).create();
        Genus malus = genuses.newGenus().name("malus").family(rosaceae).create();

        FruitManager fruits = EntitySteps.getInstance(FruitManager.class);
        fruits.newFruit().name("banana").price(1).genus(musa).create();

        assertThat(fruits.where().id().is(1).first().get().name).isEqualTo("banana");
        assertThat(fruits.where().name().contains("a").and().price().is(1).count()).isEqualTo(1L);
        assertThat(fruits.where().name().contains("a").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().doesNotContain("p").list().size()).isEqualTo(1);
        assertThat(fruits.set().price(2).where().name().is("banana").and().price().is(1).patch()).isEqualTo(1L);

        Fruit banana = fruits.where().name().is("banana").first(fruit -> fruit.genus()).get();
        Assertions.assertThat(banana.genus).isNotNull();
        Assertions.assertThat(banana.genus.name).isEqualTo("musa");

        banana = fruits.where().name().is("banana").first(fruit -> fruit.genus(genus -> genus.family(), genus -> genus.fruits())).get();

        Assertions.assertThat(banana.genus).isNotNull();
        Assertions.assertThat(banana.genus.family).isNotNull();
        Assertions.assertThat(banana.genus.family.name).isEqualTo("Musaceae");
        Assertions.assertThat(banana.genus.fruits.size()).isEqualTo(1);
        Assertions.assertThat(banana.genus.fruits.get(0).id).isEqualTo(banana.id);

        fruits.newFruit().name("apple").price(2).genus(malus).create();

        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().desc()).first().get().name).isEqualTo("banana");
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().asc()).first().get().name).isEqualTo("apple");
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().asc()).startAt(0).limitTo(1).list().size()).isEqualTo(1);

        assertThat(fruits.where().name().not("banana").list().size()).isEqualTo(1);

        // get
        assertThat(fruits.get(1, fruit -> fruit.genus()).get().id).isEqualTo(1);
        assertThat(fruits.cached().get(1, fruit -> fruit.genus()).get().id).isEqualTo(1);
        assertThat(fruits.cachedFor(Duration.ofSeconds(30)).get(1, fruit -> fruit.genus()).get().id).isEqualTo(1);
        assertThat(fruits.invalidateCache().get(1, fruit -> fruit.genus()).get().id).isEqualTo(1);
        fruits.evictCache().get(1, fruit -> fruit.genus());

        // type safe id
        assertThat(fruits.where().genus().hasKey(1).count()).isEqualTo(1);
        assertThat(fruits.where().genus().hasNotKey(1).count()).isEqualTo(1);
        assertThat(fruits.where().genus().hasKeyIn(1, 2, 3).count()).isEqualTo(2);
        assertThat(fruits.where().genus().hasKeyIn(Lists.of(1, 2, 3)).count()).isEqualTo(2);
        assertThat(fruits.where().genus().hasKeyNotIn(1, 2, 3).count()).isEqualTo(0);
        assertThat(fruits.where().genus().hasKeyNotIn(Lists.of(1, 2, 3)).count()).isEqualTo(0);

        // in, not In
        assertThat(fruits.where().name().in("banana").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().notIn("banana").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().notIn(Lists.of("banana")).list().size()).isEqualTo(1);

        // containedIn, notContainedIn
        assertThat(fruits.where().name().containedIn("3 apples or peaches per day keeps the doctor away").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().notContainedIn("3 apples or peaches per day keeps the doctor away").list().size()).isEqualTo(1);

        // like, notLike
        assertThat(fruits.where().name().like("%app%").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().notLike("%app%").list().size()).isEqualTo(1);

        // contains, doesNotContain
        assertThat(fruits.where().name().contains("app").list().size()).isEqualTo(1);
        assertThat(fruits.where().name().doesNotContain("app").list().size()).isEqualTo(1);

        // null, notNull
        fruits.newFruit().price(3).create();

        assertThat(fruits.where().name().is(null).list().size()).isEqualTo(1);
        assertThat(fruits.where().name().not(null).list().size()).isEqualTo(2);

        // < >
        assertThat(fruits.where().price().greaterThan(4).list().size()).isEqualTo(0);
        assertThat(fruits.where().price().greaterOrEquals(3).list().size()).isEqualTo(1);
        assertThat(fruits.where().price().lessOrEquals(2).list().size()).isEqualTo(2);

        // patch
        assertThat(fruits.unwrap().set("genus", "2").where("genus").is(1).patch()).isEqualTo(1L);

        // delete
        assertThat(fruits.where().price().lessThan(5).delete()).isEqualTo(3L);
        assertThat(fruits.count()).isEqualTo(0L);
    }

    @Test
    public void async_typedManagerTest() {
        Families families = EntitySteps.getInstance(Families.class);
        Family musaceae = families.newFamily().name("Musaceae").async().create()
            .toCompletableFuture().join();
        Family rosaceae = families.newFamily().name("Rosaceae").async().create()
            .toCompletableFuture().join();

        GenusManager genuses = EntitySteps.getInstance(GenusManager.class);
        Genus musa = genuses.newGenus().name("musa").family(musaceae).async().create()
            .toCompletableFuture().join();
        Genus malus = genuses.newGenus().name("malus").family(rosaceae).async().create()
            .toCompletableFuture().join();

        FruitManager fruits = EntitySteps.getInstance(FruitManager.class);
        fruits.newFruit().name("banana").price(1).genus(musa).async().create()
            .toCompletableFuture().join();

        fruits.where().id().is(1).async().first()
            .thenApply(Optional::get)
            .thenAccept(fruit -> assertThat(fruit.name).isEqualTo("banana"))
            .toCompletableFuture().join();

        fruits.where().name().contains("a").and().price().is(1).async().count()
            .thenAccept(count -> assertThat(count).isEqualTo(1L))
            .toCompletableFuture().join();

        fruits.where().name().contains("a").and().price().is(1).async().list()
            .thenAccept(list -> assertThat(list).hasSize(1))
            .toCompletableFuture().join();

        fruits.set().price(2).where().name().is("banana").and().price().is(1).async().patch()
            .thenAccept(count -> assertThat(count).isEqualTo(1L))
            .toCompletableFuture().join();

        Fruit banana = fruits.where().name().is("banana").async().first(fruit -> fruit.genus())
            .toCompletableFuture().join().get();
        Assertions.assertThat(banana.genus).isNotNull();
        Assertions.assertThat(banana.genus.name).isEqualTo("musa");

        banana = fruits.where().name().is("banana").async().first(fruit -> fruit.genus(genus -> genus.family(), genus -> genus.fruits()))
            .toCompletableFuture().join().get();

        Assertions.assertThat(banana.genus).isNotNull();
        Assertions.assertThat(banana.genus.family).isNotNull();
        Assertions.assertThat(banana.genus.family.name).isEqualTo("Musaceae");
        Assertions.assertThat(banana.genus.fruits.size()).isEqualTo(1);
        Assertions.assertThat(banana.genus.fruits.get(0).id).isEqualTo(banana.id);

        fruits.newFruit().name("apple").price(2).genus(malus).async().create()
            .toCompletableFuture().join();

        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().desc()).async().first()
            .toCompletableFuture().join().get().name).isEqualTo("banana");
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().asc()).async().first()
            .toCompletableFuture().join().get().name).isEqualTo("apple");
        Assertions.assertThat(fruits.orderedBy(FruitManager.Sort.name().asc()).startAt(0).limitTo(1).async().list()
            .toCompletableFuture().join().size()).isEqualTo(1);

        assertThat(fruits.where().name().not("banana").async().list().toCompletableFuture().join().size()).isEqualTo(1);

        // get
        assertThat(fruits.async().get(1, fruit -> fruit.genus()).toCompletableFuture().join().get().id).isEqualTo(1);

        // cached
        assertThat(fruits.cached().async().get(1, fruit -> fruit.genus()).toCompletableFuture().join().get().id).isEqualTo(1);
        assertThat(fruits.cachedFor(Duration.ofSeconds(30)).async().get(1, fruit -> fruit.genus()).toCompletableFuture().join().get().id).isEqualTo(1);
        assertThat(fruits.invalidateCache().async().get(1, fruit -> fruit.genus()).toCompletableFuture().join().get().id).isEqualTo(1);
        fruits.evictCache().async().get(1, fruit -> fruit.genus()).toCompletableFuture().join();

        // count
        assertThat(fruits.where().genus().hasKey(1).async().count().toCompletableFuture().join()).isEqualTo(1);

        // list
        assertThat(fruits.where().name().in("banana").async().list().toCompletableFuture().join().size()).isEqualTo(1);

        // patch
        assertThat(fruits.unwrap().set("genus", "2").where("genus").is(1).async().patch().toCompletableFuture().join()).isEqualTo(1L);

        // delete
        assertThat(fruits.where().price().lessThan(5).async().delete().toCompletableFuture().join()).isEqualTo(2L);

        assertThat(fruits.async().count().toCompletableFuture().join()).isEqualTo(0L);
    }

    @Test
    public void testUser() {
        UserManager users = EntitySteps.getInstance(UserManager.class);

        Methods.findMethod(UserManager.Create.class, "id", int.class).ifPresent(
            method -> fail("id method should not exist as id is annotated with @GeneratedValue")
        );

        Methods.findMethod(UserManager.Setter.class, "id", int.class).ifPresent(
            method -> fail("id method should not exist as id is annotated with @GeneratedValue")
        );

        Methods.findMethod(UserManager.Setter.class, "created", int.class).ifPresent(
            method -> fail("created method should not exist as created is annotated with @Column(updatable = false)")
        );

        Methods.findMethod(UserManager.Select.class, "first", Consumer[].class).ifPresent(
            method -> fail("user doesn't have any relation, it shouldn't have a handler")
        );

        Methods.findMethod(UserManager.class, "first", Consumer[].class).ifPresent(
            method -> fail("user doesn't have any relation, it shouldn't have a handler")
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(315532800000L);
        Calendar lastLogin = Calendar.getInstance();
        lastLogin.setTimeInMillis(3600);
        LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        User user = users.newUser("bob", new Date(347155200000L))
            .created(1)
            .additionalNames(Lists.of("steve", "dave"))
            .isCool(true)
            .initial('b')
            .mask((byte) 10)
            .powers(new int[]{1, 2, 3, 4, 5, 6})
            .age((short) 23)
            .percentage(.4f)
            .height(175.4)
            .lastSeen(new Date(65000))
            .lastLogin(lastLogin)
            .sqlDate(new java.sql.Date(631152000000L))
            .sqlTime(new Time(6000))
            .sqlTimestamp(new Timestamp(1532295982561L))
            .bigInteger(BigInteger.ONE)
            .bigDecimal(BigDecimal.valueOf(10.1))
            .calendar(calendar)
            .instant(Instant.ofEpochSecond(1))
            .localDateTime(localDateTime)
            .nickname(Optional.of("zogzog"))
            .type(User.Type.user)
            .eyecolor(User.EyeColor.brown)
            .create();

        User reloaded = users.where().id().is(user.id).first().orElseThrow();
        assertThat(Json.write(reloaded, PRETTY)).isEqualTo(Json.write(user, PRETTY));

        assertThat(users.where().created().is(1l).first().get().id).isEqualTo(user.id);
        assertThat(users.where().name().is("bob").first().get().id).isEqualTo(user.id);
        assertThat(users.where().additionalNames().is(Lists.of("steve", "dave")).first().get().id).isEqualTo(user.id);
        assertThat(users.where().isCool().is(true).first().get().id).isEqualTo(user.id);
        assertThat(users.where().initial().is('b').first().get().id).isEqualTo(user.id);
        assertThat(users.where().mask().is((byte) 10).first().get().id).isEqualTo(user.id);
        assertThat(users.where().powers().is(new int[]{1, 2, 3, 4, 5, 6}).first().get().id).isEqualTo(user.id);
        assertThat(users.where().age().is((short) 23).first().get().id).isEqualTo(user.id);
        assertThat(users.where().percentage().is(.4f).first().get().id).isEqualTo(user.id);
        assertThat(users.where().percentage().not(.3f).first().get().id).isEqualTo(user.id);
        assertThat(users.where().height().is(175.4).first().get().id).isEqualTo(user.id);
        assertThat(users.where().lastLogin().is(lastLogin).first().get().id).isEqualTo(user.id);
        assertThat(users.where().sqlDate().is(new java.sql.Date(631152000000L)).first().get().id).isEqualTo(user.id);
        assertThat(users.where().sqlTime().is(new Time(6000)).first().get().id).isEqualTo(user.id);
        assertThat(users.where().sqlTimestamp().is(new Timestamp(1532295982561L)).first().get().id).isEqualTo(user.id);
        assertThat(users.where().bigInteger().is(BigInteger.ONE).first().get().id).isEqualTo(user.id);
        assertThat(users.where().bigDecimal().is(BigDecimal.valueOf(10.1)).first().get().id).isEqualTo(user.id);
        assertThat(users.where().calendar().is(calendar).first().get().id).isEqualTo(user.id);
        assertThat(users.where().instant().is(Instant.ofEpochSecond(1)).first().get().id).isEqualTo(user.id);
        assertThat(users.where().localDateTime().is(localDateTime).first().get().id).isEqualTo(user.id);
        assertThat(users.where().nickname().is(Optional.of("zogzog")).first().get().id).isEqualTo(user.id);
        assertThat(users.where().type().is(User.Type.user).first().get().id).isEqualTo(user.id);
        assertThat(users.where().eyecolor().is(User.EyeColor.brown).first().get().id).isEqualTo(user.id);

        String serialized = Json.write(user, PRETTY);
        Json.read(serialized, User.class);

        assertThat(users.get(1).get().name).isEqualTo("bob");
    }

    @Test
    public void indexed() {
        IndexedUserManager indexedUsers = EntitySteps.getInstance(IndexedUserManager.class);
        Methods.findMethod(IndexedUserManager.PredicateHandler.class, "age").ifPresent(
            method -> fail("age method should not exist as the member is not indexed")
        );
        indexedUsers.newIndexedUser(UUID.randomUUID()).age(23).name("bob").create();
        indexedUsers.where().name().is("bob").first().get();
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }
}
