package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;

/**
 * Executes tasks at some time in the future. This interface defines a more
 * robust way to execute tasks than {@code java.util.concurrent.Executor}.
 * That is, this interface defines a simpler way for canceling tasks and allows
 * to continue after the completion of the submitted task via a {@link CompletionStage}.
 * It is possible to continue execution regardless how the submitted task completed
 * (success, failure or canceled).
 * For more control over the life of a {@code TaskExecutor}, see the extending
 * {@link TaskExecutorService} interface.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, handlers added
 * to {@code CompletionStage}, etc.
 *
 * @see CancelableTask
 * @see TaskExecutorService
 * @see TaskExecutors#upgradeToStoppable(TaskExecutor)
 */
public interface TaskExecutor extends Executor {
    /**
     * Executes the function at some time in the future. When and on
     * what thread, the function is to be executed is completely implementation
     * dependent. Implementations may choose to execute tasks later on a
     * separate thread or synchronously in the calling thread at the discretion
     * of the implementation.
     *
     * @param <V> the type of the result of the submitted function
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. If this
     *   {@code CancellationToken} signals a cancellation request, this
     *   {@code TaskExecutor} may choose to not even attempt to execute the
     *   submitted task. This argument may not be {@code null}. When the task cannot be
     *   canceled, use the static {@link org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN}
     *   for this argument (even in this case, the {@code TaskExecutor} may be able to
     *   cancel the task, if it was not submitted for execution).
     * @param function the function to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @return the {@code CompletionStage} which can be used to execute tasks
     *   after the completion of the submitted function and process the result of
     *   the submitted function. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the {@code CancellationToken}
     *   or the task is {@code null}
     *
     * @see org.jtrim2.cancel.Cancellation#createCancellationSource()
     * @see org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN
     */
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function);

    /**
     * Executes the task at some time in the future. When and on what thread, the task is
     * to be executed is completely implementation dependent. Implementations may
     * choose to execute tasks later on a separate thread or synchronously in the
     * calling thread at the discretion of the implementation.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. If this
     *   {@code CancellationToken} signals a cancellation request, this
     *   {@code TaskExecutor} may choose to not even attempt to execute the
     *   submitted task. This argument may not be {@code null}. When the task cannot be
     *   canceled, use the static {@link org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN}
     *   for this argument (even in this case, the {@code TaskExecutor} may be able to
     *   cancel the task, if it was not submitted for execution).
     * @param task the task to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @return the {@code CompletionStage} which can be used to execute tasks
     *   after the completion of the submitted task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the {@code CancellationToken}
     *   or the task is {@code null}
     *
     * @see org.jtrim2.cancel.Cancellation#createCancellationSource()
     * @see org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN
     */
    public default CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
        Objects.requireNonNull(task, "task");
        return executeFunction(cancelToken, (taskCancelToken) -> {
            task.execute(taskCancelToken);
            return null;
        });
    }

    /**
     * Executes an non-cancelable task at some time in the future. When and on what thread,
     * the task is to be executed is completely implementation dependent. Implementations may
     * choose to execute tasks later on a separate thread or synchronously in the
     * calling thread at the discretion of the implementation.
     * <P>
     * Note that although the task itself is not cancelable explicitly, the executor might cancel
     * it by not executing the submitted task. For example, due to a call to the
     * {@link TaskExecutorService#shutdownAndCancel() shutdownAndCancel} method of
     * {@link TaskExecutorService}.
     * <P>
     * The default implementation delegates the call to
     * {@link #execute(CancellationToken, CancelableTask) execute(CancellationToken, CancelableTask)}.
     *
     * @param task the task to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @return the {@code CompletionStage} which can be used to execute tasks
     *   after the completion of the submitted task. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the task is {@code null}
     *
     * @see org.jtrim2.cancel.Cancellation#createCancellationSource()
     * @see org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN
     */
    public default CompletionStage<Void> executeStaged(Runnable task) {
        Objects.requireNonNull(task, "command");
        return execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> task.run());
    }

    /**
     * {@inheritDoc }
     * <P>
     * Note that although the task itself is not cancelable explicitly, the executor might cancel
     * it by not executing the submitted task. For example, due to a call to the
     * {@link TaskExecutorService#shutdownAndCancel() shutdownAndCancel} method of
     * {@link TaskExecutorService}.
     * <P>
     * The default implementation delegates the call to {@link #executeStaged(Runnable) executeStaged} and
     * logs uncaught errors thrown by the submitted task.
     */
    @Override
    public default void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        executeStaged(command).exceptionally(AsyncTasks::expectNoError);
    }
}
