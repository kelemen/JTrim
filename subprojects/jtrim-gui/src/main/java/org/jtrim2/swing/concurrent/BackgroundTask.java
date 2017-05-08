package org.jtrim2.swing.concurrent;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a task which may take long time and is to be executed in the
 * background. This task is intended to be used in <I>Swing</I> applications
 * and be executed by a {@link BackgroundTaskExecutor}.
 * <P>
 * The task is to be executed in the context of an
 * {@link org.jtrim2.access.AccessToken access token}
 * ({@code BackgroundTaskExecutor} automatically does so). The task will have
 * a chance to report its progress (or anything else) on the
 * <I>AWT Event Dispatch Thread</I>. This reports are also executed in the
 * context of the same access token in which the task is being executed.
 *
 * <h3>Thread safety</h3>
 * The thread safety property of {@code BackgroundTask} is completely
 * implementation dependent and the submitter of the task must consider it when
 * submitting it.
 *
 * <h4>Synchronization transparency</h4>
 * {@code BackgroundTask} is not required to be
 * <I>synchronization transparent</I>. The {@code execute} method of this class
 * should not be executed on the EDT as it can be lengthy.
 *
 * @see BackgroundTaskExecutor
 */
public interface BackgroundTask {
    /**
     * Executes the implementation dependent task of this
     * {@code BackgroundTask}. This method may possibly be lengthy and in
     * general must not be executed on the EDT (so that it may not prevent
     * events to processed).
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this task to detect cancellation requests. This
     *   argument cannot be {@code null}.
     * @param reporter the object which can be used to report the progress of
     *   the execution of this task (or possibly anything else). This argument
     *   cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if
     *   cancellation has been requested and this method did not complete due to
     *   this request
     * @throws Exception thrown if there was an error while executing this
     *   task. Note that regardless, you should only throw truly unexpected
     *   exceptions  because you probably need to report such exceptions to the
     *   user.
     */
    public void execute(CancellationToken cancelToken, SwingReporter reporter) throws Exception;
}
