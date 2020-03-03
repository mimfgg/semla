package io.semla.datasource;

import io.semla.model.Player;
import io.semla.util.Lists;
import org.junit.Test;

import static io.semla.datasource.CachedDatasourceTest.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CachedKeyValueDatasourceTest extends CompositeKeyValueDatasourceTest<CachedDatasource<Player>> {

    public CachedKeyValueDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }

    @Test
    public void cachedKeyValueDatasources() {
        Player player = Player.with(4, "tim", 12);
        players.create(player);

        assertThat(datasource1.get(player.id).get().name).isEqualTo("tim");
        assertThat(datasource2.get(player.id).get().name).isEqualTo("tim");

        player.name = "mat";
        players.update(player);

        assertThat(datasource1.get(player.id).get().name).isEqualTo("mat");
        assertThat(datasource2.get(player.id).get().name).isEqualTo("mat");

        datasource1.delete(player.id);
        players.get(player.id);
        assertThat(datasource1.get(player.id).get().name).isEqualTo("mat");

        datasource1.delete(player.id);
        players.get(Lists.of(player.id));
        assertThat(datasource1.get(player.id).get().name).isEqualTo("mat");

        players.delete(player.id);

        assertThat(datasource1.get(player.id).isPresent()).isFalse();
        assertThat(datasource2.get(player.id).isPresent()).isFalse();
    }
}
