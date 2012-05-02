package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public abstract class AbstractTaskExecutorService
implements
        TaskExecutorService {

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
