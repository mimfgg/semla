package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.util.concurrent.Async;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultEagersOf;
import static io.semla.query.Includes.of;

public class Get<K, T> extends ContextualQuery<T> {

    public Get(PersistenceContext context, EntityModel<T> model) {
        super(context, model);
    }

    public Optional<T> get(K key) {
        return get(key, defaultEagersOf(model())::addTo);
    }

    public Optional<T> get(K key, UnaryOperator<Includes<T>> include) {
        return context.get(key, include.apply(of(model)));
    }

    public Map<K, T> get(Collection<K> keys) {
        return get(keys, defaultEagersOf(model())::addTo);
    }

    public Map<K, T> get(Collection<K> keys, UnaryOperator<Includes<T>> include) {
        return context.get(keys, include.apply(of(model)));
    }

    public Get<K, T> cached() {
        context.cachingStrategy().withCache(true);
        return this;
    }

    public Get<K, T> cachedFor(Duration ttl) {
        context.cachingStrategy().withCache(true).withTtl(ttl);
        return this;
    }

    public Get<K, T> invalidateCache() {
        context.cachingStrategy().invalidateCache(true);
        return this;
    }

    public Get<K, T>.Evict evictCache() {
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

        public void get(Collection<K> keys) {
            get(keys, defaultEagersOf(model())::addTo);
        }

        public void get(Collection<K> keys, UnaryOperator<Includes<T>> include) {
            context.get(keys, include.apply(of(model)));
        }

        @SuppressWarnings("unchecked")
        public AsyncHandler<K, T> async() {
            return Async.asyncHandler(AsyncHandler.class, this);
        }

        public interface AsyncHandler<K, T> {

            CompletionStage<Void> get(Object key);

            CompletionStage<Void> get(Object key, UnaryOperator<Includes<T>> include);

            CompletionStage<Void> get(Collection<K> keys);

            CompletionStage<Void> get(Collection<K> keys, UnaryOperator<Includes<T>> include);
        }
    }

    @SuppressWarnings("unchecked")
    public AsyncHandler<K, T> async() {
        return Async.asyncHandler(AsyncHandler.class, this);
    }

    public interface AsyncHandler<K, T> {

        CompletionStage<Optional<T>> get(K key);

        CompletionStage<Optional<T>> get(K key, UnaryOperator<Includes<T>> include);

        CompletionStage<Map<K, T>> get(Collection<K> keys);

        CompletionStage<Map<K, T>> get(Collection<K> keys, UnaryOperator<Includes<T>> include);

    }
}
