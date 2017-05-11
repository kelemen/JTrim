package org.jtrim2.access;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.CleanupTask;
import org.jtrim2.executor.ContextAwareWrapper;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;

/**
 * @see AccessTokens#createToken(Object)
 */
final class GenericAccessToken<IDType> extends AbstractAccessToken<IDType> {
    // Note that, as of currently implemented, this implementation will fail
    // if there are more than Integer.MAX_VALUE concurrently executing tasks.

    private final IDType accessID;
    private final CancellationSource mainCancelSource;
    private final AtomicInteger activeCount;
    private final AtomicInteger submittedCount;
    private volatile boolean shuttingDown;
    private final WaitableSignal releaseSignal;
    private final ContextAwareWrapper sharedContext;

    public GenericAccessToken(IDType accessID) {
        Objects.requireNonNull(accessID, "accessID");

        this.accessID = accessID;
        this.mainCancelSource = Cancellation.createCancellationSource();
        this.releaseSignal = new WaitableSignal();
        this.shuttingDown = false;
        this.activeCount = new AtomicInteger(0);
        this.submittedCount = new AtomicInteger(0);
        this.sharedContext = TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
    }

    @Override
    public IDType getAccessID() {
        return accessID;
    }

    @Override
    public String toString() {
        return "AccessToken{" + accessID + '}';
    }

    @Override
    public TaskExecutor createExecutor(TaskExecutor executor) {
        return new TokenExecutor(sharedContext.sameContextExecutor(executor));
    }

    @Override
    public boolean isExecutingInThis() {
        return sharedContext.isExecutingInThis();
    }

    @Override
    public boolean isReleased() {
        return releaseSignal.isSignaled();
    }

    // This method is idempotent
    private void onRelease() {
        releaseSignal.signal();
        notifyReleaseListeners();
    }

    private void checkReleased() {
        if (shuttingDown) {
            // Even if submittedCount gets increased it will immediately notice
            // the shuttingDown flag and decrement.
            if (submittedCount.get() == 0) {
                onRelease();
            }
        }
    }

    @Override
    public void release() {
        shuttingDown = true;
        // Even if submittedCount gets increased it will immediately notice
        // the shuttingDown flag and decrement.
        if (submittedCount.get() == 0) {
            onRelease();
        }
    }

    @Override
    public void releaseAndCancel() {
        release();
        mainCancelSource.getController().cancel();
    }

    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        releaseSignal.waitSignal(cancelToken);
    }

    @Override
    public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return releaseSignal.tryWaitSignal(cancelToken, timeout, unit);
    }

    private class TokenExecutor implements TaskExecutor {
        private final TaskExecutor executor;

        public TokenExecutor(TaskExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(task, "task");

            // submittedCount.incrementAndGet() must preceed the check for
            // shuttingDown
            submittedCount.incrementAndGet();
            if (shuttingDown) {
                submittedCount.decrementAndGet();
                executeCanceledCleanup(executor, cleanupTask);
                return;
            }

            // Checks if the AccessToken will no longer execute tasks
            // and notify the listeners if so.
            CleanupTask wrappedCleanup = (boolean canceled, Throwable error) -> {
                try {
                    submittedCount.decrementAndGet();
                    checkReleased();
                } finally {
                    if (cleanupTask != null) {
                        cleanupTask.cleanup(canceled, error);
                    }
                }
            };

            executor.execute(
                    Cancellation.anyToken(mainCancelSource.getToken(), cancelToken),
                    new SubTask(task),
                    wrappedCleanup);
        }

        private class SubTask implements CancelableTask {
            private final CancelableTask subTask;

            public SubTask(CancelableTask subTask) {
                this.subTask = subTask;
            }

            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                // mainCancelSource.getToken().isCanceled() is only here
                // to protect against buggy executor implementations not
                // forwarding the cancellation token properly.
                boolean canceled = cancelToken.isCanceled() || mainCancelSource.getToken().isCanceled();

                activeCount.incrementAndGet();
                if (canceled) {
                    // It is not possible that we need to execute release
                    // listeners here because as far as the executor can see
                    // a tasks is being executed, so we cannot switch to the
                    // release state. Listeners will be notified in the cleanup
                    // task if necessary.
                    activeCount.decrementAndGet();
                    throw new OperationCanceledException();
                }

                try {
                    subTask.execute(cancelToken);
                } finally {
                    if (activeCount.decrementAndGet() <= 0) {
                        checkReleased();
                    }
                }
            }
        }
    }

    private static void executeCanceledCleanup(TaskExecutor executor, CleanupTask cleanup) {
        if (cleanup != null) {
            executor.execute(
                    Cancellation.UNCANCELABLE_TOKEN,
                    CancelableTasks.noOpCancelableTask(),
                    new CanceledCleanupForwarder(cleanup));
        }
    }

    private static final class CanceledCleanupForwarder implements CleanupTask {
        private final CleanupTask cleanup;

        public CanceledCleanupForwarder(CleanupTask cleanup) {
            assert cleanup != null;
            this.cleanup = cleanup;
        }

        @Override
        public void cleanup(boolean canceled, Throwable error) throws Exception {
            cleanup.cleanup(true, error);
        }
    }
}