package io.semla.cache;

import com.decathlon.tzatziki.steps.EntitySteps;
import io.semla.util.Lists;
import org.junit.After;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CacheTest {

    private final Cache cache = EntitySteps.getInstance(Cache.class);

    @Test
    public void getEmpty() {
        assertThat(cache.get("test", String.class)).isEmpty();
    }

    @Test
    public void putAndGet() {
        cache.put("test", "value");
        assertThat(cache.get("test", String.class)).get().isEqualTo("value");
    }

    @Test
    public void putWithExpiration() throws InterruptedException {
        cache.put("test", "value", Duration.ofMillis(2));
        TimeUnit.MILLISECONDS.sleep(5);
        getEmpty();
    }

    @Test
    public void getWithTypeAndLoader() {
        assertThat(cache.get("test", String.class, () -> "value")).isEqualTo("value");
    }

    @Test
    public void getWithTypeLoaderAndTtl() {
        assertThat(cache.get("test", String.class, () -> "value", Duration.ofMinutes(1))).isEqualTo("value");
    }

    @Test
    public void getWithReflectiveLoader() {
        assertThat(cache.get("test", () -> Lists.of("value"))).isEqualTo(Lists.of("value"));
    }

    @Test
    public void getWithReflectiveLoaderAndTtl() {
        assertThat(cache.get("test", () -> Lists.of("value"), Duration.ofMinutes(1))).isEqualTo(Lists.of("value"));
    }

    @Test
    public void evict() {
        putAndGet();
        cache.evict("test");
        getEmpty();
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }
}