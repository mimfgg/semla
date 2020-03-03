package io.semla.datasource;

import io.semla.config.DatasourceConfiguration;
import io.semla.model.Player;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public abstract class ReplicatedKeyValueDatasourceTest<DatasourceType extends Datasource<Player>> extends CompositeKeyValueDatasourceTest<DatasourceType> {

    protected ReplicatedKeyValueDatasourceTest(UnaryOperator<DatasourceConfiguration> wrapper,
                                               Function<DatasourceType, Datasource<Player>> firstDatasource,
                                               Function<DatasourceType, Datasource<Player>> secondDatasource) {
        super(wrapper, firstDatasource, secondDatasource);
    }

    @Test
    public void replicatedKeyValueDatasources() {
        Player player = Player.with(4, "tim", 12);
        players.create(player);

        Assertions.assertThat(datasource1.get(player.id).get().name).isEqualTo("tim");
        Assertions.assertThat(datasource2.get(player.id).get().name).isEqualTo("tim");

        player.name = "mat";
        players.update(player);

        Assertions.assertThat(datasource1.get(player.id).get().name).isEqualTo("mat");
        Assertions.assertThat(datasource2.get(player.id).get().name).isEqualTo("mat");

        players.delete(player.id);

        Assertions.assertThat(datasource1.get(player.id).isPresent()).isFalse();
        Assertions.assertThat(datasource2.get(player.id).isPresent()).isFalse();
    }
}
