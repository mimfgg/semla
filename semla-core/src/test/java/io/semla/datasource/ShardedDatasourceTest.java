package io.semla.datasource;

import io.semla.model.Player;
import io.semla.query.Predicates;
import io.semla.util.Lists;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ShardedDatasourceTest extends CompositeDatasourceTest<ShardedDatasource<Player>> {

    public ShardedDatasourceTest() {
        super(
            defaultDatasource -> ShardedDatasource.configure()
                .withRebalancing(true)
                .withDatasources(defaultDatasource, defaultDatasource),
            datasource -> datasource.forKey(1),
            datasource -> datasource.forKey(2)
        );
    }

    @Test
    public void shardedDatasources() {
        players.create(Player.with(4, "tim", 12));
        assertThat(datasource1.list().stream().map(player -> player.id).collect(Collectors.toList()), is(Lists.of(1, 3)));
        assertThat(datasource2.list().stream().map(player -> player.id).collect(Collectors.toList()), is(Lists.of(2, 4)));
    }

    @Test
    public void firstResharded() {
        Datasource<Player> datasource3 = addDatasource3();
        assertThat(players.first(Predicates.of(Player.class).where("name").is("lea")).get().id, is(3));
        assertThat(datasource3.first().get().id, is(3));
    }

    @Test
    public void listResharded() {
        Datasource<Player> datasource3 = addDatasource3();
        assertThat(players.list(Predicates.of(Player.class).where("name").is("lea")).size(), is(1));
        assertThat(datasource3.first().get().id, is(3));
    }

    private Datasource<Player> addDatasource3() {
        //simulating adding a shard
        Datasource<Player> datasource3 = defaultDatasource.create(players.model());
        List<Datasource<Player>> datasources = ((ShardedDatasource<Player>) players).raw();
        datasources.add(datasource3);
        return datasource3;
    }
}
