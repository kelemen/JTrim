package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.event.ListenerRef;

/**
 * @see ExecutorConverter#asTaskExecutor(Executor)
 */
final class ExecutorAsTaskExecutor implements TaskExecutor {
    // It is accessed in the factory method, when attempting to convert it back.
    final Executor executor;
    private final boolean mayInterruptTask;

    public ExecutorAsTaskExecutor(Executor executor, boolean mayInterruptTask) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.mayInterruptTask = mayInterruptTask;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        return executeOnExecutor(mayInterruptTask, executor, cancelToken, function);
    }

    public static <V> CompletionStage<V> executeOnExecutor(
            boolean mayInterruptTask,
            Executor executor,
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(function, "function");

        AtomicReference<CancelableFunction<? extends V>> functionRef = new AtomicReference<>(function);
        return executeOnExecutor0(mayInterruptTask, executor, cancelToken, functionRef);
    }

    private static <V> CompletionStage<V> executeOnExecutor0(
            boolean mayInterruptTask,
            Executor executor,
            CancellationToken cancelToken,
            AtomicReference<CancelableFunction<? extends V>> functionRef) {

        CompletableFuture<V> future = new CompletableFuture<>();

        ListenerRef cancelRef = cancelToken.addCancellationListener(() -> functionRef.set(null));
        executor.execute(() -> {
            try {
                executeNow(mayInterruptTask, cancelToken, functionRef, future);
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            } finally {
                cancelRef.unregister();
            }
        });

        return future;
    }

    private static <V> void executeNow(
            boolean mayInterruptTask,
            CancellationToken cancelToken,
            AtomicReference<CancelableFunction<? extends V>> functionRef,
            CompletableFuture<V> future) throws Exception {

        CancelableFunction<? extends V> function = functionRef.get();
        if (function != null) {
            if (mayInterruptTask) {
                Cancellation.doAsCancelable(cancelToken, (CancellationToken taskCancelToken) -> {
                    CancelableTasks.complete(taskCancelToken, function, future);
                    return null;
                });
            } else {
                V result = function.execute(cancelToken);
                future.complete(result);
            }
        } else {
            future.completeExceptionally(OperationCanceledException.withoutStackTrace());
        }
    }
}
