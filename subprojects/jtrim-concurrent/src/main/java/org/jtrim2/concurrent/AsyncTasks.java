package org.jtrim2.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
     * @param <R> any type
     * @param error the error to be logged if not {@code null}. This argument can be {@code null},
     *   in which case, this method does nothing.
     * @return always null
     *
     * @see #isError(Throwable)
     */
    public static <R> R expectNoError(Throwable error) {
        if (isError(error)) {
            LOGGER.log(Level.SEVERE, "Uncaught exception in task.", error);
        }
        return null;
    }

    /**
     * Returns {@code true} if the given exception represents a cancellation event.
     *
     * @param error the exception to be checked if it represents a cancellation event.
     *   This argument can be {@code null}, in which case, the return value is {@code false}.
     * @return {@code true} if the given exception represents a cancellation event,
     *   {@code false} otherwise
     */
    public static boolean isCanceled(Throwable error) {
        return error instanceof OperationCanceledException;
    }

    /**
     * Returns {@code true} if the given exception represents an error event. That is,
     * if the given exception is {@code null} or {@link #isCanceled(Throwable) represents cancellation}.
     *
     * @param error the exception to be checked if it represents an error event.
     *   This argument can be {@code null}, in which case, the return value is {@code false}.
     * @return {@code true} if the given exception represents an error event,
     *   {@code false} otherwise
     */
    public static boolean isError(Throwable error) {
        return error != null && !isCanceled(error);
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
        } else {
            future.complete(result);
        }
    }

    /**
     * Returns a {@code BiConsumer} notifying the passed error handler if any of the
     * arguments of the returned {@code BiConsumer} is not {@code null}. See
     * {@link #handleErrorResult(Throwable, Throwable, Consumer) handleErrorResult}
     * for further explanation.
     * <P>
     * The returned {@code BiConsumer} was designed to be passable to the
     * {@code whenComplete} method of {@code CompletionStage}.
     *
     * @param <E> the type of the result of the asynchronous computation
     * @param errorHandler the handler to be notified in case of an error. This
     *   argument cannot be {@code null}.
     * @return a {@code BiConsumer} notifying the passed error handler if any of the
     *   arguments of the returned {@code BiConsumer} is not {@code null}. This method
     *   never returns {@code null}.
     *
     * @see #handleErrorResult(Throwable, Throwable, Consumer)
     */
    public static <E extends Throwable> BiConsumer<E, Throwable> errorResultHandler(
            Consumer<? super Throwable> errorHandler) {
        Objects.requireNonNull(errorHandler, "errorHandler");
        return (result, error) -> {
            handleErrorResult(result, error, errorHandler);
        };
    }

    /**
     * Calls the given error handler if any of the exception arguments is not {@code null}.
     * If none of the exception arguments is {@code null}, the {@code result} argument is
     * passed and the {@code error} argument is added as a suppressed exception. If both argument
     * is {@code null}, the handler is not called.
     * <P>
     * This method is useful if an asynchronous computation returns a {@code Throwable}
     * as a result but may also throw an exception.
     *
     * @param result the result exception which takes precedence over the other
     *   exception argument. This argument can be {@code null}.
     * @param error the error which will only be (directly) passed to the handler
     *   if the {@code result} argument is {@code null}. This argument can be {@code null}.
     * @param errorHandler the handler to be notified if any of the exception arguments is
     *   not {@code null}. This argument cannot be {@code null}.
     *
     * @see #errorResultHandler(Consumer) errorResultHandler
     */
    public static void handleErrorResult(
            Throwable result,
            Throwable error,
            Consumer<? super Throwable> errorHandler) {
        Objects.requireNonNull(errorHandler, "errorHandler");

        if (result != null) {
            if (error != null) {
                result.addSuppressed(error);
            }
            errorHandler.accept(result);
        } else if (error != null) {
            errorHandler.accept(error);
        }
    }

    private AsyncTasks() {
        throw new AssertionError();
    }
}
