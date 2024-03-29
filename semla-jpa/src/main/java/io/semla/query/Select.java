package io.semla.query;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.util.concurrent.Async;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import static io.semla.query.Includes.defaultEagersOf;
import static io.semla.query.Includes.of;

public class Select<T> extends PaginatedQuery<T, Select<T>> {

    public Select(PersistenceContext context, EntityModel<T> model) {
        super(context, model);
    }

    public Optional<T> first() {
        return first(defaultEagersOf(model())::addTo);
    }

    public Optional<T> first(UnaryOperator<Includes<T>> include) {
        return context.first(predicates, pagination, include.apply(of(model())));
    }

    public List<T> list() {
        return list(defaultEagersOf(model())::addTo);
    }

    public List<T> list(UnaryOperator<Includes<T>> include) {
        return context.list(predicates, pagination, include.apply(of(model())));
    }

    public long count() {
        return context.count(predicates);
    }

    public Evict evictCache() {
        context.cachingStrategy().evictCache();
        return new Evict();
    }

    public long delete() {
        return delete(Includes.defaultRemovesOrDeleteOf(model)::addTo);
    }

    public long delete(UnaryOperator<Includes<T>> includes) {
        return context.delete(predicates, pagination, includes.apply(of(model)));
    }

    public Patch<T> set(String fieldName, Object value) {
        return new Patch<>(context, predicates, pagination, new Values<>(model)).set(fieldName, value);
    }

    public Patch<T> set(Map<String, Object> values) {
        return new Patch<>(context, predicates, pagination, new Values<>(model)).set(values);
    }

    public class Evict {

        public void first() {
            first(UnaryOperator.identity());
        }

        public void first(UnaryOperator<Includes<T>> include) {
            context.first(predicates, pagination, include.apply(defaultEagersOf(model())));
        }

        public void list() {
            list(UnaryOperator.identity());
        }

        public void list(UnaryOperator<Includes<T>> include) {
            context.list(predicates, pagination, include.apply(defaultEagersOf(model())));
        }

        public void count() {
            context.count(predicates);
        }

        @SuppressWarnings("unchecked")
        public AsyncHandler<T> async() {
            return Async.asyncHandler(AsyncHandler.class, this);
        }

        public interface AsyncHandler<T> {

            CompletionStage<Void> first();

            CompletionStage<Void> first(UnaryOperator<Includes<T>> include);

            CompletionStage<Void> list();

            CompletionStage<Void> list(UnaryOperator<Includes<T>> include);

            CompletionStage<Void> count();
        }
    }

    @SuppressWarnings("unchecked")
    public AsyncHandler<T> async() {
        return Async.asyncHandler(AsyncHandler.class, this);
    }

    public interface AsyncHandler<T> {

        CompletionStage<Optional<T>> first();

        CompletionStage<Optional<T>> first(UnaryOperator<Includes<T>> include);

        CompletionStage<List<T>> list();

        CompletionStage<List<T>> list(UnaryOperator<Includes<T>> include);

        CompletionStage<Long> count();

        CompletionStage<Long> delete();

        CompletionStage<Long> delete(UnaryOperator<Includes<T>> includes);

    }


}
