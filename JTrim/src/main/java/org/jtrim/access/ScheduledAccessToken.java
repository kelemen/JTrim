package org.jtrim.access;

import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jtrim.collections.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.*;

/**
 * Defines an {@link AccessToken} that will execute submitted tasks only after
 * a specified set of {@code AccessToken}s terminate.
 * <P>
 * This class was designed to use when implementing the
 * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) AccessManager.getScheduledAccess(AccessRequest)}
 * method. To implement that method, you must collect all the
 * {@code AccessToken}s that conflicts the requested tokens and pass it
 * to this scheduled token. Note that you must also ensure that no other access
 * tokens will be created which conflict with the newly created token.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are completely thread-safe without any further
 * synchronization.
 * <h4>Synchronization transparency</h4>
 * Unless documented otherwise the methods of this class are not
 * <I>synchronization transparent</I> but will not wait for asynchronous
 * tasks to complete.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @author Kelemen Attila
 */
public final class ScheduledAccessToken<IDType>
extends
        DelegatedAccessToken<IDType> {

    private static <IDType> ScheduledExecutor<IDType> getAndStartExecutor(
            AccessToken<IDType> token,
            Collection<? extends AccessToken<IDType>> blockingTokens) {

        ExceptionHelper.checkNotNullArgument(blockingTokens, "blockingTokens");
        for (AccessToken<?> ctoken: blockingTokens) {
            ExceptionHelper.checkNotNullArgument(ctoken, "conflicting token");
        }

        ScheduledExecutor<IDType> result = new ScheduledExecutor<>(token);
        result.start(blockingTokens);

        return result;
    }

    private final AccessToken<IDType> tokenToUse;
    private final ScheduledExecutor<IDType> internalExecutor;

    /**
     * Creates a new scheduled token with the specified conflicting tokens and
     * base token. Tasks will only be submitted to the base token if
     * all the conflicting tokens were terminated.
     * <P>
     * The specified conflicting tokens will not be shared with clients of
     * this class, so they cannot be abused.
     *
     * @param token the token to which tasks submitted to this scheduled token
     *   will be submitted to. This token will be shutted down if the created
     *   scheduled token was shutted down. This argument cannot be {@code null}.
     * @param blockingTokens the conflicting tokens. Tasks will not be submitted
     *   to the base access token until any of these tokens are active
     *   (i.e.: not terminated). This argument or its elements cannot be
     *   {@code null} but can be an empty set of tokens.
     *
     * @throws NullPointerException thrown if any of the arguments or one of the
     *   conflicting tokens are {@code null}
     */
    public ScheduledAccessToken(
            final AccessToken<IDType> token,
            Collection<? extends AccessToken<IDType>> blockingTokens) {

        this(token, getAndStartExecutor(token, blockingTokens));
    }

    private ScheduledAccessToken(
            final AccessToken<IDType> token,
            ScheduledExecutor<IDType> internalExecutor) {
        super(new GenericAccessToken<>(
                token.getAccessID(),
                internalExecutor));

        this.tokenToUse = token;
        this.internalExecutor = internalExecutor;
        wrappedToken.addAccessListener(new AccessListener() {
            @Override
            public void onLostAccess() {
                token.shutdown();
            }
        });
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addAccessListener(AccessListener listener) {
        tokenToUse.addAccessListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAccessListener(AccessListener listener) {
        tokenToUse.removeAccessListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitTermination() throws InterruptedException {
        tokenToUse.awaitTermination();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return tokenToUse.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return tokenToUse.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void release() {
        wrappedToken.release();

        // awaitTermination() would be enough but inconvenient to call because
        // it can throw an InterruptedException
        tokenToUse.release();
    }

    private void waitForTokensUninterruptibly() {
        if (internalExecutor.isAccessGranted()) {
            return;
        }

        boolean wasInterrupted = false;
        try {
            while (true) {
                try {
                    internalExecutor.waitForTokens();
                    return;
                } catch (InterruptedException ex) {
                    wasInterrupted = true;
                }
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean executeNow(Runnable task) {
        waitForTokensUninterruptibly();
        return wrappedToken.executeNow(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T executeNow(Callable<T> task) {
        waitForTokensUninterruptibly();
        return wrappedToken.executeNow(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean executeNowAndShutdown(Runnable task) {
        waitForTokensUninterruptibly();
        return wrappedToken.executeNowAndShutdown(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T executeNowAndShutdown(Callable<T> task) {
        waitForTokensUninterruptibly();
        return wrappedToken.executeNowAndShutdown(task);
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

    private static class ScheduledExecutor<IDType> implements Executor {
        private static final AccessToken<?>[] EMPTY_TOKEN_ARRAY
                = new AccessToken<?>[0];
        private final AccessToken<IDType> token;

        private final Lock mainLock;
        private RefList<AccessToken<IDType>> blockingTokens;

        private final ReadWriteLock executeLock;
        private List<Runnable> scheduledTasks;

        // Only the "true" value is reliable
        private volatile boolean accessGranted;

        private ScheduledExecutor(AccessToken<IDType> token) {
            this.token = token;
            this.mainLock = new ReentrantLock();
            this.executeLock = new ReentrantReadWriteLock();
            this.blockingTokens = null;
            this.scheduledTasks = new LinkedList<>();
            this.accessGranted = false;
        }

        public boolean isAccessGranted() {
            return accessGranted;
        }

        public void waitForTokens() throws InterruptedException {
            AccessToken<?>[] tokensToWait;

            mainLock.lock();
            try {
                tokensToWait = scheduledTasks.toArray(EMPTY_TOKEN_ARRAY);
            } finally {
                mainLock.unlock();
            }

            ExecutorsEx.awaitExecutors(tokensToWait);
            accessGranted = true;
        }

        private void enableExecutor() {
            accessGranted = true;
            Lock wLock = executeLock.writeLock();

            List<Runnable> toExecute = new ArrayList<>();

            do {
                toExecute.clear();

                wLock.lock();
                try {
                    if (scheduledTasks != null) {
                        toExecute.addAll(scheduledTasks);
                        scheduledTasks.clear();
                    }
                } finally {
                    wLock.unlock();
                }

                if (toExecute != null) {
                    for (Runnable task: toExecute) {
                        token.execute(task);
                    }
                }

                wLock.lock();
                try {
                    if (scheduledTasks == null || scheduledTasks.isEmpty()) {
                        scheduledTasks = null;
                        toExecute = null;
                    }
                } finally {
                    wLock.unlock();
                }
            } while (toExecute != null);
        }

        private void start(Collection<? extends AccessToken<IDType>> tokens) {
            RefList<AccessToken<IDType>> result = new RefLinkedList<>(tokens);

            List<RefList.ElementRef<AccessToken<IDType>>> tokenRefs;
            tokenRefs = new ArrayList<>(result.size());
            for (RefList.ElementRef<AccessToken<IDType>> tokenRef:
                    new ElementRefIterable<>(result)) {
                tokenRefs.add(tokenRef);
            }

            boolean done;
            Collection<RefList.ElementRef<AccessToken<IDType>>> toRemove;
            toRemove = new LinkedList<>();

            mainLock.lock();
            try {
                blockingTokens = result;
                done = result.isEmpty();
            } finally {
                mainLock.unlock();
            }

            for (RefList.ElementRef<AccessToken<IDType>> tokenRef: tokenRefs) {
                AccessToken<?> blockingToken = tokenRef.getElement();
                blockingToken.addAccessListener(
                        new BlockingTokenListener(tokenRef));
                if (blockingToken.isTerminated()) {
                    toRemove.add(tokenRef);
                }
            }

            if (!toRemove.isEmpty()) {
                mainLock.lock();
                try {
                    if (!result.isEmpty()) {
                        for (RefList.ElementRef<AccessToken<IDType>> tokenRef: toRemove) {
                            tokenRef.remove();
                        }
                        done = result.isEmpty();
                    }
                } finally {
                    mainLock.unlock();
                }
            }

            if (done) {
                enableExecutor();
            }
        }

        @Override
        public void execute(Runnable command) {
            Lock rLock = executeLock.readLock();
            rLock.lock();
            try {
                if (scheduledTasks != null) {
                    scheduledTasks.add(command);
                    return;
                }
            } finally {
                rLock.unlock();
            }

            token.execute(command);
        }

        private class BlockingTokenListener implements AccessListener {
            private final RefList.ElementRef<AccessToken<IDType>> tokenRef;

            public BlockingTokenListener(RefList.ElementRef<AccessToken<IDType>> tokenRef) {
                this.tokenRef = tokenRef;
            }

            @Override
            public void onLostAccess() {
                boolean done;

                mainLock.lock();
                try {
                    tokenRef.remove();
                    done = blockingTokens.isEmpty();
                } finally {
                    mainLock.unlock();
                }

                if (done) {
                    enableExecutor();
                }
            }
        }
    }
}
