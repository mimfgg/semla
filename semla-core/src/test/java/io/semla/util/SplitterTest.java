package io.semla.util;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class SplitterTest {

    @Test
    public void testSplit() {
        assertThat(Splitter.on(':').split("a:b:c:d:::").toList())
            .isEqualTo(Lists.of("a", "b", "c", "d", "", "", ""));
        assertThat(Splitter.on(':').split("a:b:c:d:::").<List<String>>toList(LinkedList::new))
            .isEqualTo(Lists.of("a", "b", "c", "d", "", "", ""));
    }

    @Test
    public void testSplitWithTrim() {
        assertThat(Splitter.on(':').trim().split("a : b").toList()).isEqualTo(Lists.of("a", "b"));
    }

    @Test
    public void testSplitAndOmitEmptyStrings() {
        assertThat(Splitter.on(':').omitEmptyStrings().split("a:b:c:d:: :").toArray()).isEqualTo(Arrays.of("a", "b", "c", "d", " "));
    }

    @Test
    public void testSplitWithTrimAndOmitEmptyStrings() {
        assertThat(Splitter.on(':').trim().omitEmptyStrings().split("a:b:c:d:: :").toList()).isEqualTo(Lists.of("a", "b", "c", "d"));
    }

    @Test
    public void testAndMap() {
        assertThat(Splitter.on(':').trim().split("a : b").<Integer>map(List::size)).isEqualTo(2);
    }

    @Test
    public void testAndStream() {
        assertThat(Splitter.on(':').trim().split("a : b").stream().count()).isEqualTo(2);
    }

    @Test
    public void testAndForeach() {
        Splitter.on(':').trim().split("a : b").forEach(s -> assertThat(s).isNotBlank());
    }
}
