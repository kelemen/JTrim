package org.jtrim2.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.RefCollection;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.OneShotListenerManager;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

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
 * <h2>Thread safety</h2>
 * Methods of this class are completely thread-safe without any further
 * synchronization.
 *
 * <h3>Synchronization transparency</h3>
 * Unless documented otherwise the methods of this class are not
 * <I>synchronization transparent</I> but will not wait for asynchronous
 * tasks to complete.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @see #newToken(AccessToken, Collection) newToken
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
        Objects.requireNonNull(token, "token");
        ExceptionHelper.checkNotNullElements(blockingTokens, "blockingTokens");

        ScheduledAccessToken<IDType> result = new ScheduledAccessToken<>(token);
        result.startWaitForBlockingTokens(blockingTokens);
        return result;
    }

    // Must be called right after creating ScheduledAccessToken and must be
    // called exactly once.
    private void startWaitForBlockingTokens(
            Collection<? extends AccessToken<IDType>> tokens) {

        wrappedToken.addReleaseListener(subToken::release);

        if (tokens.isEmpty()) {
            enableSubmitTasks();
            return;
        }

        for (AccessToken<IDType> blockingToken: tokens) {
            final RefCollection.ElementRef<?> tokenRef;
            tokenRef = blockingTokens.addGetReference(blockingToken);
            blockingToken.addReleaseListener(() -> {
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
            });
        }
    }

    private void enableSubmitTasks() {
        EventListeners.dispatchRunnable(allowSubmitManager);
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
        private final Deque<QueuedTask<?>> scheduledTasks;
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
            allowSubmitManager.registerOrNotifyListener(this::startSubmitting);
        }

        private void submitAll(List<QueuedTask<?>> toSubmit) {
            Throwable toThrow = null;
            for (QueuedTask<?> queuedTask: toSubmit) {
                try {
                    queuedTask.execute(executor);
                } catch (Throwable ex) {
                    if (toThrow == null) toThrow = ex;
                    else toThrow.addSuppressed(ex);
                }
            }
            ExceptionHelper.rethrowIfNotNull(toThrow);
        }

        private void startSubmitting() {
            List<QueuedTask<?>> toSubmit;

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

            ExceptionHelper.rethrowIfNotNull(toThrow);
        }

        private void addToQueue(QueuedTask<?> queuedTask) {
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
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {
            if (allowSubmit) {
                return executor.executeFunction(cancelToken, function);
            }

            CompletableFuture<V> future = new CompletableFuture<>();

            ListenerRef cancelRef = cancelToken.addCancellationListener(() -> {
                future.completeExceptionally(OperationCanceledException.withoutStackTrace());
            });
            future.whenComplete((result, error) -> {
                cancelRef.unregister();
            });

            addToQueue(new QueuedTask<>(cancelToken, function, future));

            return future;
        }
    }

    private static class QueuedTask<V> {
        private final CancellationToken cancelToken;
        private final CancelableFunction<? extends V> function;
        private final CompletableFuture<? super V> future;

        public QueuedTask(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function,
                CompletableFuture<? super V> future) {
            this.cancelToken = cancelToken;
            this.function = function;
            this.future = future;
        }

        public void execute(TaskExecutor executor) {
            executor.executeFunction(cancelToken, function)
                    .whenComplete(AsyncTasks.completeForwarder(future));
        }
    }
}
