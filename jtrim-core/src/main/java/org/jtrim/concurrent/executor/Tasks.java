package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class Tasks {
    public static Runnable noOpTask() {
        return NoOp.INSTANCE;
    }

    public static CancelableTask noOpCancelableTask() {
        return CancelableNoOp.INSTANCE;
    }

    public static Runnable runOnceTask(
            Runnable task, boolean failOnReRun) {

        return new RunOnceTask(task, failOnReRun);
    }

    public static CancelableTask runOnceCancelableTask(
            CancelableTask task, boolean failOnReRun) {

        return new RunOnceCancelableTask(task, failOnReRun);
    }

    @SuppressWarnings("unchecked")
    public static <V> TaskFuture<V> canceledTaskFuture() {
        // This is safe because we never actually return any result
        // and throw an exception instead.
        return (TaskFuture<V>)CanceledTaskFuture.INSTANCE;
    }

    private static class RunOnceTask implements Runnable {
        private final boolean failOnReRun;
        private final AtomicReference<Runnable> taskRef;

        public RunOnceTask(Runnable task, boolean failOnReRun) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.taskRef = new AtomicReference<>(task);
            this.failOnReRun = failOnReRun;
        }

        @Override
        public void run() {
            Runnable task = taskRef.getAndSet(null);
            if (task == null) {
                if (failOnReRun) {
                    throw new IllegalStateException("This task is not allowed"
                            + " to be called multiple times.");
                }
            }
            task.run();
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            Runnable currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            }
            else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private static class RunOnceCancelableTask implements CancelableTask {
        private final boolean failOnReRun;
        private final AtomicReference<CancelableTask> taskRef;

        public RunOnceCancelableTask(CancelableTask task, boolean failOnReRun) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.taskRef = new AtomicReference<>(task);
            this.failOnReRun = failOnReRun;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            CancelableTask task = taskRef.getAndSet(null);
            if (task == null) {
                if (failOnReRun) {
                    throw new IllegalStateException("This task is not allowed"
                            + " to be called multiple times.");
                }
            }
            task.execute(cancelToken);
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            CancelableTask currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            }
            else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private enum CanceledTaskFuture implements TaskFuture<Object> {
        INSTANCE;

        @Override
        public TaskState getTaskState() {
            return TaskState.DONE_CANCELED;
        }

        @Override
        public Object tryGetResult() {
            throw new OperationCanceledException();
        }

        @Override
        public Object waitAndGet(CancellationToken cancelToken) {
            return tryGetResult();
        }

        @Override
        public Object waitAndGet(CancellationToken cancelToken,
                long timeout, TimeUnit timeUnit) {
            return tryGetResult();
        }

        @Override
        public String toString() {
            return "CANCELED";
        }
    }

    private enum CancelableNoOp implements CancelableTask {
        INSTANCE;

        @Override
        public void execute(CancellationToken cancelToken) {
        }

        @Override
        public String toString() {
            return "NO-OP";
        }
    }

    private enum NoOp implements Runnable {
        INSTANCE;

        @Override
        public void run() { }

        @Override
        public String toString() {
            return "NO-OP";
        }
    }

    private Tasks() {
        throw new AssertionError();
    }
}
