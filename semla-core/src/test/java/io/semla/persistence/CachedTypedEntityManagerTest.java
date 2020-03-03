package io.semla.persistence;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedTypedEntityManagerTest extends KeyValueCachedTypedEntityManagerTest {

    @Test
    public void first() {
        indexedUsers.where().name().is("bob").cachedFor(Duration.ofMinutes(1)).first().get();
        indexedUsers.delete(uuids.get(0));
        indexedUsers.where().name().is("bob").cachedFor(Duration.ofMinutes(3)).first().get(); // this should not throw
        assertThat(indexedUsers.where().name().is("bob").invalidateCache().cachedFor(Duration.ofMinutes(3)).first()).isEmpty();
    }

    @Test
    public void evictFirst() {
        indexedUsers.where().name().is("bob").cachedFor(Duration.ofMinutes(1)).first().get();
        indexedUsers.delete(uuids.get(0));
        indexedUsers.where().name().is("bob").cachedFor(Duration.ofMinutes(3)).first().get(); // this should not throw
        indexedUsers.where().name().is("bob").evictCache().first();
        assertThat(indexedUsers.where().name().is("bob").cached().first()).isEmpty();
    }

    @Test
    public void list() {
        indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).list();
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).list().size()).isEqualTo(2);
        assertThat(indexedUsers.where().name().contains("o").invalidateCache().cachedFor(Duration.ofMinutes(3)).list().size()).isEqualTo(1);
    }

    @Test
    public void evictList() {
        indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).list();
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).list().size()).isEqualTo(2);
        indexedUsers.where().name().contains("o").evictCache().list();
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).list().size()).isEqualTo(1);
    }

    @Test
    public void count() {
        indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).count();
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).count()).isEqualTo(2);
        assertThat(indexedUsers.where().name().contains("o").invalidateCache().cachedFor(Duration.ofMinutes(1)).count()).isEqualTo(1);
    }

    @Test
    public void evictCount() {
        indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).count();
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(3)).count()).isEqualTo(2);
        indexedUsers.where().name().contains("o").evictCache().count();
        assertThat(indexedUsers.where().name().contains("o").cachedFor(Duration.ofMinutes(1)).count()).isEqualTo(1);
    }
}
