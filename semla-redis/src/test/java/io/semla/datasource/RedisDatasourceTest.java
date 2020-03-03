package io.semla.datasource;

import io.semla.RedisTest;
import org.junit.BeforeClass;

public class RedisDatasourceTest extends EphemeralDatasourceTest {

    @BeforeClass
    public static void before() {
        RedisTest.container.start();
        RedisTest.init();
    }

}