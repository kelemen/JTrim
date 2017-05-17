package org.jtrim2.testutils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jtrim2.concurrent.Tasks;

public final class FutureUtils {
    public static <V> CompletableFuture<V> toWaitable(CompletionStage<V> future) {
        CompletableFuture<V> waitableFuture = new CompletableFuture<>();
        future.whenComplete(Tasks.completeForwarder(waitableFuture));
        return waitableFuture;
    }

    public static <V> V tryGetResult(CompletionStage<V> future) {
        return toWaitable(future).getNow(null);
    }

    private FutureUtils() {
        throw new AssertionError();
    }
}
