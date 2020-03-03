package io.semla.query;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.Genus;
import io.semla.model.Player;
import io.semla.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.semla.cucumber.steps.EntitySteps.newContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QueryTest {

    @Before
    public void init() {
        Query.create(
            Player.with(1, "test", 9001),
            Player.with(2, "test2", 9002),
            Player.with(3, "test3", 9003)
        ).in(newContext());
    }

    @Test
    public void unknown() {
        assertThatThrownBy(() -> Query.parse("split the players")).hasMessage("unknown query type: split");
    }

    @Test
    public void get() {
        assertThat(Query.get(Player.class, 1).toString()).isEqualTo("get the player where id is 1");
        assertThat(Query.<Player, Optional<Player>>parse("get the player where id is 1").in(newContext()).get().name).isEqualTo("test");
        assertThat(Query.get(Player.class, Lists.of(1, 2)).toString()).isEqualTo("get the players where id in [1,2]");
        assertThat(Query.<Player, Map<Integer, Player>>parse("get the players where id in [1, 2]").in(newContext()).size()).isEqualTo(2);
        assertThat(Query.get(Genus.class, 1, genus -> genus.include("fruits")).toString())
            .isEqualTo("get the genus where id is 1 including its fruits");
        assertThat(Query.parse("get the genus where id is 1 including its fruits").toString()).isEqualTo("get the genus where id is 1 including its fruits");
    }

    @Test
    public void count() {
        assertThat(Query.count(Player.class).toString()).isEqualTo("count the players");
        assertThat(Query.count(Player.class, player -> player.where("name").is("test2")).toString())
            .isEqualTo("count the players where name is \"test2\"");
        assertThat(Query.<Player, Long>parse("count the players where name not \"test\"").in(newContext())).isEqualTo(2L);
        assertThat(Query.count(Predicates.of(Player.class).where("name").is("test2")).toString())
            .isEqualTo("count the players where name is \"test2\"");
    }

    @Test
    public void delete() {
        assertThat(Query.<Player, Long>parse("delete the players where name not \"test\"").in(newContext())).isEqualTo(2L);
        assertThat(Query.delete(Player.class, player -> player.where("name").is("test")).toString())
            .isEqualTo("delete the players where name is \"test\"");
        assertThat(Query.delete(
            Genus.class,
            genus -> genus.where("name").is("prunus"),
            genus -> genus.startAt(2).limitTo(1)
        ).toString()).isEqualTo("delete the genera where name is \"prunus\" start at 2 limit to 1 including their fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.delete(
            Genus.class,
            genus -> genus.where("name").is("prunus"),
            genus -> genus.startAt(2).limitTo(1),
            genus -> genus.include("fruits")
        ).toString()).isEqualTo("delete the genera where name is \"prunus\" start at 2 limit to 1 including their fruits");
    }

    @Test
    public void deleteByKey() {
        assertThat(Query.delete(Genus.class).toString()).isEqualTo("delete the genera including their fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.delete(Genus.class, 1).toString()).isEqualTo("delete the genus where id is 1 including its fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.delete(Genus.class, 1, Includes::none).toString()).isEqualTo("delete the genus where id is 1");
        assertThat(Query.delete(Genus.class, Lists.of(1, 2)).toString())
            .isEqualTo("delete the genera where id in [1,2] including their fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.delete(Genus.class, Lists.of(1, 2), Includes::none).toString()).isEqualTo("delete the genera where id in [1,2]");
        assertThatThrownBy(() -> Query.parse("delete the genera [1, 2]")).hasMessage("unexpected tokens: [1, 2]");
        assertThat(Query.parse("delete the genera where id in [1,2] including their fruits[ALL, DELETE_ORPHANS]").toString())
            .isEqualTo("delete the genera where id in [1,2] including their fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.parse("delete the genus where id is 1 including its fruits[ALL, DELETE_ORPHANS]").toString())
            .isEqualTo("delete the genus where id is 1 including its fruits[ALL, DELETE_ORPHANS]");
        assertThat(Query.parse("delete all the genera").toString()).isEqualTo("delete the genera including their fruits[ALL, DELETE_ORPHANS]");
    }

    @Test
    public void update() {
        Query<Player, Player> updateOne = Query.update(Player.with(1, "test", 10000));
        assertThat(updateOne.in(newContext()).score).isEqualTo(10000);
        assertThat(updateOne.toString()).isEqualTo("update the player -> {\"id\":1,\"name\":\"test\",\"score\":10000}");
        assertThat(Query.<Player, Player>parse("update the player -> {\"id\":1,\"name\":\"test\",\"score\":20000}").in(newContext()).score).isEqualTo(20000);

        Query<Player, List<Player>> updateMany = Query.update(Player.with(2, "test2", 10002), Player.with(3, "test3", 10003));
        assertThat(updateMany.toString())
            .isEqualTo("update the players -> [{\"id\":2,\"name\":\"test2\",\"score\":10002},{\"id\":3,\"name\":\"test3\",\"score\":10003}]");
        assertThat(updateMany.in(newContext()).size()).isEqualTo(2);
        assertThat(Query.<Player, List<Player>>parse(
            "update the players -> [{\"id\":2,\"name\":\"test2\",\"score\":10002},{\"id\":3,\"name\":\"test3\",\"score\":10003}]"
        ).in(newContext()).size()).isEqualTo(2);

        assertThatThrownBy(() -> Query.parse("update the players")).hasMessage("payload is required for an update");
    }

    @Test
    public void patch() {
        Query<Player, Long> patch = Query.patch(
            Values.of(Player.class).with("score", 3),
            player -> player.where("id").not(1),
            pagination -> pagination.startAt(1).limitTo(1).orderedBy("name")
        );
        assertThat(patch.toString())
            .isEqualTo("patch the players where id not 1 ordered by name start at 1 limit to 1 with {\"score\":3}");
        assertThat(patch.in(newContext())).isEqualTo(1L);
        assertThat(Query.parse("patch the players where id not 1 ordered by name start at 1 limit to 1 with {\"score\":3}").in(newContext())).isEqualTo(1L);

        assertThat(Query.patch(
            Values.of(Player.class).with("score", 3),
            player -> player.where("id").not(1)
        ).toString()).isEqualTo("patch the players where id not 1 with {\"score\":3}");
    }

    @Test
    public void create() {
        Query<Player, Player> createOne = Query.create(Player.with(4, "test4", 9001));
        assertThat(createOne.toString()).isEqualTo("create the player -> {\"id\":4,\"name\":\"test4\",\"score\":9001}");
        assertThat(createOne.in(newContext()).name).isEqualTo("test4");
        assertThat(Query.<Player, Player>parse("create the player -> {\"id\":7,\"name\":\"test7\",\"score\":9001}").in(newContext()).name).isEqualTo("test7");

        Query<Player, List<Player>> createMany = Query.create(Player.with(5, "test5", 9002), Player.with(6, "test6", 9003));

        assertThat(createMany.toString()).isEqualTo("create the players -> [{\"id\":5,\"name\":\"test5\",\"score\":9002},{\"id\":6,\"name\":\"test6\",\"score\":9003}]");
        assertThat(createMany.in(newContext()).size()).isEqualTo(2);
        assertThat(Query.<Player, List<Player>>parse(
            "create the players -> [{\"id\":8,\"name\":\"test8\",\"score\":9002},{\"id\":9,\"name\":\"test9\",\"score\":9003}]"
        ).in(newContext()).size()).isEqualTo(2);

        assertThatThrownBy(() -> Query.parse("create the players")).hasMessage("payload is required for a create");
    }

    @Test
    public void select() {
        assertThat(Query.first(Player.class).in(newContext()).get().name).isEqualTo("test");
        assertThat(Query.first(
            Genus.class,
            genus -> genus.where("name").is("prunus"),
            genus -> genus.startAt(2).limitTo(1),
            genus -> genus.include("fruits")
        ).toString()).isEqualTo("fetch the first genus where name is \"prunus\" start at 2 limit to 1 including its fruits");

        assertThat(Query.parse("fetch the first genus where name is \"prunus\" start at 2 limit to 1 including its fruits").toString())
            .isEqualTo("fetch the first genus where name is \"prunus\" start at 2 limit to 1 including its fruits");

        assertThat(Query.list(Player.class).in(newContext()).size()).isEqualTo(3);
        assertThat(Query.list(
            Genus.class,
            genus -> genus.where("name").is("prunus"),
            genus -> genus.startAt(2).limitTo(1),
            genus -> genus.include("fruits")
        ).toString()).isEqualTo("list all the genera where name is \"prunus\" start at 2 limit to 1 including their fruits");

        assertThat(Query.parse("list all the genera where name is \"prunus\" start at 2 limit to 1 including their fruits").toString())
            .isEqualTo("list all the genera where name is \"prunus\" start at 2 limit to 1 including their fruits");
    }


    @After
    public void after() {
        EntitySteps.cleanup();
    }


}
