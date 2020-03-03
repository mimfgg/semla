package io.semla.query;

import io.semla.model.Player;
import io.semla.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PredicatesTest {

    @Test
    public void predicate() {
        assertThat(Predicate.is.test(1, 1)).isTrue();
        assertThat(Predicate.not.test(1, 2)).isTrue();
        assertThat(Predicate.in.test(1, Lists.of(1, 2))).isTrue();
        assertThat(Predicate.notIn.test(1, Lists.of(2, 3))).isTrue();
        assertThat(Predicate.greaterOrEquals.test(1, 1)).isTrue();
        assertThat(Predicate.greaterThan.test(1, 0)).isTrue();
        assertThat(Predicate.lessOrEquals.test(1, 1)).isTrue();
        assertThat(Predicate.lessThan.test(1, 2)).isTrue();
        assertThat(Predicate.like.test("test", "%est")).isTrue();
        assertThat(Predicate.notLike.test("test", "%ob%")).isTrue();
        assertThat(Predicate.contains.test("test", "es")).isTrue();
        assertThat(Predicate.doesNotContain.test("test", "bob")).isTrue();
        assertThat(Predicate.containedIn.test("es", "test")).isTrue();
        assertThat(Predicate.notContainedIn.test("bob", "test")).isTrue();
    }

    @Test
    public void predicates() {
        assertEquals(Predicates.of(Player.class).where("score").is(9000),
            "score is 9000");
        assertEquals(Predicates.of(Player.class).where("score").is(9000).and("name").is("test"),
            "score is 9000 and name is \"test\"");
        assertEquals(Predicates.of(Player.class).where("id").in(1, 2, 3),
            "id in [1, 2, 3]");
        assertEquals(Predicates.of(Player.class).where("id").in(Lists.of(1, 2, 3)),
            "id in [1, 2, 3]");
        assertEquals(Predicates.of(Player.class).where("id").in((Object) Lists.of(1, 2, 3)),
            "id in [1, 2, 3]");
        assertEquals(Predicates.of(Player.class).where("id").notIn(1, 2, 3),
            "id notIn [1, 2, 3]");
        assertEquals(Predicates.of(Player.class).where("id").notIn(Lists.of(1, 2, 3)),
            "id notIn [1, 2, 3]");
        assertEquals(Predicates.of(Player.class).where("id").notIn((Object) Lists.of(1, 2, 3)),
            "id notIn [1, 2, 3]");
    }

    private void assertEquals(Predicates<Player> predicates, String query) {
        assertThat(predicates.toString()).isEqualTo(query);
        assertThat(Predicates.of(Player.class).parse(query).toString()).isEqualTo(query);
    }
}
