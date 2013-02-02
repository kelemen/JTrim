package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.ChildCancellationSource;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.ContextAwareTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.OneShotListenerManager;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link AccessToken} that will execute submitted tasks only after
 * a specified set of {@code AccessToken}s terminate. To create new instances of
 * this class, call the {@link #newToken(AccessToken, Collection) newToken}
 * factory method.
 * <P>
 * This class was designed to use when implementing the
 * {@link AccessManager#getScheduledAccess(AccessRequest)} method. To implement
 * that method, you must collect all the {@code AccessToken}s that conflicts
 * the requested tokens and pass it to this scheduled token. Note that you must
 * also ensure that no other access tokens will be created which conflict with
 * the newly created token.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are completely thread-safe without any further
 * synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * Unless documented otherwise the methods of this class are not
 * <I>synchronization transparent</I> but will not wait for asynchronous
 * tasks to complete.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @see #newToken(AccessToken, Collection) newToken
 *
 * @author Kelemen Attila
 */
public final class ScheduledAccessToken<IDType>
extends
        DelegatedAccessToken<IDType> {
    // Lock order: ScheduledExecutor.taskLock, mainLock

    private final Lock mainLock;
    private volatile boolean shuttingDown;
    private long queuedExecutorCount;
    private final RefCollection<CancellationController> cancelControllers;
    private final RefCollection<AccessToken<IDType>> blockingTokens;
    private final OneShotListenerManager<Runnable, Void> allowSubmitManager;

    private ScheduledAccessToken(final AccessToken<IDType> token) {
        super(AccessTokens.createToken(token.getAccessID()));

        wrappedToken.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                token.release();
            }
        });

        this.mainLock = new ReentrantLock();
        this.blockingTokens = new RefLinkedList<>();
        this.cancelControllers = new RefLinkedList<>();
        this.allowSubmitManager = new OneShotListenerManager<>();
        this.shuttingDown = false;
    }

    /**
     * Creates a new scheduled token with the specified conflicting tokens and
     * base token. Tasks will only be submitted to the base token if
     * all the conflicting tokens were terminated.
     * <P>
     * The specified conflicting tokens will not be shared with clients of
     * this class, so they cannot be abused.
     *
     * @param <IDType> the type of the access ID (see {@link #getAccessID()})
     * @param token the token to which tasks submitted to this scheduled token
     *   will be submitted to. This token will be shutted down if the created
     *   scheduled token was shutted down. This argument cannot be {@code null}.
     * @param blockingTokens the conflicting tokens. Tasks will not be submitted
     *   to the base access token until any of these tokens are active
     *   (i.e.: not terminated). This argument or its elements cannot be
     *   {@code null} but can be an empty set of tokens.
     * @return a new scheduled token with the specified conflicting tokens and
     *   base token. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments or one of the
     *   conflicting tokens are {@code null}
     */
    public static <IDType> ScheduledAccessToken<IDType> newToken(
            AccessToken<IDType> token,
            Collection<? extends AccessToken<IDType>> blockingTokens) {
        ExceptionHelper.checkNotNullArgument(token, "token");
        ExceptionHelper.checkNotNullElements(blockingTokens, "blockingTokens");

        ScheduledAccessToken<IDType> result = new ScheduledAccessToken<>(token);
        result.startWaitForBlockingTokens(blockingTokens);
        return result;
    }

    // Must be called right after creating ScheduledAccessToken and must be
    // called exactly once.
    private void startWaitForBlockingTokens(
            Collection<? extends AccessToken<IDType>> tokens) {

        if (tokens.isEmpty()) {
            enableSubmitTasks();
            return;
        }

        for (AccessToken<IDType> blockingToken: tokens) {
            final RefCollection.ElementRef<?> tokenRef;
            tokenRef = blockingTokens.addGetReference(blockingToken);
            blockingToken.addReleaseListener(new Runnable() {
                @Override
                public void run() {
                    boolean allReleased;
                    mainLock.lock();
                    try {
                        tokenRef.remove();
                        allReleased = blockingTokens.isEmpty();
                    } finally {
                        mainLock.unlock();
                    }

                    if (allReleased) {
                        enableSubmitTasks();
                    }
                }
            });
        }
    }

    private void enableSubmitTasks() {
        allowSubmitManager.onEvent(RunnableDispatcher.INSTANCE, null);
    }

    @Override
    public ContextAwareTaskExecutor createExecutor(TaskExecutor executor) {
        return new ScheduledExecutor(wrappedToken.createExecutor(executor));
    }

    @Override
    public void release() {
        boolean mayReleaseNow;
        mainLock.lock();
        try {
            shuttingDown = true;
            mayReleaseNow = queuedExecutorCount == 0;
        } finally {
            mainLock.unlock();
        }

        // Otherwise, the last executor finishing to empty its queue
        // will call release().
        if (mayReleaseNow) {
            wrappedToken.release();
        }
    }

    @Override
    public void releaseAndCancel() {
        release();
        wrappedToken.releaseAndCancel();

        List<CancellationController> toCancel;
        mainLock.lock();
        try {
            toCancel = new ArrayList<>(cancelControllers);
            cancelControllers.clear();
        } finally {
            mainLock.unlock();
        }

        for (CancellationController controller: toCancel) {
            controller.cancel();
        }
    }

    /**
     * Returns the string representation of this access token in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "ScheduledAccessToken{" + wrappedToken + '}';
    }

    private class ScheduledExecutor implements ContextAwareTaskExecutor {
        private final ContextAwareTaskExecutor executor;
        private final Lock taskLock;
        private final Deque<QueuedTask> scheduledTasks;
        private final AtomicBoolean listeningForTokens;
        private volatile boolean allowSubmit;

        public ScheduledExecutor(ContextAwareTaskExecutor executor) {
            this.executor = executor;
            this.taskLock = new ReentrantLock();
            this.scheduledTasks = new LinkedList<>();
            this.allowSubmit = false;
            this.listeningForTokens = new AtomicBoolean(false);
        }

        private void submitAll(List<QueuedTask> toSubmit) {
            Throwable toThrow = null;
            while (!toSubmit.isEmpty()) {
                QueuedTask queuedTask = toSubmit.remove(0);

                try {
                    executor.execute(
                            queuedTask.cancelToken,
                            queuedTask.getTask(),
                            queuedTask.cleanupTask);
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

        private void startSubmitting() {
            List<QueuedTask> toSubmit;

            Throwable toThrow = null;
            while (true) {
                boolean releaseNow = false;
                taskLock.lock();
                try {
                    if (scheduledTasks.isEmpty()) {
                        allowSubmit = true;
                        break;
                    }

                    toSubmit = new ArrayList<>(scheduledTasks);
                    scheduledTasks.clear();
                    queuedExecutorCount--;
                    releaseNow = shuttingDown && queuedExecutorCount == 0;
                } finally {
                    taskLock.unlock();
                }

                try {
                    if (releaseNow) {
                        wrappedToken.release();
                    }

                    submitAll(toSubmit);
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

        private void startListeningForTokens() {
            allowSubmitManager.registerOrNotifyListener(new Runnable() {
                @Override
                public void run() {
                    startSubmitting();
                }
            });
        }

        private void addToQueue(QueuedTask queuedTask) {
            boolean submitNow;
            taskLock.lock();
            try {
                submitNow = allowSubmit;
                if (!submitNow) {
                    if (scheduledTasks.isEmpty()) {
                        mainLock.lock();
                        try {
                            if (shuttingDown) {
                                queuedTask.taskRef.set(null);
                            }
                            queuedExecutorCount++;
                        } finally {
                            mainLock.unlock();
                        }
                    }
                    scheduledTasks.add(queuedTask);
                }
            } finally {
                taskLock.unlock();
            }

            if (submitNow) {
                executor.execute(
                        queuedTask.cancelToken,
                        queuedTask.getTask(),
                        queuedTask.cleanupTask);
            }
        }

        private void cancelableExecute(
                CancellationToken cancelToken,
                CancelableTask task,
                final CleanupTask cleanupTask) {

            // This check is not required for correctness.
            if (allowSubmit) {
                executor.execute(cancelToken, task, cleanupTask);
                return;
            }

            Throwable toThrow = null;
            if (!listeningForTokens.getAndSet(true)) {
                try {
                    startListeningForTokens();
                } catch (Throwable ex) {
                    toThrow = ex;
                }
                // This check is not required for correctness.
                if (allowSubmit) {
                    if (toThrow == null) {
                        executor.execute(cancelToken, task, cleanupTask);
                    }
                    else {
                        try {
                            executor.execute(cancelToken, task, cleanupTask);
                        } catch (Throwable ex) {
                            ex.addSuppressed(toThrow);
                            throw ex;
                        }
                    }
                    return;
                }
            }

            try {
                final AtomicReference<CancelableTask> taskRef
                        = new AtomicReference<>(task);

                final ListenerRef cancelRef = cancelToken.addCancellationListener(new Runnable() {
                    @Override
                    public void run() {
                        taskRef.set(null);
                    }
                });
                CleanupTask wrappedCleanup = new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) throws Exception {
                        try {
                            cancelRef.unregister();
                        } finally {
                            if (cleanupTask != null) {
                                cleanupTask.cleanup(canceled, error);
                            }
                        }
                    }
                };

                addToQueue(new QueuedTask(cancelToken, taskRef, wrappedCleanup));
            } catch (Throwable ex) {
                if (toThrow != null) {
                    ex.addSuppressed(toThrow);
                    toThrow = ex;
                }
                else {
                    toThrow = ex;
                }
            }

            if (toThrow != null) {
                ExceptionHelper.rethrow(toThrow);
            }
        }

        @Override
        public boolean isExecutingInThis() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            if (cleanupTask == null && isReleased()) {
                return;
            }

            final ChildCancellationSource cancelSource
                    = Cancellation.createChildCancellationSource(cancelToken);
            final RefCollection.ElementRef<?> controllerRef;

            mainLock.lock();
            try {
                controllerRef = !shuttingDown
                        ? cancelControllers.addGetReference(cancelSource.getController())
                        : null;
            } finally {
                mainLock.unlock();
            }

            // This check is important for releaseAndCancel() not to forget
            // to cancel a CancellationController.
            if (controllerRef == null) {
                try {
                    cancelSource.detachFromParent();
                } finally {
                    if (cleanupTask != null) {
                        cancelableExecute(
                                Cancellation.UNCANCELABLE_TOKEN,
                                Tasks.noOpCancelableTask(),
                                cleanupTask);
                    }
                }
                return;
            }

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
                        if (cleanupTask != null) {
                            cleanupTask.cleanup(canceled, error);
                        }
                    }
                }
            };

            cancelableExecute(cancelSource.getToken(), task, wrappedCleanup);
        }
    }

    private static class QueuedTask {
        public final CancellationToken cancelToken;
        public final AtomicReference<CancelableTask> taskRef;
        public final CleanupTask cleanupTask;

        public QueuedTask(
                CancellationToken cancelToken,
                AtomicReference<CancelableTask> taskRef,
                CleanupTask cleanupTask) {

            this.cancelToken = cancelToken;
            this.taskRef = taskRef;
            this.cleanupTask = cleanupTask;
        }

        public CancelableTask getTask() {
            CancelableTask task = taskRef.get();
            return task != null ? task : Tasks.noOpCancelableTask();
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
