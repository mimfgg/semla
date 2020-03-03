package io.semla.query;

import io.semla.model.Fruit;
import io.semla.model.Genus;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class IncludesTest {

    @Test
    public void includes() {
        assertEquals(Includes.of(Fruit.class), "");
        assertEquals(Includes.of(Fruit.class).include("genus"), "genus");
        assertEquals(Includes.of(Fruit.class).include("genus", genus -> genus.include("family")), "genus{family}");
        assertEquals(
            Includes.of(Fruit.class).include("genus", genus -> genus.include("family", family -> family.include("genuses"))),
            "genus{family{genuses}}"
        );
        assertEquals(
            Includes.of(Fruit.class).include("genus",
                genus -> genus.include("family",
                    family -> family.include("genuses",
                        genuses -> genuses.include("fruits")))),
            "genus{family{genuses{fruits}}}"
        );
        assertEquals(
            Includes.of(Fruit.class).include("genus",
                genus -> genus.include("family",
                    family -> family.include("genuses",
                        genuses -> genuses.include("fruits")))),
            "genus{family{genuses{fruits}}}"
        );

        assertFail(() -> Includes.of(Fruit.class).include("genus", genus -> genus.include("location")),
            "location doesn't exist on Genus",
            "couldn't find relation location on class io.semla.model.Genus");
        assertFail(() -> Includes.of(Fruit.class).include("genus{location}"),
            "location doesn't exist on Genus",
            "couldn't find relation location on class io.semla.model.Genus");
    }

    @Test
    public void parsed() {
        assertThat(Includes.of(Genus.class).include("family,fruits").relations().size()).isEqualTo(2);
        assertFail(() -> Includes.of(Fruit.class).include(",fruits"),
            "should have failed",
            "unexpected ',' encountered in \",fruits\"");
        assertFail(() -> Includes.of(Fruit.class).include("-fruits"),
            "should have failed",
            "unexpected '-' encountered in \"-fruits\"");
        assertFail(() -> Includes.of(Fruit.class).include("family{genuses"),
            "should have failed",
            "no } is closing { in \"family{genuses\"");
        assertThat(Includes.of(Genus.class).include("fruits[ALL, DELETE_ORPHANS]").toString())
            .isEqualTo("fruits[ALL, DELETE_ORPHANS]");
        assertFail(() -> Includes.of(Fruit.class).include("fruits[ALL, DELETE_ORPHANS"),
            "should have failed",
            "no ] is closing [ in \"fruits[ALL, DELETE_ORPHANS\"");

    }

    private void assertEquals(Includes<Fruit> includes, String query) {
        assertThat(includes.toString()).isEqualTo(query);
        assertThat(Includes.of(Fruit.class).include(query).toString()).isEqualTo(query);
    }

    private void assertFail(Supplier<Includes<Fruit>> supplier, String failWith, String compareTo) {
        try {
            supplier.get();
            Assert.fail(failWith);
        } catch (IllegalArgumentException | IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo(compareTo);
        }
    }
}
