package io.semla.util.concurrent;

import ch.qos.logback.classic.Level;
import io.semla.logging.Logging;
import io.semla.util.Lists;
import io.semla.util.Pair;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static io.semla.util.Unchecked.uncheckedRunnable;
import static io.semla.util.Unchecked.uncheckedSupplier;
import static org.assertj.core.api.Assertions.assertThat;

public class AsyncTest {

    @Test
    public void testBlocking() {
        Logging.setTo(Level.WARN);
        List<Boolean> results = IntStream.range(0, 1000)
            .parallel()
            .mapToObj(i -> Async.blocking(uncheckedSupplier(() -> {
                TimeUnit.MILLISECONDS.sleep(1);
                return true;
            })))
            .toList();
        assertThat(results).hasSize(1000);
    }

    @Test
    public void processAsyncFunctions() {
        List<Integer> lengths = Async.process(Lists.of("a", "ab", "abc"), String::length)
            .stream()
            .map(Pair::left)
            .toList();
        assertThat(lengths).isEqualTo(Lists.of(1, 2, 3));
    }

    @Test
    public void testProcessAsyncConsumers() {
        List<Throwable> errors = Async.process(Lists.of("a", "ab", "abc"), value -> {
                if (value.length() == 2) {
                    throw new RuntimeException();
                }
            })
            .stream()
            .toList();
        assertThat(errors.get(0)).isNull();
        assertThat(errors.get(1)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testSupplyBlockingAsync() {
        List<CompletionStage<Boolean>> stages = IntStream.range(0, 1000)
            .mapToObj(__ -> Async.supplyBlocking(uncheckedSupplier(() -> {
                TimeUnit.MILLISECONDS.sleep(1);
                return true;
            })))
            .toList();
        assertThat(Async.joinAll(stages)).hasSize(1000).allMatch(value -> value);
    }

    @Test
    public void testRunBlockingAsync() {
        List<CompletionStage<Void>> stages = IntStream.range(0, 1000)
            .mapToObj(i -> Async.runBlocking(uncheckedRunnable(() -> TimeUnit.MILLISECONDS.sleep(1))))
            .toList();
        assertThat(Async.joinAll(stages)).hasSize(1000);
    }

    @Test
    public void testRunBlockingAsyncOnAnotherExecutorService() {
        ExecutorService executorService = Executors.newWorkStealingPool();
        List<CompletionStage<Void>> stages = IntStream.range(0, 1000)
            .mapToObj(i -> Async.runBlocking(uncheckedRunnable(() -> TimeUnit.MILLISECONDS.sleep(1)), executorService))
            .toList();
        assertThat(Async.joinAll(stages)).hasSize(1000);
        assertThat(((ForkJoinPool) executorService).getPoolSize()).isGreaterThan(0);
    }

    @Test
    public void replaceDefaultExecutorService() {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        Async.setDefaultExecutorService(executorService);
        try {
            List<CompletionStage<Void>> stages = IntStream.range(0, 10)
                .mapToObj(i -> Async.runBlocking(uncheckedRunnable(() -> TimeUnit.MILLISECONDS.sleep(1))))
                .toList();
            assertThat(Async.joinAll(stages)).hasSize(10);
        } finally {
            Async.setDefaultExecutorService(ForkJoinPool.commonPool());
        }
    }
}