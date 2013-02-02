package org.jtrim.access;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#createToken(Object)
 * @author Kelemen Attila
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

    public GenericAccessToken(IDType accessID) {
        ExceptionHelper.checkNotNullArgument(accessID, "accessID");

        this.accessID = accessID;
        this.mainCancelSource = Cancellation.createCancellationSource();
        this.releaseSignal = new WaitableSignal();
        this.shuttingDown = false;
        this.activeCount = new AtomicInteger(0);
        this.submittedCount = new AtomicInteger(0);
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
    public ContextAwareTaskExecutor createExecutor(TaskExecutor executor) {
        return new TokenExecutor(executor);
    }

    @Override
    public boolean isExecutingInThis() {
        throw new UnsupportedOperationException("Not supported yet.");
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

    private class TokenExecutor implements ContextAwareTaskExecutor {
        private final ContextAwareTaskExecutor executor;

        public TokenExecutor(TaskExecutor executor) {
            this.executor = new ContextAwareWrapper(executor);
        }

        @Override
        public boolean isExecutingInThis() {
            return executor.isExecutingInThis();
        }

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

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
            CleanupTask wrappedCleanup = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) throws Exception {
                    try {
                        submittedCount.decrementAndGet();
                        checkReleased();
                    } finally {
                        if (cleanupTask != null) {
                            cleanupTask.cleanup(canceled, error);
                        }
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

    private static class ContextAwareWrapper implements ContextAwareTaskExecutor {
        private final TaskExecutor executor;
        // Never set this variable to false, because we only check the presence
        // of the variable.
        private final ThreadLocal<Boolean> inContext;

        public ContextAwareWrapper(TaskExecutor executor) {
            ExceptionHelper.checkNotNullArgument(executor, "executor");
            this.executor = executor;
            this.inContext = new ThreadLocal<>();
        }

        @Override
        public boolean isExecutingInThis() {
            Boolean result = inContext.get();
            if (result == null) {
                inContext.remove();
                return false;
            }
            return result;
        }

        @Override
        public void execute(
                CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            CancelableTask contextTask = new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    Boolean prevValue = inContext.get();
                    try {
                        if (prevValue == null) {
                            inContext.set(true);
                        }
                        task.execute(cancelToken);
                    } finally {
                        if (prevValue == null) {
                            inContext.remove();
                        }
                    }
                }
            };

            if (cleanupTask != null) {
                CleanupTask contextCleanup = new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) throws Exception {
                        Boolean prevValue = inContext.get();
                        try {
                            if (prevValue == null) {
                                inContext.set(true);
                            }
                            cleanupTask.cleanup(canceled, error);
                        } finally {
                            if (prevValue == null) {
                                inContext.remove();
                            }
                        }
                    }
                };
                executor.execute(cancelToken, contextTask, contextCleanup);
            }
            else {
                executor.execute(cancelToken, contextTask, null);
            }
        }
    }

    private static void executeCanceledCleanup(TaskExecutor executor, CleanupTask cleanup) {
        if (cleanup != null) {
            executor.execute(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Tasks.noOpCancelableTask(),
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
