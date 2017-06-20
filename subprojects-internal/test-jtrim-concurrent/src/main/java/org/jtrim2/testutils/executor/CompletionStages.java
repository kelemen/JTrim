package org.jtrim2.testutils.executor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskExecutionException;

public final class CompletionStages {
    // This class should be promoted to be a public API after a cleanup.

    public static <V> V get(CompletableFuture<V> future, long timeout, TimeUnit unit) {
        return get(() -> future.get(timeout, unit));
    }

    public static <V> V get(CompletableFuture<V> future) {
        return get(future::get);
    }

    public static <V> V getNow(CompletableFuture<V> future, V valueIfAbsent) {
        return get(() -> future.getNow(valueIfAbsent));
    }

    private static <V> V get(FutureResultGetter<V> getter) {
        try {
            return getter.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OperationCanceledException(ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CancellationException) {
                throw new OperationCanceledException(cause);
            }
            throw new TaskExecutionException(ex.getMessage(), cause);
        } catch (TimeoutException ex) {
            throw new OperationCanceledException(ex);
        }
    }

    public static <V> CompletableFuture<V> toSafeWaitable(CompletionStage<V> future) {
        CompletableFuture<V> resultFuture = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            if (error != null) {
                resultFuture.completeExceptionally(error);
            } else {
                resultFuture.complete(result);
            }
        });
        return resultFuture;
    }

    private interface FutureResultGetter<V> {
        public V get() throws InterruptedException, ExecutionException, TimeoutException;
    }

    private CompletionStages() {
        throw new AssertionError();
    }
}
