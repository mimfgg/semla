package io.semla.datasource;

import io.semla.model.Player;
import io.semla.query.Predicates;
import io.semla.query.Values;
import org.junit.Test;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachedDatasourceTest extends CompositeDatasourceTest<CachedDatasource<Player>> {

    public static final UnaryOperator<Datasource.Configuration> WRAPPER = defaultFactory ->
        CachedDatasource.configure().withCache(SoftKeyValueDatasource.configure()).withDatasource(defaultFactory);
    public static final Function<CachedDatasource<Player>, Datasource<Player>> FIRST_DATASOURCE = datasource -> datasource.raw().first();
    public static final Function<CachedDatasource<Player>, Datasource<Player>> SECOND_DATASOURCE = datasource -> datasource.raw().second();

    public CachedDatasourceTest() {
        super(WRAPPER, FIRST_DATASOURCE, SECOND_DATASOURCE);
    }

    @Test
    public void cachedDatasources() {
        Player player = Player.with(4, "tim", 12);
        players.create(player);

        assertThat(datasource1.get(player.id).get().name, is("tim"));
        assertThat(datasource2.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("tim"));

        player.name = "mat";
        players.patch(Values.of(Player.class).with("name", "mat"), Predicates.of(Player.class).where("id").is(player.id));

        assertThat(datasource1.get(player.id).get().name, is("mat"));
        assertThat(datasource2.first(Predicates.of(Player.class).where("id").is(player.id)).get().name, is("mat"));

        datasource1.delete(player.id);
        players.first(Predicates.of(Player.class).where("id").is(player.id));
        assertThat(datasource1.get(player.id).get().name, is("mat"));

        datasource1.delete(player.id);
        players.list(Predicates.of(Player.class).where("id").is(player.id));
        assertThat(datasource1.get(player.id).get().name, is("mat"));

        players.delete(Predicates.of(Player.class).where("name").is("mat"));

        assertThat(datasource1.get(player.id).isPresent(), is(false));
        assertThat(datasource2.count(Predicates.of(Player.class).where("id").is(player.id)), is(0L));
    }
}
