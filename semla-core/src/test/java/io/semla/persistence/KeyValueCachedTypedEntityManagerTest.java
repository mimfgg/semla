package io.semla.persistence;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.datasource.SoftKeyValueDatasource;
import io.semla.model.EntityModel;
import io.semla.model.IndexedUserManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyValueCachedTypedEntityManagerTest {

    List<UUID> uuids = new ArrayList<>();
    IndexedUserManager indexedUsers;

    @Before
    public void before() {
        EntitySteps.registerDatasource(SoftKeyValueDatasource.configure().create(EntityModel.of(CacheEntry.class)));
        indexedUsers = EntitySteps.getInstance(IndexedUserManager.class);
        UUID uuid = UUID.randomUUID();
        indexedUsers.newIndexedUser(uuid).age(23).name("bob").create();
        uuids.add(uuid);
        uuid = UUID.randomUUID();
        indexedUsers.newIndexedUser(uuid).age(22).name("tom").create();
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
