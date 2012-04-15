package org.jtrim.concurrent;

/**
 * The listener interface for receiving notification when an
 * {@link java.util.concurrent.ExecutorService ExecutorService} no longer
 * executes tasks scheduled to it. Note that this not only means that the
 * {@code ExecutorService} will no longer accept tasks to be submitted to it but
 * tasks already submitted to it will not be executed as well when this listener
 * is notified.
 * <P>
 * Note that not all {@code ExecutorService} implementations supports this
 * event listener (obviously implementations provided by Java does not). However
 * to add such support for any {@link java.util.concurrent.Executor Executor}
 * and not just {@code ExecutorService}, you can use {@link UpgraderExecutor}.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * This listener is not required to be <I>synchronization transparent</I> but
 * they must expect to be notified on any thread.
 *
 * @see UpgraderExecutor
 * @author Kelemen Attila
 */
public interface ExecutorShutdownListener {
    /**
     * Invoked when there are no more tasks will be executed by the
     * {@code ExecutorService} to which this listener was registered to. This
     * method may only be invoked at most once by the {@code ExecutorService}.
     */
    public void onTerminate();
}
