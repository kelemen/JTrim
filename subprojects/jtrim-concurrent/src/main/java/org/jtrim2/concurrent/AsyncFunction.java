package org.jtrim2.concurrent;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

/**
 * Represents an asynchronous computation. Note that although, instances of this interface represent
 * an asynchronous computation, they are not strictly required to execute the computation asynchronously.
 * That is, callers must be aware, that the computation might have been done synchronously on the calling
 * thread.
 * <P>
 * If you have a synchronous task, you can convert it using the <I>jtrim-executor</I> module with
 * {@link org.jtrim2.executor.CancelableTasks#toAsync(org.jtrim2.executor.TaskExecutor, org.jtrim2.executor.CancelableFunction) CancelableTasks.toAsync}
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of {@code CancelableFunction} is completely
 * implementation dependent, so in general they does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * {@code AsyncFunction} is not required to be <I>synchronization transparent</I>.
 *
 * @param <R> the type of the result of the asynchronous computation
 */
public interface AsyncFunction<R> {
    /**
     * Starts executing the task asynchronously and returns a {@code CompletionStage} notified
     * after the computation completes (normally or exceptionally).
     *
     * @param cancelToken the {@code CancellationToken} which can signal that the asynchronous computation
     *   is to be canceled. The computation may ignore this request. However, if it does not, it must
     *   complete exceptionally with an {@link org.jtrim2.cancel.OperationCanceledException OperationCanceledException}
     *   exception. This argument cannot be {@code null}.
     * @return the {@code CompletionStage} representing the completion of the asynchronous computation.
     *   This method may never return {@code null}.
     */
    public CompletionStage<R> executeAsync(CancellationToken cancelToken);
}
