package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
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

    private final AccessToken<IDType> subToken;
    private final Lock mainLock;
    private final RefCollection<AccessToken<IDType>> blockingTokens;
    private final OneShotListenerManager<Runnable, Void> allowSubmitManager;

    private ScheduledAccessToken(final AccessToken<IDType> token) {
        super(AccessTokens.createToken(token.getAccessID()));

        this.subToken = token;
        this.mainLock = new ReentrantLock();
        this.blockingTokens = new RefLinkedList<>();
        this.allowSubmitManager = new OneShotListenerManager<>();
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

        wrappedToken.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                subToken.release();
            }
        });

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

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: The tasks submitted to the returned executor
     * will  also run in the context of the token specified when creating this
     * {@code ScheduledAccessToken}.
     */
    @Override
    public TaskExecutor createExecutor(TaskExecutor executor) {
        TaskExecutor subTokenExecutor = subToken.createExecutor(executor);
        TaskExecutor wrappedExecutor = wrappedToken.createExecutor(subTokenExecutor);
        ScheduledExecutor scheduledExecutor = new ScheduledExecutor(wrappedExecutor);
        scheduledExecutor.start();
        return scheduledExecutor;
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

    private class ScheduledExecutor implements TaskExecutor {
        private final TaskExecutor executor;
        private final Lock taskLock;
        private final Deque<QueuedTask> scheduledTasks;
        // allowSubmit means that we will not check the queue anymore, this
        // variable is set only while holding the taskLock. However if it is
        // true, we can blindly forward tasks.
        private volatile boolean allowSubmit;

        public ScheduledExecutor(TaskExecutor executor) {
            this.executor = executor;
            this.taskLock = new ReentrantLock();
            this.scheduledTasks = new LinkedList<>();
            this.allowSubmit = false;
        }

        // Must be called exactly once and right after creation
        public void start() {
            allowSubmitManager.registerOrNotifyListener(new Runnable() {
                @Override
                public void run() {
                    startSubmitting();
                }
            });
        }

        private void submitAll(List<QueuedTask> toSubmit) {
            Throwable toThrow = null;
            while (!toSubmit.isEmpty()) {
                QueuedTask queuedTask = toSubmit.remove(0);

                try {
                    queuedTask.execute(executor);
                } catch (Throwable ex) {
                    if (toThrow == null) toThrow = ex;
                    else toThrow.addSuppressed(ex);
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
                taskLock.lock();
                try {
                    if (scheduledTasks.isEmpty()) {
                        allowSubmit = true;
                        break;
                    }

                    toSubmit = new ArrayList<>(scheduledTasks);
                    scheduledTasks.clear();
                } finally {
                    taskLock.unlock();
                }

                try {
                    submitAll(toSubmit);
                } catch (Throwable ex) {
                    if (toThrow == null) toThrow = ex;
                    else toThrow.addSuppressed(ex);
                }
            }

            if (toThrow != null) {
                ExceptionHelper.rethrow(toThrow);
            }
        }

        private void addToQueue(QueuedTask queuedTask) {
            boolean submitNow;
            taskLock.lock();
            try {
                submitNow = allowSubmit;
                if (!submitNow) {
                    scheduledTasks.add(queuedTask);
                }
            } finally {
                taskLock.unlock();
            }

            if (submitNow) {
                queuedTask.execute(executor);
            }
        }

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                final CleanupTask cleanupTask) {

            // This check is not required for correctness.
            if (allowSubmit) {
                executor.execute(cancelToken, task, cleanupTask);
                return;
            }

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
        }
    }

    private static class QueuedTask {
        private final CancellationToken cancelToken;
        private final AtomicReference<CancelableTask> taskRef;
        private final CleanupTask cleanupTask;

        public QueuedTask(
                CancellationToken cancelToken,
                AtomicReference<CancelableTask> taskRef,
                CleanupTask cleanupTask) {

            this.cancelToken = cancelToken;
            this.taskRef = taskRef;
            this.cleanupTask = cleanupTask;
        }

        public void execute(TaskExecutor executor) {
            CancelableTask task = taskRef.get();
            if (task == null) {
                task = Tasks.noOpCancelableTask();
            }
            executor.execute(cancelToken, task, cleanupTask);
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
