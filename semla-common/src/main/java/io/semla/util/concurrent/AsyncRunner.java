package io.semla.util.concurrent;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface AsyncRunner<SelfType> {

    @SuppressWarnings("unchecked")
    default CompletionStage<Void> async(Consumer<SelfType> asyncHandler) {
        return Async.runBlocking(() -> asyncHandler.accept((SelfType) this));
    }
}
