package io.semla;

import io.semla.cache.CacheTest;
import io.semla.datasource.*;
import io.semla.persistence.KeyValueCachedEntityManagerTest;
import io.semla.persistence.KeyValueCachedTypedEntityManagerTest;
import io.semla.relations.KeyValueRelationsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CacheTest.class,
    CachedKeyValueDatasourceTest.class,
    KeyValueDatasourceTest.class,
    MasterSlaveKeyValueDatasourceTest.class,
    ReadOneWriteAllKeyValueDatasourceTest.class,
    ShardedKeyValueDatasourceTest.class,
    KeyValueCachedEntityManagerTest.class,
    KeyValueCachedTypedEntityManagerTest.class,
    KeyValueRelationsTest.class
})
public abstract class KeyValueDatasourceSuite extends BaseSuite {}
