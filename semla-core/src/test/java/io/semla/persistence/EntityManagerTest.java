package io.semla.persistence;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.exception.InvalidQueryException;
import io.semla.model.IndexedUser;
import io.semla.model.Player;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.util.ImmutableMap;
import io.semla.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.util.TypeLiteral;
import javax.persistence.PersistenceException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EntityManagerTest {

    private EntityManager<Player> players;

    @Before
    public void init() {
        players = EntitySteps.getInstance(new TypeLiteral<EntityManager<Player>>() {});
        players.newInstance().with("id", 1).with("name", "test").with("score", 9001).create();
        players.newInstance().with("id", 2).with("name", "test2").with("score", 9002).create();
        players.newInstance().with("id", 3).with("name", "test3").with("score", 9003).create();
    }

    @Test
    public void get() {
        assertThat(players.get(1).get().id).isEqualTo(1);
        assertThat(players.get(1, 2).size()).isEqualTo(2);
    }

    @Test
    public void first() {
        assertThat(players.first().get().id).isEqualTo(1);
    }

    @Test
    public void list() {
        assertThat(players.list().size()).isEqualTo(3);
    }

    @Test
    public void count() {
        assertThat(players.count()).isEqualTo(3L);
    }

    @Test
    public void create() {
        assertThat(players.create(Player.with(4, "test4", 9004)).id).isEqualTo(4);
        assertThat(players.create(Player.with(5, "test5", 9004), Player.with(6, "test6", 9005)).size()).isEqualTo(2);
        assertThat(players.create(Lists.of(Player.with(7, "test7", 9004), Player.with(8, "test8", 9005))).size()).isEqualTo(2);
        assertThat(players.create(Stream.of(Player.with(9, "test9", 9004), Player.with(10, "test10", 9005))).size()).isEqualTo(2);

        assertThatThrownBy(() -> players.create(Player.with(0, "rak", 1000)))
            .isInstanceOf(PersistenceException.class)
            .hasMessageStartingWith("entity has no primary key set");
    }

    @Test
    public void update() {
        assertThat(players.update(Player.with(3, "test3", 9004)).id).isEqualTo(3);
        assertThat(players.update(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004)).size()).isEqualTo(2);
        assertThat(players.update(Lists.of(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004))).size()).isEqualTo(2);
        assertThat(players.update(Stream.of(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004))).size()).isEqualTo(2);
    }

    @Test
    public void delete() {
        assertThat(players.delete(1)).isTrue();
        assertThat(players.delete(Lists.of(2))).isEqualTo(1);
        assertThat(players.delete(2, 3)).isEqualTo(1);
    }

    @Test
    public void where() {
        assertThat(players.where("name").is("test").first().isPresent()).isTrue();
        assertThat(players.where(Predicates.of(Player.class).where("name").is("test")).first().isPresent()).isTrue();
    }

    @Test
    public void orderedBy() {
        assertThat(players.orderedBy("name").first().get().name).isEqualTo("test");
        assertThat(players.orderedBy("name", Pagination.Sort.DESC).first().get().name).isEqualTo("test3");
    }

    @Test
    public void limit() {
        assertThat(players.limitTo(1).delete()).isEqualTo(1L);
    }

    @Test
    public void startAt() {
        assertThat(players.startAt(1).delete()).isEqualTo(2L);
    }

    @Test
    public void set() {
        assertThat(players.set("name", "bob").where("id").is(1).patch()).isEqualTo(1L);
        assertThat(players.set(ImmutableMap.of("name", "tom")).where("id").is(1).patch()).isEqualTo(1L);
        assertThatThrownBy(() -> players.set("id", 2).where("id").is(1).patch())
            .hasMessage("io.semla.model.Player.id is not updatable");
    }

    @Test
    public void indexed() {
        EntityManager<IndexedUser> indexedUsers = EntitySteps.entityManagerOf(IndexedUser.class);
        indexedUsers.newInstance().with("uuid", UUID.randomUUID()).with("age", 23).with("name", "bob").create();
        indexedUsers.where("name").is("bob").first().get();
        try {
            indexedUsers.where("age").is(23).first().get();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
        try {
            indexedUsers.where("age").is(23).set("age", 24).patch();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
        try {
            indexedUsers.where("age").is(23).delete();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
    }


    @After
    public void after() {
        EntitySteps.cleanup();
    }
}