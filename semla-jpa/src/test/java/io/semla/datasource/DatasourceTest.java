package io.semla.datasource;

import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.model.Player;
import io.semla.model.VersionedEntity;
import io.semla.model.VersionedEntityManager;
import io.semla.query.Pagination;
import io.semla.query.Predicates;
import io.semla.query.Values;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatasourceTest {

    protected Datasource<Player> players;

    @Before
    public void before() {
        players = EntitySteps.datasourceOf(Player.class);
        players.create(Player.with(1, "bob", 100));
        players.create(Player.with(2, "tom", 200));
        players.create(Player.with(3, "lea", 400));
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }

    @Test
    public void entityAlreadyExists() {
        assertThatThrownBy(() -> players.create(Player.with(1, "bob", 100)))
            .isInstanceOf(EntityExistsException.class)
            .hasMessage("entity 'player' with key '1' already exist!");
    }

    @Test
    public void entityNotFound() {
        assertThatThrownBy(() -> players.update(Player.with(4, "rak", 1000)))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("entity 'player' with key '4' doesn't exist!");
    }

    @Test
    public void first() {
        assertThat(players.first().get().id).isEqualTo(1);
        assertThat(players.first(Predicates.of(Player.class).where("name").is("lea")).get().id).isEqualTo(3);
    }

    @Test
    public void list() {
        assertThat(players.list(Predicates.of(Player.class).where("name").contains("o")).size()).isEqualTo(2);
    }

    @Test
    public void patch() {
        assertThat(players.patch(
            Values.of(Player.class).with("score", 200),
            Predicates.of(Player.class).where("name").is("lea")
        )).isEqualTo(1L);
        assertThat(players.count(Predicates.of(Player.class).where("score").is(200))).isEqualTo(2L);
        assertThat(players.patch(
            Values.of(Player.class).with("score", 400),
            Predicates.of(Player.class).where("score").is(200),
            Pagination.of(Player.class).limitTo(1)
        )).isEqualTo(1L);
    }

    @Test
    public void delete() {
        assertThat(players.delete(Predicates.of(Player.class).where("score").lessThan(300), Pagination.of(Player.class).limitTo(1))).isEqualTo(1L);
        assertThat(players.delete(Predicates.of(Player.class).where("score").is(500))).isEqualTo(0L);
        assertThat(players.deleteAll()).isEqualTo(2L);
    }

    @Test
    public void count() {
        assertThat(players.count()).isEqualTo(3L);
        assertThat(players.count(Predicates.of(Player.class).where("score").lessThan(300))).isEqualTo(2L);
        assertThat(players.count(Predicates.of(Player.class).where("score").is(400))).isEqualTo(1L);
        assertThat(players.count(Predicates.of(Player.class).where("name").contains("e"))).isEqualTo(1L);
        assertThat(players.count(Predicates.of(Player.class).where("name").like("%a").and("score").is(400))).isEqualTo(1L);
        assertThat(players.count(Predicates.of(Player.class).where("name").notLike("%o%"))).isEqualTo(1L);
    }

    @Test
    public void ordered() {
        List<Player> list = players.list(Pagination.of(Player.class).orderedBy("name"));
        Assertions.assertThat(list.get(0).name).isEqualTo("bob");
        Assertions.assertThat(list.get(1).name).isEqualTo("lea");
        Assertions.assertThat(list.get(2).name).isEqualTo("tom");
    }

    @Test
    public void orderedDesc() {
        List<Player> list = players.list(Pagination.of(Player.class).orderedBy("score", Pagination.Sort.DESC));
        Assertions.assertThat(list.get(0).score).isEqualTo(400);
        Assertions.assertThat(list.get(1).score).isEqualTo(200);
        Assertions.assertThat(list.get(2).score).isEqualTo(100);
    }

    @Test
    public void startAtAndLimit() {
        Optional<Player> player = players.first(Pagination.of(Player.class).orderedBy("score", Pagination.Sort.DESC).startAt(1).limitTo(1));
        Assertions.assertThat(player).isPresent();
        Assertions.assertThat(player.get().score).isEqualTo(200);
    }

    @Test
    public void versioned() {
        VersionedEntityManager versionedEntities = EntitySteps.getInstance(VersionedEntityManager.class);
        VersionedEntity entity1 = versionedEntities.newVersionedEntity(UUID.randomUUID()).name("test1").create();
        Assertions.assertThat(entity1.version).isEqualTo(1);
        entity1.name = "test_1";
        entity1 = versionedEntities.update(entity1);
        Assertions.assertThat(entity1.version).isEqualTo(2);
        assertThat(versionedEntities.set().name("test1").where().uuid().is(entity1.uuid).patch()).isEqualTo(1L);
        try {
            // trying to save version 2 again
            versionedEntities.update(entity1);
            Assert.fail("was expecting an OptimisticLockException");
        } catch (OptimisticLockException e) {
            // expected
        }

        entity1 = versionedEntities.where().uuid().is(entity1.uuid).first().get();
        Assertions.assertThat(entity1.version).isEqualTo(3);

        VersionedEntity entity2 = versionedEntities.newVersionedEntity(UUID.randomUUID()).name("test2").create();
        entity1.value = 2;
        entity2.value = 2;
        versionedEntities.update(entity1, entity2).forEach(entity -> assertThat(entity.value).isEqualTo(2));
    }
}
