package io.semla.datasource;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.persistence.CacheEntry;
import io.semla.util.Lists;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class EphemeralDatasourceTest {

    EphemeralKeyValueDatasource<CacheEntry> cacheEntries = EntitySteps.datasourceOf(CacheEntry.class);

    @Test
    public void setOne() {
        cacheEntries.set(CacheEntry.of("key", "value"), Duration.ofMinutes(2));
        assertThat(cacheEntries.get("key")).isPresent();
    }


    @Test
    public void setMany() {
        cacheEntries.set(Lists.of(CacheEntry.of("key1", "value1"), CacheEntry.of("key2", "value2")), Duration.ofMinutes(2));
        Map<String, CacheEntry> entries = cacheEntries.get(Lists.of("key1", "key2"));
        assertThat(entries).isNotEmpty();
        assertThat(entries.get("key1")).isNotNull().extracting(entry -> entry.value).isEqualTo("value1");
        assertThat(entries.get("key2")).isNotNull().extracting(entry -> entry.value).isEqualTo("value2");
    }

}