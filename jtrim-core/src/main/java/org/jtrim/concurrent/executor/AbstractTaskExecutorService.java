package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a convenient abstract base class for {@link TaskExecutorService}
 * implementations.
 * <P>
 * {@code AbstractTaskExecutorService} defines default implementations for all
 * the {@code submit} and {@code execute} methods which all rely on the protected
 * {@link #submitTask(CancellationToken, CancellationController, CancelableTask, Runnable)}
 * method. Only this {@code submitTask} method is needed to be implemented by
 * subclasses to actually schedule a task. Note that all the {@code submit} and
 * {@code execute} methods rely directly the {@code submitTask} method and
 * overriding any of them has no effect on the others (i.e.: they don't call
 * each other). For further details on how to implement the {@code submitTask}
 * method: see its documentation.
 * <P>
 * {@code AbstractTaskExecutorService} also defines a default implementation for
 * the {@link #awaitTermination(CancellationToken)} method. The implementation
 * of this method simply calls repeatedly the other variant of
 * {@code awaitTermination} until it returns {@code true}.
 *
 * @author Kelemen Attila
 */
public abstract class AbstractTaskExecutorService
implements
        TaskExecutorService {

    private static final Logger LOGGER = Logger.getLogger(AbstractTaskExecutorService.class.getName());

    /**
     * Implementations must override this method to actually execute submitted
     * tasks.
     * <P>
     * Assuming no cancellation requests, implementations must first execute
     * {@code task} then after the task terminates, they must execute
     * {@code cleanupTask} (notice that {@code cleanupTask} is a simple
     * {@code Runnable}). Implementations must ensure that the
     * {@code cleanupTask} is executed always, regardless of the circumstances
     * and they must also ensure, that it is not executed concurrently with
     * {@code task}. Note that {@code AbstractTaskExecutorService} will catch
     * every exception {@code task} may throw (i.e.: anything extending
     * {@code Throwable}, even {@link OperationCanceledException}). Therefore if
     * {@code task} throws an exception, it can be considered an error in
     * {@code AbstractTaskExecutorService}.
     * <P>
     * Cancellation requests can be detected using the provided
     * {@code CancellationToken} and if an implementation chooses not to even
     * try to execute {@code task}, it must only call {@code cleanupTask}. The
     * {@code submit} and {@code execute} implementations assume, that if
     * {@code cleanupTask} has been called, {@code task} will not be called and
     * the task was canceled.
     * <P>
     * It might be possible, that an implementation wishes to cancel
     * {@code task} after it has been started (possibly due to a
     * {@code shutdownAndCancel} request). This can be done by the provided
     * {@link CancellationController} which in will cause the task to be
     * canceled and the passed {@code CancellationToken} signaling cancellation
     * (it will not cause, the {@code CancellationToken} passed to the
     * {@code submit} or the {@code execute} methods to signal cancellation
     * request).
     * <P>
     * The specified {@code cleanupTask} must always be executed (this is the
     * way {@code AbstractTaskExecutorService} is able to detect that the task
     * has terminated) but the {@code hasUserDefinedCleanup} may allow some
     * optimization possibilities for implementations. This is because if there
     * was no user defined cleanup task specified, the {@code cleanupTask} is
     * <I>synchronization transparent</I> and as such, can be called from any
     * context (although it is still recommended not to execute the
     * {@code cleanupTask} while holding a lock).
     * <P>
     * In case there is a user defined cleanup task and it throws an exception,
     * the exception is logged by {@code AbstractTaskExecutorService} with the
     * log level {@code Level.WARNING}. The exception thrown will be hidden from
     * the implementations executing the {@code cleanupTask} and so they may
     * expect the {@code cleanupTask} not to throw an exception.
     * <P>
     * Note that none of the passed argument is {@code null}, this is enforced
     * by the {@code AbstractTaskExecutorService}, so implementations may safely
     * assume the arguments to be non-null and does not need to verify them.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked by
     *   implementations if the currently submitted task has been canceled.
     *   Also this is the {@code CancellationToken} implementations should pass
     *   to {@code task}. This argument cannot be {@code null}.
     * @param cancelController the {@code CancellationController} which can be
     *   used by implementations to make the specified {@code CancellationToken}
     *   signal a cancellation request. This argument cannot be {@code null}.
     * @param task the {@code CancelableTask} whose {@code execute} method is
     *   to be executed. Implementations must execute this task at most once and
     *   only before calling {@code cleanupTask.run()}. This argument cannot be
     *   {@code null}. Note that this task will not throw any exception,
     *   {@code AbstractTaskExecutorService} will catch every exception thrown
     *   by the submitted task.
     * @param cleanupTask the {@code Runnable} whose {@code run} method must be
     *   invoked after the specified task has completed, or the implementation
     *   chooses never to execute the task. This cleanup task must be executed
     *   always regardless of the circumstances, and it must be executed exactly
     *   once. Note that it can be expected that this {@code cleanupTask} does
     *   not throw any exception. This argument cannot be {@code null}.
     * @param hasUserDefinedCleanup {@code true} if the specified
     *   {@code cleanupTask} needs to execute a user defined cleanup task,
     *   {@code false} otherwise. In case this argument is {@code false}, the
     *   {@code cleanupTask} can be considered
     *   <I>synchronization transparent</I>.
     */
    protected abstract void submitTask(
            CancellationToken cancelToken,
            CancellationController cancelController,
            CancelableTask task,
            Runnable cleanupTask,
            boolean hasUserDefinedCleanup);

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        while (!awaitTermination(cancelToken, Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
            // Repeat until it has been terminated, or throws an exception.
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        callSubmitTask(cancelToken, task, cleanupTask);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        return callSubmitTask(cancelToken, task, cleanupTask);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask) {
        return callSubmitTask(cancelToken, task, cleanupTask);
    }

    private <V> TaskFuture<V> callSubmitTask(
            CancellationToken userCancelToken,
            CancelableTask userTask,
            CleanupTask userCleanupTask) {

        return callSubmitTask(
                userCancelToken,
                new FunctionWrapper<V>(userTask),
                userCleanupTask);
    }

    // This implementation prevents errors (that is, it will always be in a
    // consistent state) from misuses and usually responds with an
    // IllegalStateException (for example if the cleanup task is called
    // multiple times).
    private <V> TaskFuture<V> callSubmitTask(
            CancellationToken userCancelToken,
            CancelableFunction<V> userFunction,
            CleanupTask userCleanupTask) {
        ExceptionHelper.checkNotNullArgument(userCancelToken, "userCancelToken");
        ExceptionHelper.checkNotNullArgument(userFunction, "userFunction");

        if (isShutdown() || userCancelToken.isCanceled()) {
            if (userCleanupTask == null) {
                return Tasks.canceledTaskFuture();
            }

            Runnable cleanupTask = new UserCleanupWrapper(userCleanupTask, true, null);
            cleanupTask = Tasks.runOnceTask(cleanupTask, true);

            submitTask(
                    CancellationSource.CANCELED_TOKEN,
                    DummyCancellationController.INSTANCE,
                    Tasks.noOpCancelableTask(),
                    cleanupTask,
                    true);
            return Tasks.canceledTaskFuture();
        }

        final AtomicReference<CancelableFunction<V>> userFunctionRef
                = new AtomicReference<>(userFunction);
        final PostExecuteCleanupTask postExecuteCleanup
                = new PostExecuteCleanupTask();
        AtomicReference<TaskState> currentState = new AtomicReference<>(TaskState.NOT_STARTED);
        AtomicReference<TaskResult<V>> resultRef = new AtomicReference<>(TaskResult.<V>getCanceledResult());
        WaitableSignal waitDoneSignal = new WaitableSignal();

        final TaskFinalizer<V> taskFinalizer;
        taskFinalizer = new TaskFinalizer<>(postExecuteCleanup, currentState,
                resultRef, waitDoneSignal, userCleanupTask);

        final CancellationSource newCancelSource = new CancellationSource();
        postExecuteCleanup.setCancelRef(userCancelToken.addCancellationListener(
                new Runnable() {
            @Override
            public void run() {
                newCancelSource.getController().cancel();
                if (userFunctionRef.getAndSet(null) != null) {
                    taskFinalizer.finish();
                }
            }
        }));

        CancelableTask task = new TaskOfAbstractExecutor<>(
                userFunctionRef, currentState, resultRef, taskFinalizer);
        task = Tasks.runOnceCancelableTask(task, true);

        Runnable cleanupTask = new CleanupTaskOfAbstractExecutor(
                postExecuteCleanup, taskFinalizer);
        cleanupTask = Tasks.runOnceTask(cleanupTask, true);

        submitTask(newCancelSource.getToken(), newCancelSource.getController(),
                task, cleanupTask, userCleanupTask != null);

        return new TaskFutureOfAbstractExecutor<>(
                currentState,
                resultRef,
                waitDoneSignal);
    }

    private enum DummyCancellationController implements CancellationController {
        INSTANCE;

        @Override
        public void cancel() {
        }
    }

    private static class FunctionWrapper<V> implements CancelableFunction<V> {
        private final CancelableTask task;

        public FunctionWrapper(CancelableTask task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public V execute(CancellationToken cancelToken) {
            task.execute(cancelToken);
            return null;
        }
    }

    private static class TaskResult<V> {
        private static final TaskResult<?> CANCELED = new TaskResult<>(null, true);

        @SuppressWarnings("unchecked")
        public static <V> TaskResult<V> getCanceledResult() {
            // This cast is safe, since it returns null for the result which
            // is valid for every kind of objects.
            return (TaskResult<V>)CANCELED;
        }

        public final V result;
        public final Throwable error;
        public final boolean canceled;

        public TaskResult(V result) {
            this.result = result;
            this.error = null;
            this.canceled = false;
        }

        public TaskResult(Throwable error, boolean canceled) {
            this.result = null;
            this.error = error;
            this.canceled = canceled;
        }
    }

    private static class TaskOfAbstractExecutor<V>
    implements
            CancelableTask {

        private final AtomicReference<CancelableFunction<V>> functionRef;
        private final AtomicReference<TaskState> currentState;
        private final AtomicReference<TaskResult<V>> resultRef;
        private final TaskFinalizer<V> taskFinalizer;
        private final AtomicBoolean executed;

        public TaskOfAbstractExecutor(
                AtomicReference<CancelableFunction<V>> functionRef,
                AtomicReference<TaskState> currentState,
                AtomicReference<TaskResult<V>> resultRef, TaskFinalizer<V> taskFinalizer) {

            this.functionRef = functionRef;
            this.currentState = currentState;
            this.resultRef = resultRef;
            this.taskFinalizer = taskFinalizer;
            this.executed = new AtomicBoolean(false);
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            if (executed.getAndSet(true)) {
                throw new IllegalStateException("Multiple execute call "
                        + "of the task of AbstractTaskExecutorService.");
            }
            if (!currentState.compareAndSet(TaskState.NOT_STARTED, TaskState.RUNNING)) {
                // The task was canceled prior executing
                return;
            }

            CancelableFunction<V> function = functionRef.getAndSet(null);
            if (function == null) {
                // This task was canceled before it was executed, so
                // just return silently. In this case event the
                // taskFinalizer.finish() was called.
            }

            // This default null value should be overwritten in every case
            TaskResult<V> taskResult = null;
            try {
                V result = function.execute(cancelToken);
                taskResult = new TaskResult<>(result);
            } catch (OperationCanceledException ex) {
                taskResult = new TaskResult<>(ex, true);
            } catch (Throwable ex) {
                taskResult = new TaskResult<>(ex, false);
            } finally {
                // This check should always succeed (not counting Thread.stop
                // and similar methods) because of the "catch Throwable".
                if (taskResult != null) {
                    // resultRef is only set here and due to the first check, it
                    // can only overwrite the initial value
                    resultRef.set(taskResult);
                }
                taskFinalizer.finish();
            }
        }
    }

    // The job of TaskFinalizer is to set the state of the task to finished.
    // finish() is called from the submitted task and the cleanup method.
    //
    // TaskFinalizer also calls a post execute cleanup task, which currently
    // unregisters the cancellation listener which was only registered to
    // forward cancellation request to the cancellation token passed to
    // submitTask. This cancellation forwarding is not useful after the task
    // completes, since there is nothing we can do at that point.
    //
    // Whenever finish() is called for the first time, the only task remaining
    // is to call the cleanup method which does not need the result of the task
    // and the post cleanup task makes sure that after it has been executed it
    // will no longer retain references to possibly heavy objects.
    private static class TaskFinalizer<V> {
        private final AtomicReference<TaskState> currentState;
        private final WaitableSignal waitDoneSignal;
        private final AtomicBoolean finished;
        private final CleanupTask userCleanupTask;

        private final PostExecuteCleanupTask postExecuteCleanup;
        // unset after finish() returns.
        // i.e.: only available for the first call of finish()
        private AtomicReference<TaskResult<V>> resultRef;

        // This field is only set after finish() returns, in whic case
        // finished.get() == true
        private volatile Runnable cleanupTask;

        public TaskFinalizer(
                PostExecuteCleanupTask postExecuteCleanup,
                AtomicReference<TaskState> currentState,
                AtomicReference<TaskResult<V>> resultRef,
                WaitableSignal waitDoneSignal,
                CleanupTask userCleanupTask) {

            this.postExecuteCleanup = postExecuteCleanup;
            this.currentState = currentState;
            this.resultRef = resultRef;
            this.waitDoneSignal = waitDoneSignal;
            this.userCleanupTask = userCleanupTask;
            this.finished = new AtomicBoolean(false);
            this.cleanupTask = null;
        }

        public Runnable finish() {
            if (!finished.getAndSet(true)) {
                TaskResult<V> result = resultRef.get();
                try {
                    // Do not reference it anymore, so that the result of
                    // the task will not be retained until the cleanup
                    // task is done.
                    resultRef = null;

                    TaskState terminateState;
                    if (result.canceled) {
                        terminateState = TaskState.DONE_CANCELED;
                    }
                    else if (result.error != null) {
                        terminateState = TaskState.DONE_ERROR;
                    }
                    else {
                        terminateState = TaskState.DONE_COMPLETED;
                    }

                    currentState.set(terminateState);
                    waitDoneSignal.signal();

                    postExecuteCleanup.run();
                } finally {
                    if (userCleanupTask != null) {
                        cleanupTask = new UserCleanupWrapper(
                                userCleanupTask,
                                result.canceled,
                                result.error);
                    }
                    else {
                        cleanupTask = Tasks.noOpTask();
                    }
                }
            }

            Runnable result = cleanupTask;
            if (result == null) {
                // First of all, finish is called from two places:
                // from the submitted task and the cleanup method.
                // If finishResult was not yet set but finished is
                // already set, it means that the first finish call
                // is executing concurrently which is not allowed.
                throw new IllegalStateException(
                        "task and cleanup were called concurrently.");
            }
            return result;
        }
    }

    private static class UserCleanupWrapper implements Runnable {
        private final CleanupTask userCleanupTask;
        private final boolean canceled;
        private final Throwable error;

        public UserCleanupWrapper(
                CleanupTask userCleanupTask,
                boolean canceled,
                Throwable error) {

            this.userCleanupTask = userCleanupTask;
            this.canceled = canceled;
            this.error = error;
        }

        @Override
        public void run() {
            try {
                userCleanupTask.cleanup(canceled, error);
            } catch (Throwable ex) {
                LOGGER.log(Level.WARNING,
                        "The cleanup task has thrown an exception.", ex);
            }
        }
    }

    // Must be idempotent, thread safe and synchronization transparent because
    // it will be called after execute and when cleaning up and when the task
    // got canceled.
    private static class PostExecuteCleanupTask implements Runnable {
        private volatile ListenerRef cancelRef;
        private volatile boolean executed;

        public PostExecuteCleanupTask() {
            this.cancelRef = null;
        }

        public void setCancelRef(ListenerRef cancelRef) {
            this.cancelRef = cancelRef;
            if (executed) {
                run();
            }
        }

        @Override
        public void run() {
            executed = true;

            ListenerRef currentCancelRef = cancelRef;
            cancelRef = null;
            if (currentCancelRef != null) {
                currentCancelRef.unregister();
            }
        }
    }

    private static class CleanupTaskOfAbstractExecutor implements Runnable {
        private final PostExecuteCleanupTask postExecuteCleanup;
        private final TaskFinalizer<?> taskFinalizer;

        public CleanupTaskOfAbstractExecutor(
                PostExecuteCleanupTask postExecuteCleanup,
                TaskFinalizer<?> taskFinalizer) {

            this.postExecuteCleanup = postExecuteCleanup;
            this.taskFinalizer = taskFinalizer;
        }

        @Override
        public void run() {
            try {
                postExecuteCleanup.run();
            } finally {
                taskFinalizer.finish().run();
            }
        }
    }

    private static class TaskFutureOfAbstractExecutor<V>
    implements
            TaskFuture<V> {

        private final AtomicReference<TaskState> currentState;
        private final AtomicReference<TaskResult<V>> resultRef;
        private final WaitableSignal waitDoneSignal;

        public TaskFutureOfAbstractExecutor(
                AtomicReference<TaskState> currentState,
                AtomicReference<TaskResult<V>> resultRef,
                WaitableSignal waitDoneSignal) {

            this.currentState = currentState;
            this.resultRef = resultRef;
            this.waitDoneSignal = waitDoneSignal;
        }

        @Override
        public TaskState getTaskState() {
            return currentState.get();
        }

        private V fetchResult() {
            assert getTaskState().isDone();

            TaskResult<V> result = resultRef.get();
            Throwable resultException = result.error;
            if (resultException != null) {
                throw new TaskExecutionException(resultException);
            }
            else if (result.canceled) {
                // We pass the causing exception, to preserve the stack
                // trace of the point of cancellation.
                throw new OperationCanceledException(resultException);
            }
            return result.result;
        }

        @Override
        public V tryGetResult() {
            if (getTaskState().isDone()) {
                return fetchResult();
            }
            return getTaskState().isDone() ? fetchResult() : null;
        }

        @Override
        public V waitAndGet(CancellationToken cancelToken) {
            waitDoneSignal.waitSignal(cancelToken);
            return fetchResult();
        }

        @Override
        public V waitAndGet(CancellationToken cancelToken, long timeout, TimeUnit timeUnit) {
            if (!waitDoneSignal.waitSignal(cancelToken, timeout, timeUnit)) {
                throw new OperationCanceledException();
            }
            return fetchResult();
        }
    }
}
