package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an executor which executes tasks synchronously on the calling thread
 * which submits them (by the {@code execute} or one of the {@code submit}
 * methods). Therefore whenever such {@code execute} or {@code submit} method
 * returns, the submitted task is already executed or never will be (due to the
 * executor being shut down).
 * <P>
 * There are two special instances which can be accessed through static
 * methods:
 * <ul>
 *  <li>
 *   {@link #getSimpleExecutor() getSimpleExecutor()} for a simple and efficient
 *   {@code TaskExecutor} instance which executes tasks synchronously on the
 *   calling thread.
 *  </li>
 *  <li>
 *   {@link #getDefaultInstance() getDefaultInstance()} to use as sensible
 *   default values when an {@code TaskExecutorService} instance is needed.
 *  </li>
 * </ul>
 * Note that unlike general {@code TaskExecutorService} instances, instances of
 * this class does not need to be shut down (but it is possible to do so).
 *
 * <h2>Thread safety</h2>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not <I>synchronization transparent</I>.
 */
public final class SyncTaskExecutor
extends
        DelegatedTaskExecutorService
implements
        MonitorableTaskExecutorService {

    private static final TaskExecutorService DEFAULT_INSTANCE
            = TaskExecutors.asUnstoppableExecutor(new SyncTaskExecutor());

    /**
     * Returns a plain and efficient {@code TaskExecutor} which executes tasks
     * synchronously on the calling thread. This method always returns the same
     * executor instance and is considerably more efficient than a full-fledged
     * {@link SyncTaskExecutor} implementation (or the one returned by
     * {@link #getDefaultInstance() getDefaultInstance()}.
     *
     * @return a plain and efficient {@code TaskExecutor} which executes tasks
     *   synchronously on the calling thread. This method never returns
     *   {@code null}.
     */
    public static TaskExecutor getSimpleExecutor() {
        return SimpleTaskExecutor.INSTANCE;
    }

    /**
     * Returns an {@code TaskExecutorService} which executes tasks synchronously
     * on the calling thread and cannot be shutted down. Attempting to shutdown
     * the returned {@code TaskExecutorService} will result in an unchecked
     * {@link UnsupportedOperationException} to be thrown.
     * <P>
     * This method always returns the same {@code TaskExecutorService} instance
     * and note that sharing the return value across multiple threads can cause
     * some synchronization overhead. So it is more efficient to create a new
     * instance when this is an issue. Therefore this
     * {@code TaskExecutorService} instance is only intended to be used as a
     * sensible default value.
     *
     * @return an {@code TaskExecutorService} which executes tasks synchronously
     *   on the calling thread and cannot be shutted down. This method never
     *   returns {@code null} and always returns the same instance.
     */
    public static TaskExecutorService getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private final MonitorableSyncTaskExecutor wrappedMonitorable;

    /**
     * Creates a new executor which executes tasks synchronously on the calling
     * thread.
     */
    public SyncTaskExecutor() {
        this(new MonitorableSyncTaskExecutor());
    }

    private SyncTaskExecutor(MonitorableSyncTaskExecutor wrappedMonitorable) {
        super(new UpgradedTaskExecutor(wrappedMonitorable));
        this.wrappedMonitorable = wrappedMonitorable;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method always returns zero, since
     * this executor never queues tasks.
     */
    @Override
    public long getNumberOfQueuedTasks() {
        return wrappedMonitorable.getNumberOfQueuedTasks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public long getNumberOfExecutingTasks() {
        return wrappedMonitorable.getNumberOfExecutingTasks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isExecutingInThis() {
        return wrappedMonitorable.isExecutingInThis();
    }

    private static <V> CompletionStage<V> completeNow(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {

        CompletableFuture<V> future = new CompletableFuture<>();
        CancelableTasks.complete(cancelToken, function, future);
        return future;
    }

    private enum SimpleTaskExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            Objects.requireNonNull(command, "command");
            CancelableTasks.executeAndLogError(command);
        }

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(function, "function");

            return completeNow(cancelToken, function);
        }
    }

    private static final class MonitorableSyncTaskExecutor implements MonitorableTaskExecutor {
        private final ThreadLocal<Object> mark;
        private final AtomicInteger numberOfTasks;

        public MonitorableSyncTaskExecutor() {
            this.mark = new ThreadLocal<>();
            this.numberOfTasks = new AtomicInteger(0);
        }

        @Override
        public long getNumberOfQueuedTasks() {
            return 0;
        }

        @Override
        public long getNumberOfExecutingTasks() {
            return numberOfTasks.get();
        }

        @Override
        public boolean isExecutingInThis() {
            if (mark.get() == null) {
                mark.remove();
                return false;
            } else {
                return true;
            }
        }

        private void startExecuting() {
            mark.set(Boolean.TRUE);
        }

        private void endExecuting() {
            mark.remove();
        }

        private <V> V executeNow(Supplier<V> task) {
            numberOfTasks.incrementAndGet();
            try {
                startExecuting();
                return task.get();
            } finally {
                numberOfTasks.decrementAndGet();
                endExecuting();
            }
        }

        @Override
        public void execute(Runnable command) {
            Objects.requireNonNull(command, "command");
            executeNow(() -> {
                CancelableTasks.executeAndLogError(command);
                return null;
            });
        }

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(function, "function");

            return executeNow(() -> completeNow(cancelToken, function));
        }
    }
}
