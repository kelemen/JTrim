package org.jtrim2.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.OperationCanceledException;

/**
 * Defines utility methods to help with asynchronous tasks relying on
 * {@code CompletableFuture} or {@code CompletionStage}.
 */
public final class AsyncTasks {
    private static final Logger LOGGER = Logger.getLogger(AsyncTasks.class.getName());

    /**
     * Logs the argument as a {@code SEVERE} level issue, if it is not {@code null}
     * and does not represent cancellation.
     * <P>
     * This function is expected to be used with {@code CompletionStage}'s
     * {@code exceptionally} method to log any uncaught error. This is important to do as a last action
     * because otherwise the stack trace of the thrown exception will be lost. That is, the intended use is:
     * <pre>{@code
     * CompletionStage<?> future = ...;
     * future.exceptionally(Tasks::expectNoError);
     * }</pre>
     * <P>
     * This method does not log {@code OperationCanceledException} because
     * cancellation is considered as a normal event.
     *
     * @param error the error to be logged if not {@code null}. This argument can be {@code null},
     *   in which case, this method does nothing.
     * @return always null
     */
    public static Void expectNoError(Throwable error) {
        if (error != null && !(error instanceof OperationCanceledException)) {
            LOGGER.log(Level.SEVERE, "Uncaught exception in task.", error);
        }
        return null;
    }

    /**
     * Returns a {@code BiConsumer} passable to the {@code whenComplete} method of {@code CompletionStage}
     * completing the passed {@code CompletableFuture}. That is, the returned {@code BiConsumer}
     * simply calls the {@link #complete(Object, Throwable, CompletableFuture) complete} method
     * with the arguments passed to the returned {@code BiConsumer}.
     *
     * @param <V> the type of the result of the asynchronous computation
     * @param future the future to be completed by the returned {@code BiConsumer}. This argument
     *   cannot be {@code null}.
     * @return a {@code BiConsumer} passable to the {@code whenComplete} method of {@code CompletionStage}
     *   completing the passed {@code CompletableFuture}. This method never returns {@code null}.
     */
    public static <V> BiConsumer<V, Throwable> completeForwarder(CompletableFuture<? super V> future) {
        Objects.requireNonNull(future, "future");
        return (result, error) -> complete(result, error, future);
    }

    /**
     * Completes the passed future exceptionally or normally. This method was designed to be called
     * from the {@code whenComplete} method of {@code CompletionStage}.
     * <P>
     * If the passed exception is not {@code null}, the passed future is completed exceptionally with
     * the given error. Otherwise, the passed future is completed normally with the given {@code result}.
     *
     * @param <V> the type of the result of the asynchronous computation
     * @param result the result of the asynchronous computation to complete the passed future normally with.
     *   This argument can be {@code null}, if the asynchronous computation yielded {@code null} result,
     *   and should be {@code null} if the passed error is not {@code null}.
     * @param error the exception to complete the passed future with (if not {@code null}. This
     *   argument can be {@code null}, if the asynchronous computation completed normally. However,
     *   if not {@code null}, the result argument will be ignored.
     * @param future the {@code CompletableFuture} to be completed. This future will always be
     *   completed after this method returns. This argument cannot be {@code null}.
     */
    public static <V> void complete(V result, Throwable error, CompletableFuture<? super V> future) {
        if (error != null) {
            future.completeExceptionally(error);
        }
        else {
            future.complete(result);
        }
    }

    private AsyncTasks() {
        throw new AssertionError();
    }
}
