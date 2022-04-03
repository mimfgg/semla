package io.semla.util.concurrent;

import io.semla.reflect.Methods;
import io.semla.reflect.Proxy;
import io.semla.util.Pair;
import io.semla.util.Unchecked;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Slf4j
public class Async {

    public static ExecutorService defaultExecutorService = ForkJoinPool.commonPool();

    public static void setDefaultExecutorService(ExecutorService executorService) {
        Async.defaultExecutorService = executorService;
    }

    static <T> T blocking(Supplier<T> supplier) {
        return blocking(supplier, defaultExecutorService);
    }

    static <T> T blocking(Supplier<T> supplier, ExecutorService executorService) {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                @Override
                public boolean block() {
                    result.set(supplier.get());
                    return true;
                }

                @Override
                public boolean isReleasable() {
                    return result.get() != null;
                }
            });
        } catch (RejectedExecutionException e) {
            // we reached ForkJoinPool.DEFAULT_COMMON_MAX_SPARES,
            // our blocking supplier was rejected, lets re-enqueing it ...
            log.warn(e.getMessage() + ", running ...");
            return supplier.get();
        } catch (InterruptedException e) {
            Unchecked.rethrow(e);
        }
        return result.get();
    }

    public static <E> List<Throwable> process(List<E> elements, Consumer<E> function) {
        return process(elements, function, defaultExecutorService);
    }

    public static <E> List<Throwable> process(List<E> elements, Consumer<E> function, ExecutorService executorService) {
        return process(elements, element -> {
            function.accept(element);
            return null;
        }, executorService)
            .stream()
            .map(Pair::second)
            .toList();
    }

    public static <E, R> List<Pair<R, Throwable>> process(List<E> elements, Function<E, R> function) {
        return process(elements, function, defaultExecutorService);
    }

    public static <E, R> List<Pair<R, Throwable>> process(List<E> elements, Function<E, R> function, ExecutorService executorService) {
        return completedFuture(null)
            .thenApply(__ -> elements.stream().map(element ->
                supplyBlocking(() -> function.apply(element), executorService).handle(Pair::of)).toList())
            .thenApply(Async::joinAll)
            .join();
    }

    @SafeVarargs
    public static <T> List<T> joinAll(CompletionStage<T>... stages) {
        return joinAll(List.of(stages));
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public static <T> List<T> joinAll(List<CompletionStage<T>> stages) {
        CompletableFuture<T>[] cfs = stages.stream()
            .map(CompletionStage::toCompletableFuture)
            .<CompletableFuture<T>>toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(cfs).join();
        return Stream.of(cfs).map(CompletableFuture::join).toList();
    }

    public static <T> CompletionStage<T> supplyBlocking(Supplier<T> supplier) {
        return supplyBlocking(supplier, defaultExecutorService);
    }

    public static <T> CompletionStage<T> supplyBlocking(Supplier<T> supplier, ExecutorService executorService) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() {
                        try {
                            completableFuture.complete(supplier.get());
                        } catch (Throwable t) {
                            completableFuture.completeExceptionally(t);
                        }
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        return completableFuture.isDone();
                    }
                });
            } catch (RejectedExecutionException e) {
                // we reached ForkJoinPool.DEFAULT_COMMON_MAX_SPARES, our blocking supplier was rejected
                // lets re-enqueing it ...
                log.warn(e.getMessage() + ", re-enqueing ...");
                CompletableFuture.supplyAsync(supplier, executorService)
                    .whenComplete((result, t) -> {
                        if (t != null) {
                            completableFuture.completeExceptionally(t);
                        } else {
                            completableFuture.complete(result);
                        }
                    });
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    public static CompletionStage<Void> runBlocking(Runnable runnable) {
        return runBlocking(runnable, defaultExecutorService);
    }

    public static CompletionStage<Void> runBlocking(Runnable runnable, ExecutorService executorService) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() {
                        try {
                            runnable.run();
                            completableFuture.complete(null);
                        } catch (Throwable t) {
                            completableFuture.completeExceptionally(t);
                        }
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        return completableFuture.isDone();
                    }
                });
            } catch (RejectedExecutionException e) {
                // we reached ForkJoinPool.DEFAULT_COMMON_MAX_SPARES, our blocking supplier was rejected
                // lets re-enqueing it ...
                log.warn(e.getMessage() + ", re-enqueing ...");
                CompletableFuture.runAsync(runnable, executorService)
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            completableFuture.completeExceptionally(t);
                        } else {
                            completableFuture.complete(null);
                        }
                    });
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    public static <A> A asyncHandler(Class<A> handler, Object target) {
        return asyncHandler(handler, target, defaultExecutorService);
    }

    public static <A> A asyncHandler(Class<A> handler, Object target, ExecutorService executorService) {
        return Proxy.of(handler, (proxy, method, args) -> switch (method.getName()) {
            case "equals", "hashCode", "toString" -> Methods.invoke(target, method.getName(), args);
            default -> Async.supplyBlocking(() -> Methods.invoke(target, method.getName(), args), executorService);
        });
    }
}
