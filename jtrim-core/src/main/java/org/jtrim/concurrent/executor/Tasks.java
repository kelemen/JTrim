package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;

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

    @SuppressWarnings("unchecked")
    public static <V> TaskFuture<V> canceledTaskFuture() {
        // This is safe because we never actually return any result
        // and throw an exception instead.
        return (TaskFuture<V>)CanceledTaskFuture.INSTANCE;
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
    }

    private enum CancelableNoOp implements CancelableTask {
        INSTANCE;

        @Override
        public void execute(CancellationToken cancelToken) {
        }
    }

    private enum NoOp implements Runnable {
        INSTANCE;

        @Override
        public void run() { }
    }

    private Tasks() {
        throw new AssertionError();
    }
}
