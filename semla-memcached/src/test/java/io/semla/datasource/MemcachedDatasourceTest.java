package io.semla.datasource;

import io.semla.MemcachedTest;
import org.junit.BeforeClass;

public class MemcachedDatasourceTest extends EphemeralDatasourceTest {

    @BeforeClass
    public static void before() {
        MemcachedTest.container.start();
        MemcachedTest.init();
    }

}