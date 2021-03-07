package io.semla.persistence;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.IndexedUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyValueCachedEntityManagerTest {

    List<UUID> uuids = new ArrayList<>();
    EntityManager<IndexedUser> indexedUsers;

    @Before
    public void before() {
        indexedUsers = EntitySteps.entityManagerOf(IndexedUser.class);
        UUID uuid = UUID.randomUUID();
        indexedUsers.newInstance().with("uuid", uuid).with("age", 23).with("name", "bob").create();
        uuids.add(uuid);
        uuid = UUID.randomUUID();
        indexedUsers.newInstance().with("uuid", uuid).with("age", 22).with("name", "tom").create();
        uuids.add(uuid);
    }

    @Test
    public void getOne() {
        indexedUsers.cached().get(uuids.get(0));
        indexedUsers.delete(uuids.get(0));
        indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids.get(0)); // this should not throw
        assertThat(indexedUsers.invalidateCache().cachedFor(Duration.ofMinutes(1)).get(uuids.get(0))).isEmpty();
    }

    @Test
    public void evictGetOne() {
        indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids.get(0));
        indexedUsers.delete(uuids.get(0));
        indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids.get(0)); // this should not throw
        indexedUsers.evictCache().get(uuids.get(0));
        assertThat(indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids.get(0))).isEmpty();
    }

    @Test
    public void getMany() {
        indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids);
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids).size()).isEqualTo(2);
        assertThat(indexedUsers.invalidateCache().cachedFor(Duration.ofMinutes(1)).get(uuids).get(uuids.get(0))).isNull();
    }

    @Test
    public void evictGetMany() {
        indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids);
        indexedUsers.delete(uuids.get(0));
        assertThat(indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids).size()).isEqualTo(2);
        indexedUsers.evictCache().get(uuids);
        assertThat(indexedUsers.cachedFor(Duration.ofMinutes(1)).get(uuids).get(uuids.get(0))).isNull();
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }

}
