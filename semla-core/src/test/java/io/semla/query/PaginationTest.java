package io.semla.query;

import io.semla.model.Player;
import io.semla.util.Lists;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaginationTest {

    @Test
    public void pagination() {
        assertEquals(Pagination.of(Player.class).orderedBy("score"), "ordered by score");
        assertEquals(Pagination.of(Player.class).orderedBy("score", Pagination.Sort.DESC), "ordered by score desc");
        assertEquals(Pagination.of(Player.class).orderedBy("score", Pagination.Sort.DESC).startAt(1),
            "ordered by score desc start at 1");
        assertEquals(Pagination.of(Player.class).orderedBy("score", Pagination.Sort.DESC).startAt(1).limitTo(1),
            "ordered by score desc start at 1 limit to 1");
        assertEquals(Pagination.of(Player.class).startAt(1).limitTo(1), "start at 1 limit to 1");
        assertEquals(Pagination.of(Player.class).startAt(1), "start at 1");
        assertEquals(Pagination.of(Player.class).limitTo(1), "limit to 1");
    }

    private void assertEquals(Pagination<Player> pagination, String query) {
        assertThat(pagination.toString()).isEqualTo(query);
        assertThat(Pagination.of(Player.class).parse(query).toString()).isEqualTo(query);
    }

    @Test
    public void errors() {
        assertThatThrownBy(() -> Pagination.of(Player.class).parse("start 0 limit 1"))
            .hasMessage("was expecting 'at' after 'start' in 'start 0 limit 1'");
        assertThatThrownBy(() -> Pagination.of(Player.class).parse("start at 0 limit 1"))
            .hasMessage("was expecting 'to' after 'limit' in 'start at 0 limit 1'");
    }

    @Test
    public void compare() {
        assertThat(Lists.of(
            Player.with(1, null, 0),
            Player.with(2, null, 0)
        )
            .stream()
            .sorted(Pagination.of(Player.class).orderedBy("name")::compare)
            .collect(Collectors.toList()).get(0).id).isEqualTo(1);

        assertThat(Lists.of(
            Player.with(1, null, 0),
            Player.with(2, "test", 0)
        )
            .stream()
            .sorted(Pagination.of(Player.class).orderedBy("name")::compare)
            .collect(Collectors.toList()).get(0).id).isEqualTo(1);

        assertThat(Lists.of(
            Player.with(1, "test", 0),
            Player.with(2, null, 0)
        )
            .stream()
            .sorted(Pagination.of(Player.class).orderedBy("name")::compare)
            .collect(Collectors.toList()).get(0).id).isEqualTo(2);

        assertThat(Lists.of(
            Player.with(1, "test1", 0),
            Player.with(2, "test2", 0)
        )
            .stream()
            .sorted(Pagination.of(Player.class).orderedBy("name")::compare)
            .collect(Collectors.toList()).get(0).id).isEqualTo(1);

        assertThat(Lists.of(
            Player.with(1, "test1", 0),
            Player.with(2, "test2", 0)
        )
            .stream()
            .sorted(Pagination.of(Player.class).orderedBy("name", Pagination.Sort.DESC)::compare)
            .collect(Collectors.toList()).get(0).id).isEqualTo(2);
    }
}
