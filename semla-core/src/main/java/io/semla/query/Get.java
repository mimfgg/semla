package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultEagersOf;
import static io.semla.query.Includes.of;

public class Get<T> extends ContextualQuery<T> {

    public Get(PersistenceContext context, EntityModel<T> model) {
        super(context, model);
    }

    public Optional<T> get(Object key) {
        return get(key, defaultEagersOf(model())::addTo);
    }

    public Optional<T> get(Object key, UnaryOperator<Includes<T>> include) {
        return context.get(key, include.apply(of(model)));
    }

    public <K> Map<K, T> get(Collection<K> keys) {
        return get(keys, defaultEagersOf(model())::addTo);
    }

    public <K> Map<K, T> get(Collection<K> keys, UnaryOperator<Includes<T>> include) {
        return context.get(keys, include.apply(of(model)));
    }

    public Get<T> cached() {
        context.cachingStrategy().withCache(true);
        return this;
    }

    public Get<T> cachedFor(Duration ttl) {
        context.cachingStrategy().withCache(true).withTtl(ttl);
        return this;
    }

    public Get<T> invalidateCache() {
        context.cachingStrategy().invalidateCache(true);
        return this;
    }

    public Get<T>.Evict evictCache() {
        context.cachingStrategy().evictCache();
        return new Evict();
    }

    public class Evict {

        public void get(Object key) {
            get(key, defaultEagersOf(model())::addTo);
        }

        public void get(Object key, UnaryOperator<Includes<T>> include) {
            context.get(key, include.apply(of(model)));
        }

        public <K> void get(Collection<K> keys) {
            get(keys, defaultEagersOf(model())::addTo);
        }

        public <K> void get(Collection<K> keys, UnaryOperator<Includes<T>> include) {
            context.get(keys, include.apply(of(model)));
        }

    }
}
