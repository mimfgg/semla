package io.semla.datasource;

import io.semla.config.DatasourceConfiguration;
import io.semla.model.Player;
import io.semla.query.Predicates;
import io.semla.query.Values;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ReplicatedDatasourceTest<DatasourceType extends Datasource<Player>> extends CompositeDatasourceTest<DatasourceType> {

    protected ReplicatedDatasourceTest(UnaryOperator<DatasourceConfiguration> wrapper,
                                       Function<DatasourceType, Datasource<Player>> firstDatasource,
                                       Function<DatasourceType, Datasource<Player>> secondDatasource) {
        super(wrapper, firstDatasource, secondDatasource);
    }

    @Test
    public void replicatedDatasources() {
        Player player = Player.with(4, "tim", 12);
        players.create(player);

        assertThat(datasource1.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("tim"));
        assertThat(datasource2.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("tim"));

        players.patch(Values.of(Player.class).with("name", "mat"), Predicates.of(Player.class).where("id").is(player.id));

        assertThat(datasource1.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("mat"));
        assertThat(datasource2.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("mat"));

        players.delete(Predicates.of(Player.class).where("name").is("mat"));

        assertThat(datasource1.count(Predicates.of(Player.class).where("id").is(player.id)), is(0L));
        assertThat(datasource2.count(Predicates.of(Player.class).where("id").is(player.id)), is(0L));
    }
}
