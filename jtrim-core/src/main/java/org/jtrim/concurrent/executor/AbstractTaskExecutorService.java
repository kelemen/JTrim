package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
     * {@code Throwable}, even {@link TaskCanceledException}). Therefore if
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
     *   once. This argument cannot be {@code null}.
     */
    protected abstract void submitTask(
            CancellationToken cancelToken,
            CancellationController cancelController,
            CancelableTask task,
            Runnable cleanupTask);

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
    public void execute(CancellationToken cancelToken, CancelableTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        WrapperTask<?> wrapper = new WrapperTask<>(
                cancelToken,
                new FunctionWrapper(task),
                null);

        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");

        WrapperTask<?> wrapper = new WrapperTask<>(
                cancelToken,
                new FunctionWrapper(task),
                cleanupTask);

        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            CancelableTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        WrapperTask<?> wrapper = new WrapperTask<>(
                cancelToken,
                new FunctionWrapper(task),
                null);

        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
        return wrapper.getTaskFuture();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");

        WrapperTask<?> wrapper = new WrapperTask<>(
                cancelToken,
                new FunctionWrapper(task),
                cleanupTask);

        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
        return wrapper.getTaskFuture();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        WrapperTask<V> wrapper = new WrapperTask<>(cancelToken, task, null);
        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
        return wrapper.getTaskFuture();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");

        WrapperTask<V> wrapper = new WrapperTask<>(cancelToken, task, cleanupTask);
        submitTask(
                wrapper.getNewCancellationToken(),
                wrapper.getNewCancellationController(),
                wrapper,
                wrapper.getCleanupTask());
        return wrapper.getTaskFuture();
    }

    private static class SimpleWaitSignal {
        private final Lock lock;
        private final Condition waitSignal;
        private volatile boolean signaled;

        public SimpleWaitSignal() {
            this.lock = new ReentrantLock();
            this.waitSignal = lock.newCondition();
            this.signaled = false;
        }

        public void signal() {
            signaled = true;
            lock.lock();
            try {
                waitSignal.signalAll();
            } finally {
                lock.unlock();
            }
        }

        public boolean isSignaled() {
            return signaled;
        }

        public void waitSignal(CancellationToken cancelToken) {
            if (signaled) {
                return;
            }

            lock.lock();
            try {
                while (!signaled) {
                    CancelableWaits.await(cancelToken, waitSignal);
                }
            } finally {
                lock.unlock();
            }
        }

        public boolean waitSignal(CancellationToken cancelToken,
                long timeout, TimeUnit timeUnit) {

            if (signaled) {
                return true;
            }

            long timeoutNanos = timeUnit.toNanos(timeout);
            long startTime = System.nanoTime();
            lock.lock();
            try {
                while (!signaled) {
                    long elapsed = System.nanoTime() - startTime;
                    long timeToWait = timeoutNanos - elapsed;
                    if (timeToWait <= 0) {
                        return false;
                    }
                    CancelableWaits.await(cancelToken, timeToWait, TimeUnit.NANOSECONDS, waitSignal);
                }
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    private static class FunctionWrapper implements CancelableFunction<Void> {
        private final CancelableTask task;

        public FunctionWrapper(CancelableTask task) {
            this.task = task;
        }

        @Override
        public Void execute(CancellationToken cancelToken) {
            task.execute(cancelToken);
            return null;
        }
    }

    private static class WrapperTask<V> implements CancelableTask {
        private CancelableFunction<V> function;
        private final ListenerRef cancelRef;
        private final CancellationSource newCancelSource;
        private final CleanupTask userCleanupTask;
        private final AtomicReference<TaskState> currentState;
        private final SimpleWaitSignal waitDoneSignal;
        private V result;
        private boolean taskCanceled;
        private Throwable resultException;

        public WrapperTask(
                CancellationToken cancelToken,
                CancelableFunction<V> function,
                CleanupTask userCleanupTask) {

            this.function = function;
            this.currentState = new AtomicReference<>(TaskState.NOT_STARTED);
            this.userCleanupTask = userCleanupTask;
            this.waitDoneSignal = new SimpleWaitSignal();
            this.taskCanceled = true;
            this.result = null;
            this.newCancelSource = new CancellationSource();
            this.cancelRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    newCancelSource.getController().cancel();
                }
            });
        }

        public CancellationToken getNewCancellationToken() {
            return newCancelSource.getToken();
        }

        public CancellationController getNewCancellationController() {
            return newCancelSource.getController();
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            try {
                taskCanceled = false;
                currentState.set(TaskState.RUNNING);
                result = function.execute(cancelToken);
                function = null; // do not reference it needlessly
            } catch (TaskCanceledException ex) {
                taskCanceled = true;
                resultException = ex;
            } catch (Throwable ex) {
                resultException = ex;
            }
        }

        public TaskFuture<V> getTaskFuture() {
            return new TaskFuture<V>() {
                @Override
                public TaskState getTaskState() {
                    return currentState.get();
                }

                private V fetchResult() {
                    assert getTaskState().isDone();

                    if (resultException != null) {
                        throw new TaskExecutionException(resultException);
                    }
                    else if (taskCanceled) {
                        throw new TaskCanceledException();
                    }
                    return result;
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
                        throw new TaskCanceledException();
                    }
                    return fetchResult();
                }
            };
        }

        public Runnable getCleanupTask() {
            return new Runnable() {
                @Override
                public void run() {
                    boolean canceled = taskCanceled;
                    Throwable error = resultException;

                    if (canceled) {
                        currentState.set(TaskState.DONE_CANCELED);
                    }
                    else if (error != null) {
                        currentState.set(TaskState.DONE_ERROR);
                    }
                    else {
                        currentState.set(TaskState.DONE_COMPLETED);
                    }

                    waitDoneSignal.signal();

                    // We call cancelRef after the signal because it might
                    // have a user defined implementation and we cannot trust,
                    // that it will not throw an exception and the signal must
                    // be set.
                    cancelRef.unregister();
                    if (userCleanupTask != null) {
                        userCleanupTask.cleanup(canceled, error);
                    }
                }
            };
        }
    }
}
