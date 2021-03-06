package io.semla;

import io.semla.cache.CacheTest;
import io.semla.datasource.*;
import io.semla.persistence.*;
import io.semla.relation.KeyValueRelationsTest;
import io.semla.relation.RelationsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    CacheTest.class,
    CoreTest.class,
    CachedDatasourceTest.class,
    CachedKeyValueDatasourceTest.class,
    DatasourceTest.class,
    KeyValueDatasourceTest.class,
    MasterSlaveDatasourceTest.class,
    MasterSlaveKeyValueDatasourceTest.class,
    ReadOneWriteAllDatasourceTest.class,
    ReadOneWriteAllKeyValueDatasourceTest.class,
    ShardedDatasourceTest.class,
    ShardedKeyValueDatasourceTest.class,
    CachedEntityManagerTest.class,
    CachedTypedEntityManagerTest.class,
    EntityManagerTest.class,
    KeyValueCachedEntityManagerTest.class,
    KeyValueCachedTypedEntityManagerTest.class,
    TypedEntityManagerTest.class,
    KeyValueRelationsTest.class,
    RelationsTest.class
})
public abstract class DatasourceSuite extends BaseSuite {}
