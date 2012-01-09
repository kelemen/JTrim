package org.jtrim.concurrent;

/**
 * Defines an interface for cancelable tasks. This interface is intentionally
 * defines the same method as the {@link java.util.concurrent.Future} interface
 * and specifies the same meaning to the {@link #cancel(boolean) cancel} method.
 * So tasks implementing the {@code Future} interface may also implement this
 * interface safely.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public interface Cancelable {
    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been canceled,
     * or could not be canceled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run.  If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return <tt>true</tt>.  Subsequent calls to {@link #isCancelled}
     * will always return <tt>true</tt> if this method returned <tt>true</tt>.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     * @return <tt>false</tt> if the task could not be canceled,
     * typically because it has already completed normally;
     * <tt>true</tt> otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning);
}
