package org.jtrim2.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a task which is made of multiple subtasks. Once the task is finished
 * anyone may call {@link #finishTask(Object, Throwable, boolean) finishTask}
 * to finish this {@code MultiPhaseTask}. The main advantages of this class
 * is that it offers a {@link #getFuture() future} object representing the
 * {@code MultiPhaseTask} and it can notify a listener when the
 * {@code MultiPhaseTask} terminates and no more subtasks may run anymore.
 * <P>
 * Therefore to use this class, submit subtasks by one of the
 * {@code executeSubTask} or {@code submitSubTask} methods and when finished
 * invoke the {@code finishTask} method (possibly from a subtask).
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the result of this task. The result can be
 *   specified to the {@code finishTask} method and later can also be retrieved
 *   by the shared {@link #getFuture() future's} get methods.
 *
 * @author Kelemen Attila
 */
public final class MultiPhaseTask<ResultType> {
    private final Future<ResultType> future;

    private final AtomicReference<FinishResult<ResultType>> finishResult;
    private final TaskExecutorService syncExecutor;

    /**
     * The listener interface which can be notified when a specific
     * {@code MultiPhaseTask} terminates.
     *
     * <h3>Thread safety</h3>
     * This interface doesn't need to be safe to use by multiple threads
     * concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this interface are not required to be
     * <I>synchronization transparent</I>.
     *
     * @param <ResultType> the type of the result of the task this listener
     *   receives notification from
     */
    public static interface TerminateListener<ResultType> {
        /**
         * Invoked when the associated {@code MultiPhaseTask} terminates.
         * {@code MultiPhaseTask} instances may call this method only once.
         * When this method is called no more tasks scheduled to the
         * {@code MultiPhaseTask} allowed to execute (even those scheduled
         * before terminating the task).
         *
         * @param result the result of the associated {@code MultiPhaseTask}.
         *   This argument can be {@code null}, if the result of the task is
         *   {@code null}, or it did not terminate normally (i.e.: has been
         *   canceled, caused an exception).
         * @param exception the exception caused by the associated
         *   {@code MultiPhaseTask} or {@code null} if no such error occurred.
         *   Note that usually when this argument is non-null, the
         *   {@code result} is {@code null} but it is not strictly required.
         * @param canceled {@code true} if the associated {@code MultiPhaseTask}
         *   was canceled, so it could not finish its intended task,
         *   {@code false} otherwise
         */
        public void onTerminate(ResultType result, Throwable exception,
            boolean canceled);
    }

    /**
     * Creates a new non-terminated {@code MultiPhaseTask} with the given
     * terminate listener.
     *
     * @param terminateListener the listener to be notified when this task
     *   terminates. This argument can be {@code null}, if no such notification
     *   is required.
     */
    public MultiPhaseTask(
            TerminateListener<? super ResultType> terminateListener) {

        this.finishResult = new AtomicReference<>(null);
        this.future = new MultiPhaseFuture();

        this.syncExecutor = new SyncTaskExecutor();
        if (terminateListener != null) {
            this.syncExecutor.addTerminateListener(
                    new TerminateEventForwarder(terminateListener));
        }
    }

    /**
     * Returns the {@code Future} representing this task. The returned future
     * provides all the features of the {@code Future} interface. That is:
     * <ul>
     *  <li>
     *   The {@code get} methods can be used to wait for this task to terminate
     *   and retrieve its result.
     *  </li>
     *  <li>
     *   Canceling the returned future will cancel this task with no exception
     *   if it did not terminate for some other reason.
     *  </li>
     *  <li>
     *   The {@code canceled} and the {@code done} state of this task can also
     *   be queried.
     *  </li>
     * </ul>
     *
     * @return the {@code Future} representing this task. This method never
     *   returns {@code null}.
     */
    public Future<ResultType> getFuture() {
        return future;
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code UpdateTaskExecutor}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param executor the {@code UpdateTaskExecutor} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code UpdateTaskExecutor}. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code task} is {@code null}
     */
    public void executeSubTask(
            final UpdateTaskExecutor executor, final Runnable task) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        syncExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executor.execute(() -> executeSubTask(task));
        }, null);
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code TaskExecutor}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param executor the {@code TaskExecutor} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. This is the
     *   {@code CancellationToken} which will be forwarded to the specified
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @param cleanupTask the cleanup task to be executed after the specified
     *   task completes or it is determined that it will never be executed.
     *   This argument can be {@code null} if no such task is required.
     *   Note that the cleanup task will always be executed by the specified
     *   {@code TaskExecutor}.
     *
     * @throws NullPointerException thrown if either {@code executor},
     *   {@code cancelToken} or {@code task} is {@code null}
     */
    public void executeSubTask(
            final TaskExecutor executor,
            CancellationToken cancelToken,
            final CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final CleanupTask idempotentCleanup = cleanupTask != null
                ? new IdempotentCleanup(cleanupTask)
                : null;
        final CleanupTask abandonedCleanupForwarder = idempotentCleanup != null
                ? new AbandonedCleanupForwarder(executor, idempotentCleanup)
                : null;

        syncExecutor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            executor.execute(taskCancelToken, new SubTaskExecutor(task, null), idempotentCleanup);
        }, abandonedCleanupForwarder);
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code TaskExecutorService}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param executor the {@code TaskExecutorService} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. This is the
     *   {@code CancellationToken} which will be forwarded to the specified
     *   {@code TaskExecutorService}. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code TaskExecutorService}. This argument cannot be {@code null}.
     * @param cleanupTask the cleanup task to be executed after the specified
     *   task completes or it is determined that it will never be executed.
     *   This argument can be {@code null} if no such task is required. Note
     *   that the cleanup task will always be executed by the specified
     *   {@code TaskExecutorService}.
     * @return the future representing the submitted subtask. This method never
     *   returns {@code null} assuming that the specified
     *   {@code TaskExecutorService} never returns {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor},
     *   {@code cancelToken} or {@code task} is {@code null}
     */
    @SuppressWarnings("overloads")
    public TaskFuture<?> submitSubTask(
            final TaskExecutorService executor,
            CancellationToken cancelToken,
            final CancelableTask task,
            CleanupTask cleanupTask) {

        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final CleanupTask idempotentCleanup = cleanupTask != null
                ? new IdempotentCleanup(cleanupTask)
                : null;
        final CleanupTask abandonedCleanupForwarder = idempotentCleanup != null
                ? new AbandonedCleanupForwarder(executor, idempotentCleanup)
                : null;

        final ObjectRef<TaskFuture<?>> result = new ObjectRef<>(null);
        syncExecutor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            result.setValue(executor.submit(taskCancelToken, new SubTaskExecutor(task, null), idempotentCleanup));
        }, abandonedCleanupForwarder);

        TaskFuture<?> resultFuture = result.getValue();
        return resultFuture != null
                ? resultFuture
                : Tasks.canceledTaskFuture();
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code TaskExecutorService}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param <V> the type of the result of the submitted subtask
     * @param executor the {@code TaskExecutorService} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. This is the
     *   {@code CancellationToken} which will be forwarded to the specified
     *   {@code TaskExecutorService}. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code TaskExecutorService}. This argument cannot be {@code null}.
     * @param cleanupTask the cleanup task to be executed after the specified
     *   task completes or it is determined that it will never be executed.
     *   This argument can be {@code null} if no such task is required. Note
     *   that the cleanup task will always be executed by the specified
     *   {@code TaskExecutorService}.
     * @return the future representing the submitted subtask. This method never
     *   returns {@code null} assuming that the specified
     *   {@code ExecutorService} never returns {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor},
     *   {@code cancelToken} or {@code task} is {@code null}
     */
    @SuppressWarnings("overloads")
    public <V> TaskFuture<V> submitSubTask(
            final TaskExecutorService executor,
            CancellationToken cancelToken,
            final CancelableFunction<V> task,
            CleanupTask cleanupTask) {

        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final CleanupTask idempotentCleanup = cleanupTask != null
                ? new IdempotentCleanup(cleanupTask)
                : null;
        final CleanupTask abandonedCleanupForwarder = idempotentCleanup != null
                ? new AbandonedCleanupForwarder(executor, idempotentCleanup)
                : null;

        final ObjectRef<TaskFuture<V>> result = new ObjectRef<>(null);
        syncExecutor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            result.setValue(executor.submit(taskCancelToken, new SubFunctionExecutor<>(task, null), idempotentCleanup));
        }, abandonedCleanupForwarder);

        TaskFuture<V> resultFuture = result.getValue();
        return resultFuture != null
                ? resultFuture
                : Tasks.<V>canceledTaskFuture();
    }

    /**
     * Executes the specified task as a subtask of this {@code MultiPhaseTask}
     * synchronously on the calling thread. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the subtask to be executed throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public void executeSubTask(final Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        executeSubTask(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            task.run();
        }, null);
    }

    /**
     * Executes the specified task as a subtask of this {@code MultiPhaseTask}
     * synchronously on the calling thread. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the subtask to be executed throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled and not be executed. This
     *   argument cannot be {@code null}.
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @param cleanupTask the cleanup task to be executed after the specified
     *   task completes or it is determined that it will never be executed.
     *   This argument can be {@code null} if no such task is required. Note
     *   that the cleanup task will also be executed synchronously on the
     *   calling thread and is always executed.
     *
     * @throws NullPointerException thrown if the specified task or
     *   {@code CancellationToken} is {@code null}
     */
    @SuppressWarnings("overloads")
    public void executeSubTask(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        executeSubTask(cancelToken, new TaskWrapper(task), cleanupTask);
    }

    /**
     * Executes the specified task as a subtask of this {@code MultiPhaseTask}
     * synchronously on the calling thread. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the subtask to be executed throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param <V> the type of the result of the subtask to be executed
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return the return value of the subtask to be executed or {@code null}
     *   if the task could not be executed because this {@code MultiPhaseTask}
     *   had already been terminated. Note that this method may also return
     *   {@code null} if the subtask returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public <V> V executeSubTask(final Callable<V> task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        return executeSubTask(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            return task.call();
        }, null);
    }

    /**
     * Executes the specified task as a subtask of this {@code MultiPhaseTask}
     * synchronously on the calling thread. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the subtask to be executed throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param <V> the type of the result of the subtask to be executed
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled and not be executed. This
     *   argument cannot be {@code null}.
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @param cleanupTask the cleanup task to be executed after the specified
     *   task completes or it is determined that it will never be executed.
     *   This argument can be {@code null} if no such task is required. Note
     *   that the cleanup task will also be executed synchronously on the
     *   calling thread and is always executed.
     * @return the return value of the subtask to be executed or {@code null}
     *   if the task could not be executed because this {@code MultiPhaseTask}
     *   had already been terminated. Note that this method may also return
     *   {@code null} if the subtask returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified task or
     *   {@code CancellationToken} is {@code null}
     */
    @SuppressWarnings("overloads")
    public <V> V executeSubTask(
            CancellationToken cancelToken,
            final CancelableFunction<V> task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final ObjectRef<V> result = new ObjectRef<>(null);
        syncExecutor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            result.setValue(executeSubTaskAlways(taskCancelToken, task));
        }, cleanupTask);

        return result.getValue();
    }

    // This method must be called in the context of the "syncExecutor"
    private <V> V executeSubTaskAlways(
            CancellationToken cancelToken, CancelableFunction<V> task) {
        try {
            return task.execute(cancelToken);
        } catch (OperationCanceledException ex) {
            finishTask(null, ex, true);
            return null;
        } catch (Throwable ex) {
            finishTask(null, ex, false);
            return null;
        }
    }

    /**
     * Invoking this method causes this {@code MultiPhaseTask} to terminate.
     * How this task is terminated depends on the arguments of this method.
     * Note that this {@code MultiPhaseTask} may not be able to immediately
     * terminate because some subtask maybe running concurrently but will
     * terminate when those concurrently running tasks actually terminate.
     * This method guarantees that subtask submitted after this method call
     * will never execute. Note however that even subtasks submitted before this
     * call may be prevented from executing.
     *
     * @param result the result of this task. This argument can also be
     *   {@code null} if the result cannot be determined (e.g.: because the task
     *   was canceled).
     * @param exception the exception preventing this task to be completing
     *   normally. Specifying {@code null} usually means that the result
     *   is {@code null} as it could not be computed. However it is not required
     *   and some intermediate result can be published with the exception. In
     *   case this task completed normally, this argument can be {@code null}.
     * @param canceled {@code true} if this task was canceled and does not need
     *   to complete, {@code false} otherwise.
     * @return {@code true} if this {@code MultiPhaseTask} did terminate before
     *   this method returned, {@code false} if it will terminate later
     */
    public boolean finishTask(
            ResultType result,
            Throwable exception,
            boolean canceled) {

        final FinishResult<ResultType> completeResult
                = new FinishResult<>(result, exception, canceled);

        if (!finishResult.compareAndSet(null, completeResult)) {
            return syncExecutor.isTerminated();
        }
        syncExecutor.shutdown();

        return syncExecutor.isTerminated();
    }

    /**
     * Terminates this task with a {@code canceled} state. This method call is
     * equivalent to calling {@code getFuture().cancel(true)} and
     * {@code finishTask(null, null, true}.
     */
    public void cancel() {
        future.cancel(true);
    }

    /**
     * Checks whether this {@code MultiPhaseTask} has been terminated or not.
     * In case this task is terminated: No more previously submitted subtask
     * will execute (not even concurrently) and submitted subtasks will not
     * be executed as well.
     *
     * @return {@code true} if this {@code MultiPhaseTask} has been terminated,
     *   {@code false} otherwise
     */
    public boolean isDone() {
        return future.isDone();
    }

    private class MultiPhaseFuture implements Future<ResultType> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone()) {
                return isCancelled();
            }

            return finishTask(null, null, true);
        }

        @Override
        public boolean isCancelled() {
            FinishResult<?> result = finishResult.get();

            return result != null && result.isCanceled();
        }

        @Override
        public boolean isDone() {
            return syncExecutor.isTerminated();
        }

        public ResultType fetchResult()
                throws InterruptedException, ExecutionException {
            FinishResult<ResultType> result = finishResult.get();

            Throwable asyncException = result.getException();
            if (asyncException != null) {
                throw new ExecutionException(asyncException);
            }

            if (result.isCanceled()) {
                throw new CancellationException();
            }

            return result.getResult();
        }

        @Override
        public ResultType get()
                throws InterruptedException, ExecutionException {
            ExecutorsEx.awaitExecutor(ExecutorConverter.asExecutorService(syncExecutor));
            return fetchResult();
        }

        @Override
        public ResultType get(long timeout, TimeUnit unit) throws
                InterruptedException, ExecutionException, TimeoutException {

            if (!syncExecutor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, timeout, unit)) {
                throw new TimeoutException();
            }
            return fetchResult();
        }
    }

    private static class TaskWrapper implements CancelableFunction<Object> {
        private final CancelableTask task;

        public TaskWrapper(CancelableTask task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public Object execute(CancellationToken cancelToken) throws Exception {
            task.execute(cancelToken);
            return null;
        }
    }

    private static class IdempotentCleanup implements CleanupTask {
        private final AtomicReference<CleanupTask> cleanupTaskRef;

        public IdempotentCleanup(CleanupTask cleanupTask) {
            this.cleanupTaskRef = new AtomicReference<>(cleanupTask);
        }

        @Override
        public void cleanup(boolean canceled, Throwable error) throws Exception {
            CleanupTask cleanupTask = cleanupTaskRef.getAndSet(null);
            if (cleanupTask != null) {
                cleanupTask.cleanup(canceled, error);
            }
        }
    }

    // To be called as the syncExecutor cleanup task, note that the passed task
    // must be idempotent.
    private static class AbandonedCleanupForwarder implements CleanupTask {
        private final TaskExecutor executor;
        private final CleanupTask cleanupTask;

        public AbandonedCleanupForwarder(
                TaskExecutor executor, CleanupTask cleanupTask) {
            assert executor != null;
            assert cleanupTask instanceof IdempotentCleanup;
            this.executor = executor;
            this.cleanupTask = cleanupTask;
        }

        @Override
        public void cleanup(final boolean canceled, final Throwable error) throws Exception {
            if (canceled || error != null) {
                CleanupTask forwarder = (boolean ignoredArg1, Throwable ignoredArg2) -> {
                    cleanupTask.cleanup(canceled, error);
                };
                executor.execute(
                        Cancellation.UNCANCELABLE_TOKEN,
                        Tasks.noOpCancelableTask(),
                        forwarder);
            }
        }
    }

    private class SubTaskExecutor implements CancelableTask {
        private final CancelableTask task;
        private final CleanupTask cleanupTask;

        public SubTaskExecutor(CancelableTask task, CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            executeSubTask(cancelToken, new TaskWrapper(task), cleanupTask);
        }
    }

    private class SubFunctionExecutor<V> implements CancelableFunction<V> {
        private final CancelableFunction<V> task;
        private final CleanupTask cleanupTask;

        public SubFunctionExecutor(
                CancelableFunction<V> task, CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        @Override
        public V execute(CancellationToken cancelToken) {
            return executeSubTask(cancelToken, task, cleanupTask);
        }
    }

    private static class FinishResult<ResultType> {
        private final ResultType result;
        private final Throwable exception;
        private final boolean canceled;

        public FinishResult(
                ResultType result, Throwable exception, boolean canceled) {
            this.result = result;
            this.exception = exception;
            this.canceled = canceled;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public Throwable getException() {
            return exception;
        }

        public ResultType getResult() {
            return result;
        }
    }

    private static class ObjectRef<T> {
        private T value;

        public ObjectRef(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    private class TerminateEventForwarder implements Runnable {
        private final TerminateListener<? super ResultType> terminateListener;

        public TerminateEventForwarder(
                TerminateListener<? super ResultType> terminateListener) {
            this.terminateListener = terminateListener;
        }

        @Override
        public void run() {
            FinishResult<ResultType> result = finishResult.get();
            terminateListener.onTerminate(
                    result.getResult(),
                    result.getException(),
                    result.isCanceled());
        }
    }
}
