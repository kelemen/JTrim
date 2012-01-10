package org.jtrim.concurrent;

import java.util.concurrent.Executor;

/**
 * Defines an executor which invokes tasks in the order they were scheduled
 * to it. The tasks will execute in one of the
 * {@link #execute(Runnable) execute} call of the same executor but the user
 * has no influence in which one it will actually be called.
 * <P>
 * Creating an instance of this executor is effectively the same as the executor
 * created by the following code:
 * {@code new InOrderExecutor(SyncTaskExecutor.getSimpleExecutor())}. However
 * this implementation is slightly more efficient. So see
 * {@link InOrderExecutor} for further details.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>
 * unless all the tasks scheduled to this executor are
 * <I>synchronization transparent</I>.
 *
 * @see InOrderExecutor
 * @see UpgraderExecutor
 * @see TaskScheduler
 * @author Kelemen Attila
 */
public final class InOrderScheduledSyncExecutor implements Executor {
    private final TaskScheduler taskScheduler;

    /**
     * Creates a new executor which invokes tasks in the order they were
     * scheduled to it.
     */
    public InOrderScheduledSyncExecutor() {
        taskScheduler = TaskScheduler.newSyncScheduler();
    }

    /**
     * Submits the task for execution. The task will eventually be executed in
     * one of the {@code execute} call of this executor.
     *
     * @param command the task to be executed. This argument cannot be
     *   {@code null}.
     */
    @Override
    public void execute(Runnable command) {
        taskScheduler.scheduleTask(command);
        taskScheduler.dispatchTasks();
    }

    /**
     * Checks whether the calling code is running in a task scheduled to this
     * executor.
     *
     * @return {@code true} if the calling code is running in a task scheduled
     *   to this executor, {@code false} otherwise
     */
    public boolean isCurrentThreadExecuting() {
        return taskScheduler.isCurrentThreadDispatching();
    }
}
