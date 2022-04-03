package io.semla.persistence;

import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.exception.InvalidQueryException;
import io.semla.model.IndexedUser;
import io.semla.model.Player;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.reflect.TypeReference;
import io.semla.util.ImmutableMap;
import io.semla.util.Lists;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EntityManagerTest {

    private EntityManager<Integer, Player> players;

    @Before
    public void init() {
        players = EntitySteps.getInstance(new TypeReference<EntityManager<Integer, Player>>() {});
        players.newInstance().with("id", 1).with("name", "test").with("score", 9001).create();
        players.newInstance().with("id", 2).with("name", "test2").with("score", 9002).create();
        players.newInstance().with("id", 3).with("name", "test3").with("score", 9003)
            .async().create().toCompletableFuture().join();
    }

    @Test
    public void get() {
        assertThat(players.get(1).get().id).isEqualTo(1);
        assertThat(players.get(1, 2).size()).isEqualTo(2);
        assertThat(players.get(List.of(1, 2)).size()).isEqualTo(2);
    }

    @Test
    public void async_get() {
        players.async().get(1)
            .thenApply(Optional::get)
            .thenAccept(player -> assertThat(player.id).isEqualTo(1))
            .toCompletableFuture().join();
        players.async().get(1, 2)
            .thenAccept(playersById -> assertThat(playersById.size()).isEqualTo(2))
            .toCompletableFuture().join();
        players.async().get(List.of(1, 2))
            .thenAccept(playersById -> assertThat(playersById.size()).isEqualTo(2))
            .toCompletableFuture().join();

        players.async().get(1)
            .thenApply(Optional::get)
            .thenAccept(player -> assertThat(player.id).isEqualTo(1))
            .toCompletableFuture().join();
    }

    @Test
    public void first() {
        assertThat(players.first().get().id).isEqualTo(1);
    }

    @Test
    public void async_first() {
        players.async().first()
            .thenApply(Optional::get)
            .thenAccept(player -> assertThat(player.id).isEqualTo(1))
            .toCompletableFuture().join();
        players.async().first()
            .thenApply(Optional::get)
            .thenAccept(player -> assertThat(player.id).isEqualTo(1))
            .toCompletableFuture().join();
    }

    @Test
    public void list() {
        assertThat(players.list().size()).isEqualTo(3);
    }

    @Test
    public void async_list() {
        players.async().list()
            .thenAccept(list -> assertThat(list.size()).isEqualTo(3))
            .toCompletableFuture().join();
    }

    @Test
    public void count() {
        assertThat(players.count()).isEqualTo(3L);
    }

    @Test
    public void async_count() {
        players.async().count()
            .thenAccept(value -> assertThat(value).isEqualTo(3L))
            .toCompletableFuture().join();
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
    public void async_create() {
        players.async().create(Player.with(4, "test4", 9004))
            .thenAccept(player -> assertThat(player.id).isEqualTo(4))
            .toCompletableFuture().join();
        players.async().create(Player.with(5, "test5", 9004), Player.with(6, "test6", 9005))
            .thenAccept(results -> assertThat(results).hasSize(2))
            .toCompletableFuture().join();
        players.async().create(Lists.of(Player.with(7, "test7", 9004), Player.with(8, "test8", 9005)))
            .thenAccept(results -> assertThat(results).hasSize(2))
            .toCompletableFuture().join();
        players.async().create(Stream.of(Player.with(9, "test9", 9004), Player.with(10, "test10", 9005)))
            .thenAccept(results -> assertThat(results).hasSize(2))
            .toCompletableFuture().join();
        assertThatThrownBy(() -> players.async().create(Player.with(0, "rak", 1000)).toCompletableFuture().join())
            .getRootCause()
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
    public void async_update() {
        players.async().update(Player.with(3, "test3", 9004))
            .thenAccept(player -> assertThat(player.id).isEqualTo(3))
            .toCompletableFuture().join();
        players.async().update(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004))
            .thenAccept(result -> assertThat(result).hasSize(2))
            .toCompletableFuture().join();
        players.async().update(Lists.of(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004)))
            .thenAccept(result -> assertThat(result).hasSize(2))
            .toCompletableFuture().join();
        players.async().update(Stream.of(Player.with(2, "test2", 9004), Player.with(3, "test3", 9004)))
            .thenAccept(result -> assertThat(result).hasSize(2))
            .toCompletableFuture().join();
    }

    @Test
    public void delete() {
        assertThat(players.delete(1)).isTrue();
        assertThat(players.delete(Lists.of(2))).isEqualTo(1);
        assertThat(players.delete(2, 3)).isEqualTo(1);
    }

    @Test
    public void async_delete() {
        players.async().delete(1).thenAccept(result -> assertThat(result).isTrue()).toCompletableFuture().join();
        players.async().delete(Lists.of(2)).thenAccept(result -> assertThat(result).isEqualTo(1)).toCompletableFuture().join();
        players.async().delete(2, 3).thenAccept(result -> assertThat(result).isEqualTo(1)).toCompletableFuture().join();
    }


    @Test
    public void where() {
        assertThat(players.where("name").is("test").first().isPresent()).isTrue();
        assertThat(players.where(Predicates.of(Player.class).where("name").is("test")).first().isPresent()).isTrue();
    }

    @Test
    public void async_where() {
        players.where("name").is("test").async().count()
            .thenAccept(count -> assertThat(count).isEqualTo(1))
            .toCompletableFuture().join();
        players.where("name").is("test").async().first()
            .thenAccept(player -> assertThat(player).isPresent())
            .toCompletableFuture().join();
    }

    @Test
    public void orderedBy() {
        assertThat(players.orderedBy("name").first().get().name).isEqualTo("test");
        assertThat(players.orderedBy("name", Pagination.Sort.DESC).first().get().name).isEqualTo("test3");
    }

    @Test
    public void async_orderedBy() {
        players.orderedBy("name").async().first()
            .thenApply(Optional::get)
            .thenAccept(player -> assertThat(player.name).isEqualTo("test"))
            .toCompletableFuture().join();
    }

    @Test
    public void limit() {
        assertThat(players.limitTo(1).delete()).isEqualTo(1L);
    }

    @Test
    public void async_limit() {
        players.limitTo(1).async().delete()
            .thenAccept(count -> assertThat(count).isEqualTo(1L))
            .toCompletableFuture().join();
    }

    @Test
    public void startAt() {
        assertThat(players.startAt(1).delete()).isEqualTo(2L);
    }

    @Test
    public void async_startAt() {
        players.startAt(1).async().delete()
            .thenAccept(count -> assertThat(count).isEqualTo(2L))
            .toCompletableFuture().join();
    }

    @Test
    public void set() {
        assertThat(players.set("name", "bob").where("id").is(1).patch()).isEqualTo(1L);
        assertThat(players.set(ImmutableMap.of("name", "tom")).where("id").is(1).patch()).isEqualTo(1L);
        assertThatThrownBy(() -> players.set("id", 2).where("id").is(1).patch())
            .hasMessage("io.semla.model.Player.id is not updatable");
    }

    @Test
    public void async_set() {
        players.set("name", "bob").where("id").is(1).async().patch()
            .thenAccept(count -> assertThat(count).isEqualTo(1L))
            .toCompletableFuture().join();
    }


    @Test
    public void indexed() {
        EntityManager<UUID, IndexedUser> indexedUsers = EntitySteps.entityManagerOf(IndexedUser.class);
        indexedUsers.newInstance().with("uuid", UUID.randomUUID()).with("age", 23).with("name", "bob").create();
        indexedUsers.where("name").is("bob").first().get();
        try {
            indexedUsers.where("age").is(23).first().get();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
        try {
            indexedUsers.where("age").is(23).set("age", 24).patch();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
        try {
            indexedUsers.where("age").is(23).delete();
            Assert.fail("should have failed");
        } catch (InvalidQueryException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("age is not indexed and this entityManager requires all queried properties to be indexed!");
        }
    }


    @After
    public void after() {
        EntitySteps.cleanup();
    }
}
