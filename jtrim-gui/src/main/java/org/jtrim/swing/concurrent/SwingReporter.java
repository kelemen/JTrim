package org.jtrim.swing.concurrent;

/**
 * Defines an interface to report the progress of a {@link BackgroundTask} or
 * execute other task on the <I>AWT Event Dispatch Thread</I>.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be used by
 * multiple threads concurrently. Note however, that they can only be
 * meaningfully used while the associated background task is being executed.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not required to be
 * <I>synchronization transparent</I> but they are non-blocking relatively quick
 * methods.
 *
 * @see BackgroundTask
 * @see BackgroundTaskExecutor
 *
 * @author Kelemen Attila
 */
public interface SwingReporter {
    /**
     * Executes a task on the <I>AWT Event Dispatch Thread</I> which is intended
     * to be used to display the current progress of the associated background
     * task.
     * <P>
     * The submitted task will be executed in the context of the access token
     * associated with the background task. If the access token has been
     * released, the submitted task will not be executed but will be silently
     * discarded.
     * <P>
     * Calling this method may cause tasks submitted by this method previously
     * to be discarded. This allows the task to update its progress frequently
     * and not worry about flooding the event queue.
     * <P>
     * Tasks submitted by this method and the
     * {@link #writeData(Runnable) writeData} method are executed in the same
     * order as they were submitted.
     *
     * @param task the task to be executed on the
     *   <I>AWT Event Dispatch Thread</I>. This argument cannot be {@code null}.
     */
    public void updateProgress(Runnable task);

    /**
     * Executes a generic task on the <I>AWT Event Dispatch Thread</I>. This
     * method is intended to be used to write intermediate data created by the
     * associated background task.
     * <P>
     * The submitted task will be executed in the context of the access token
     * associated with the background task. If the access token has been
     * released, the submitted task will not be executed but will be silently
     * discarded.
     * <P>
     * This method differs from the {@code updateProgress} method only by not
     * being discarded due to tasks submitted later.
     * <P>
     * Tasks submitted by this method and the
     * {@link #updateProgress(Runnable) updateProgress} method are executed in
     * the same order as they were submitted.
     *
     * @param task the task to be executed on the
     *   <I>AWT Event Dispatch Thread</I>. This argument cannot be {@code null}.
     */
    public void writeData(Runnable task);
}
