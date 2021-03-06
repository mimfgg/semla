package io.semla.relation;

import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import static javax.persistence.CascadeType.*;
import static org.assertj.core.api.Assertions.assertThat;


public class IncludeTypeTest {

    @Test
    public void persistMerge() {
        IncludeTypes includeType = new IncludeTypes(FetchType.LAZY, new CascadeType[]{PERSIST, MERGE}, false);
        assertThat(includeType.toString()).isEqualTo("[CREATE, UPDATE]");
        assertThat(includeType.isEager()).isFalse();
        assertThat(includeType.should(PERSIST)).isTrue();
        assertThat(includeType.should(MERGE)).isTrue();
        assertThat(includeType.should(REMOVE)).isFalse();
    }

    @Test
    public void none() {
        IncludeTypes includeType = new IncludeTypes(FetchType.LAZY, new CascadeType[0], false);
        assertThat(includeType.toString()).isEqualTo("[]");
        assertThat(includeType.should(PERSIST)).isFalse();
        assertThat(includeType.should(MERGE)).isFalse();
        assertThat(includeType.should(REMOVE)).isFalse();
    }

    @Test
    public void all() {
        IncludeTypes includeType = new IncludeTypes(FetchType.EAGER, new CascadeType[]{ALL}, true);
        assertThat(includeType.toString()).isEqualTo("[FETCH, ALL, DELETE_ORPHANS]");
        assertThat(includeType.isEager()).isTrue();
        assertThat(includeType.should(PERSIST)).isTrue();
        assertThat(includeType.should(MERGE)).isTrue();
        assertThat(includeType.should(REMOVE)).isTrue();
        assertThat(includeType.should(IncludeType.DELETE_ORPHANS)).isTrue();
    }

    @Test
    public void unsupported() {
        assertThat(IncludeType.fromCascadeType(REFRESH)).isEqualTo(IncludeType.NONE);
        assertThat(IncludeType.fromCascadeType(DETACH)).isEqualTo(IncludeType.NONE);
    }
}
