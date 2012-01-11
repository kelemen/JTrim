package org.jtrim.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jtrim.utils.*;

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
 * The methods of this interface are not <I>synchronization transparent</I>.
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
    private final ExecutorService syncExecutor;

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

        TaskRefusePolicy refusePolicy = SilentTaskRefusePolicy.INSTANCE;
        if (terminateListener != null) {
            this.syncExecutor = new SyncTaskExecutor(refusePolicy,
                    new TerminateEventForwarder(terminateListener));
        }
        else {
            this.syncExecutor = new SyncTaskExecutor(refusePolicy);
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
    public void executeSubTask(final UpdateTaskExecutor executor, final Runnable task) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                executor.execute(new SubTaskExecutor(task));
            }
        });
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code Executor}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param executor the {@code Executor} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code Executor}. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code task} is {@code null}
     */
    public void executeSubTask(final Executor executor, final Runnable task) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                executor.execute(new SubTaskExecutor(task));
            }
        });
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code ExecutorService}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param executor the {@code ExecutorService} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code ExecutorService}. This argument cannot be {@code null}.
     * @return the future representing the submitted subtask. This method never
     *   returns {@code null} assuming that the specified
     *   {@code ExecutorService} never returns {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code task} is {@code null}
     */
    public Future<?> submitSubTask(final ExecutorService executor,
            final Runnable task) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final ObjectRef<Future<?>> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executor.submit(new SubTaskExecutor(task)));
            }
        });

        return result.getValue();
    }

    /**
     * Submits the specified task as a subtask of this {@code MultiPhaseTask} to
     * the given {@code ExecutorService}. The submitted task will not be
     * actually executed if this {@code MultiPhaseTask} had been terminated
     * before it is actually scheduled.
     * <P>
     * If the submitted subtask throws an exception, this
     * {@code MultiPhaseTask} will immediately be terminated with the given
     * exception (non-canceled and {@code null} as a result).
     *
     * @param <V> the type of the result of the submitted subtask
     * @param executor the {@code ExecutorService} to which the specified
     *   task will be submitted to. This argument cannot be {@code null}.
     * @param task the task to be executed on the specified
     *   {@code ExecutorService}. This argument cannot be {@code null}.
     * @return the future representing the submitted subtask. This method never
     *   returns {@code null} assuming that the specified
     *   {@code ExecutorService} never returns {@code null}.
     *
     * @throws NullPointerException thrown if either {@code executor} or
     *   {@code task} is {@code null}
     */
    public <V> Future<V> submitSubTask(final ExecutorService executor,
            final Callable<V> task) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(task, "task");

        final ObjectRef<Future<V>> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executor.submit(new SubCallableExecutor<>(task)));
            }
        });

        return result.getValue();
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

        executeSubTask(new RunnableWrapper(task));
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

        final ObjectRef<V> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executeSubTaskAlways(task));
            }
        });

        return result.getValue();
    }

    // This method must be called in the context of the "syncExecutor"
    private <V> V executeSubTaskAlways(Callable<V> task) {
        try {
            return task.call();
        } catch (InterruptedException ex) {
            finishTask(null, ex, false);
            Thread.currentThread().interrupt();
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
            return false;
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

        public ResultType fetchResult() throws InterruptedException, ExecutionException {
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
        public ResultType get() throws InterruptedException, ExecutionException {
            ExecutorsEx.awaitExecutor(syncExecutor);
            return fetchResult();
        }

        @Override
        public ResultType get(long timeout, TimeUnit unit) throws
                InterruptedException, ExecutionException, TimeoutException {

            if (!syncExecutor.awaitTermination(timeout, unit)) {
                throw new TimeoutException();
            }
            return fetchResult();
        }
    }

    private static class RunnableWrapper implements Callable<Object> {
        private final Runnable task;

        public RunnableWrapper(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public Object call() {
            task.run();
            return null;
        }
    }

    private class SubTaskExecutor implements Runnable {
        private final Runnable task;

        public SubTaskExecutor(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public void run() {
            executeSubTask(new RunnableWrapper(task));
        }
    }

    private class SubCallableExecutor<V> implements Callable<V> {
        private final Callable<V> task;

        public SubCallableExecutor(Callable<V> task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public V call() {
            return executeSubTask(task);
        }
    }

    private static class FinishResult<ResultType> {
        private final ResultType result;
        private final Throwable exception;
        private final boolean canceled;

        public FinishResult(ResultType result, Throwable exception, boolean canceled) {
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

    private class TerminateEventForwarder implements ExecutorShutdownListener {
        private final TerminateListener<? super ResultType> terminateListener;

        public TerminateEventForwarder(TerminateListener<? super ResultType> terminateListener) {
            this.terminateListener = terminateListener;
        }

        @Override
        public void onTerminate() {
            FinishResult<ResultType> result = finishResult.get();
            terminateListener.onTerminate(
                    result.getResult(),
                    result.getException(),
                    result.isCanceled());
        }
    }
}
