package io.semla.util.concurrent;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface AsyncSupplier<SelfType> {

    @SuppressWarnings("unchecked")
    default <R> CompletionStage<R> async(Function<SelfType, R> asyncHandler) {
        return Async.supplyBlocking(() -> asyncHandler.apply((SelfType) this));
    }
}
