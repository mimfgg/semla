package io.semla.datasource;

import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.model.Player;
import io.semla.util.Lists;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KeyValueDatasourceTest {

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
    public void rawAccess() {
        assertThat(players.raw()).isNotNull();
    }

    @Test
    public void getByKey() {
        assertThat(players.get(1).get().name).isEqualTo("bob");
    }

    @Test
    public void getByKeys() {
        assertThat(
            players.get(Lists.of(1, 3)).values().stream().map(player -> player.name).collect(Collectors.joining(","))
        ).isEqualTo("bob,lea");

        assertThat(players.get(Lists.empty())).isEmpty();
    }

    @Test
    public void create() {
        players.create(Player.with(4, "rak", 1000));
        assertThat(players.get(4).get().name).isEqualTo("rak");
    }

    @Test
    public void persistMany() {
        List<Player> entities = IntStream.range(0, 10).mapToObj(i -> Player.with(4 + i, "player_" + i, 0)).collect(Collectors.toList());
        players.create(entities);
        entities.forEach(entity -> assertThat(players.get(entity.id).get().name).isEqualTo(entity.name));
    }

    @Test
    public void update() {
        Player lea = players.get(3).get();
        lea.score = 200;
        players.update(lea);
        Assertions.assertThat(lea.score).isEqualTo(200);
        assertThat(players.get(3).get().score).isEqualTo(200);
    }

    @Test
    public void mergeMany() {
        players.update(players.get(Lists.of(1, 2, 3)).values().stream().peek(player -> player.score = 300).collect(Collectors.toList()));
        players.get(Lists.of(1, 2, 3)).values().forEach(player -> assertThat(player.score).isEqualTo(300));
    }

    @Test
    public void deleteByKey() {
        players.delete(1);
        assertThat(players.get(1).isPresent()).isFalse();
        players.delete(Lists.empty());
    }

    @Test
    public void deleteMany() {
        players.delete(Lists.of(1, 3));
        Map<Integer, Player> playerById = players.get(Lists.of(1, 3));
        assertThat(playerById.size()).isEqualTo(2);
        Assertions.assertThat(playerById.get(1)).isNull();
        Assertions.assertThat(playerById.get(3)).isNull();
    }

    @Test
    public void unsupportedMethods() {
        if (players instanceof KeyValueDatasource) {
            assertThatThrownBy(() -> players.first()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> players.list()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> players.patch(null, null)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> players.delete(null, null)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> players.count(null)).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
