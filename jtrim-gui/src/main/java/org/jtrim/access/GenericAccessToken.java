package org.jtrim.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.*;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
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
    private final Lock mainLock;
    private final RefCollection<CancellationController> cancelControllers;
    private final AtomicInteger activeCount;
    private volatile boolean shuttingDown;
    private volatile boolean terminating; // shuttingDown with cancellation
    private final WaitableSignal releaseSignal;

    public GenericAccessToken(IDType accessID) {
        ExceptionHelper.checkNotNullArgument(accessID, "accessID");

        this.accessID = accessID;
        this.mainLock = new ReentrantLock();
        this.cancelControllers = new RefLinkedList<>();
        this.releaseSignal = new WaitableSignal();
        this.shuttingDown = false;
        this.terminating = false;
        this.activeCount = new AtomicInteger(0);
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
        return new TokenExecutor(executor);
    }

    @Override
    public boolean isReleased() {
        return releaseSignal.isSignaled();
    }

    private void onRelease() {
        releaseSignal.signal();
        notifyReleaseListeners();
    }

    private void checkReleased() {
        if (terminating) {
            if (activeCount.get() <= 0) {
                onRelease();
            }
        }
        else if (shuttingDown) {
            boolean terminateNow;
            mainLock.lock();
            try {
                terminateNow = cancelControllers.isEmpty();
            } finally {
                mainLock.unlock();
            }
            if (terminateNow) {
                onRelease();
            }
        }
    }

    @Override
    public void release() {
        boolean terminateNow;
        mainLock.lock();
        try {
            shuttingDown = true;
            terminateNow = cancelControllers.isEmpty();
        } finally {
            mainLock.unlock();
        }

        if (terminateNow) {
            onRelease();
        }
    }

    @Override
    public void releaseAndCancel() {
        release();

        terminating = true;

        Collection<CancellationController> toCancel;
        mainLock.lock();
        try {
            toCancel = new ArrayList<>(cancelControllers);
            cancelControllers.clear();
        } finally {
            mainLock.unlock();
        }

        Throwable toThrow = null;
        for (CancellationController controller: toCancel) {
            try {
                controller.cancel();
            } catch (Throwable ex) {
                if (toThrow == null) {
                    toThrow = ex;
                }
                else {
                    toThrow.addSuppressed(ex);
                }
            }
        }
        if (toThrow != null) {
            ExceptionHelper.rethrow(toThrow);
        }
    }

    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        releaseSignal.waitSignal(cancelToken);
    }

    @Override
    public boolean awaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return releaseSignal.waitSignal(cancelToken, timeout, unit);
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
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            // Just a quick check for the already terminated case.
            // This check is not required for correctness.
            if (shuttingDown || isReleased()) {
                if (cleanupTask != null) {
                    executor.execute(
                            Cancellation.UNCANCELABLE_TOKEN,
                            Tasks.noOpCancelableTask(),
                            cleanupTask);
                }
                return;
            }

            final ChildCancellationSource cancelSource
                    = Cancellation.createChildCancellationSource(cancelToken);
            final RefCollection.ElementRef<?> controllerRef;
            mainLock.lock();
            try {
                controllerRef = cancelControllers.addGetReference(cancelSource.getController());
            } finally {
                mainLock.unlock();
            }

            if (shuttingDown) {
                controllerRef.remove();
                cancelSource.detachFromParent();

                if (cleanupTask != null) {
                    executor.execute(
                            Cancellation.UNCANCELABLE_TOKEN,
                            Tasks.noOpCancelableTask(),
                            cleanupTask);
                }
                return;
            }

            // The reasons for the wrapped cleanup task are:
            //  1. Remove the added reference from "cancelControllers"
            //  2. Dispose the no longer required ChildCancellationSource.
            //  3. Check if the AccessToken will no longer execute tasks
            //     and notify the listeners if so.
            CleanupTask wrappedCleanup = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) throws Exception {
                    try {
                        mainLock.lock();
                        try {
                            controllerRef.remove();
                        } finally {
                            mainLock.unlock();
                        }
                        cancelSource.detachFromParent();
                    } finally {
                        try {
                            checkReleased();
                        } finally {
                            if (cleanupTask != null) {
                                cleanupTask.cleanup(canceled, error);
                            }
                        }
                    }
                }
            };

            executor.execute(
                    cancelSource.getToken(),
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
                activeCount.incrementAndGet();
                if (cancelToken.isCanceled() || terminating) {
                    int nextActiveCount = activeCount.decrementAndGet();

                    OperationCanceledException toThrow
                            = new OperationCanceledException();

                    if (nextActiveCount <= 0) {
                        try {
                            checkReleased();
                        } catch (Throwable ex) {
                            toThrow.addSuppressed(ex);
                        }
                    }
                    throw toThrow;
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
}
