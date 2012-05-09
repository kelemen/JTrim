package org.jtrim.concurrent;

/**
 * @deprecated MARKED FOR DELETION
 *
 * A {@link TaskRefusePolicy} which silently ignores refused tasks.
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
@Deprecated
public enum SilentTaskRefusePolicy implements TaskRefusePolicy {
    /**
     * The one and only instance of {@code SilentTaskRefusePolicy}.
     */
    INSTANCE;

    /**
     * This method does nothing and returns immediately to the caller.
     *
     * @param task this argument is ignored
     */
    @Override
    public void refuseTask(Runnable task) {
    }
}
