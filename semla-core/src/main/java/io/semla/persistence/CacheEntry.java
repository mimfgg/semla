package io.semla.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.validation.constraints.Max;
import java.time.Duration;
import java.time.Instant;

@Entity
public class CacheEntry {

    @Id
    public String key;

    public Instant expires;

    @Lob
    public String value;

    public boolean isExpired() {
        return expires != null && expires.isBefore(Instant.now());
    }

    public static CacheEntry of(String key, String value) {
        return of(key, value, null);
    }

    public static CacheEntry of(String key, String value, Duration ttl) {
        CacheEntry cacheEntry = new CacheEntry();
        cacheEntry.key = key;
        if (ttl != null) {
            cacheEntry.expires = Instant.now().plusMillis(ttl.toMillis());
        }
        cacheEntry.value = value;
        return cacheEntry;
    }


}
