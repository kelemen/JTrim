package org.jtrim2.access;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTasks;
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
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(function, "function");

            if (cancelToken.isCanceled()) {
                return CancelableTasks.canceledComplationStage();
            }

            submittedCount.incrementAndGet();
            if (shuttingDown) {
                submittedCount.decrementAndGet();
                return CancelableTasks.canceledComplationStage();
            }

            CompletableFuture<V> future = new CompletableFuture<>();
            CancellationToken combinedToken = Cancellation.anyToken(mainCancelSource.getToken(), cancelToken);
            executor.executeFunction(combinedToken, new SubTask<>(function)).handle((result, error) -> {
                try {
                    // Checks if the AccessToken will no longer execute tasks
                    // and notify the listeners if so.
                    submittedCount.decrementAndGet();
                    checkReleased();
                    return null;
                } finally {
                    AsyncTasks.complete(result, error, future);
                }
            }).exceptionally(AsyncTasks::expectNoError);
            return future;
        }
    }

    private class SubTask<V> implements CancelableFunction<V> {
        private final CancelableFunction<? extends V> function;

        public SubTask(CancelableFunction<? extends V> function) {
            this.function = function;
        }

        @Override
        public V execute(CancellationToken cancelToken) throws Exception {
            // mainCancelSource.getToken().isCanceled() is only here
            // to protect against buggy executor implementations not
            // forwarding the cancellation token properly.
            if (cancelToken.isCanceled() || mainCancelSource.getToken().isCanceled()) {
                throw new OperationCanceledException();
            }

            activeCount.incrementAndGet();
            try {
                return function.execute(cancelToken);
            } finally {
                if (activeCount.decrementAndGet() <= 0) {
                    checkReleased();
                }
            }
        }
    }
}
