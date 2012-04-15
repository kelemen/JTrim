package org.jtrim.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link TaskRefusePolicy} which throws a {@code RejectedExecutionException}
 * when a task was refused to be executed.
 * <P>
 * Note that there is only a single instance of this class which can be accessed
 * through {@link #INSTANCE}.
 *
 * <h3>Thread safety</h3>
 * This class is completely safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I> and
 * can be called from any context (e.g.: while holding a lock).
 *
 * @author Kelemen Attila
 */
public enum AbortTaskRefusePolicy implements TaskRefusePolicy {
    /**
     * The one and only instance of {@code AbortTaskRefusePolicy}.
     */
    INSTANCE;

    /**
     * Throws an {@code RejectedExecutionException} always.
     *
     * @param task {@inheritDoc }
     *
     * @throws RejectedExecutionException thrown always
     */
    @Override
    public void refuseTask(Runnable task) {
        throw new RejectedExecutionException("The task cannot be executed in"
                + " the current state.");
    }

}
