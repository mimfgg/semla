package io.semla.datasource;

import io.semla.model.Player;
import io.semla.util.Lists;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ShardedKeyValueDatasourceTest extends CompositeKeyValueDatasourceTest<ShardedDatasource<Player>> {

    public ShardedKeyValueDatasourceTest() {
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
        assertThat(datasource1.get(Lists.of(1, 3)).values().stream().map(player -> player.id).collect(Collectors.toList()), is(Lists.of(1, 3)));
        assertThat(datasource2.get(Lists.of(2, 4)).values().stream().map(player -> player.id).collect(Collectors.toList()), is(Lists.of(2, 4)));
    }

    @Test
    public void getByKeyResharded() {
        Datasource<Player> datasource3 = addDatasource3();
        assertThat(players.get(3).get().name, is("lea"));
        assertThat(datasource3.get(3).isPresent(), is(true));
    }

    @Test
    public void getByKeysResharded() {
        Datasource<Player> datasource3 = addDatasource3();
        assertThat(players.get(Lists.of(3)).values().iterator().next().name, is("lea"));
        assertThat(datasource3.get(3).isPresent(), is(true));
    }

    private Datasource<Player> addDatasource3() {
        //simulating adding a shard
        Datasource<Player> datasource3 = defaultDatasource.create(players.model());
        List<Datasource<Player>> datasources = ((ShardedDatasource<Player>) players).raw();
        datasources.add(datasource3);
        return datasource3;
    }
}
