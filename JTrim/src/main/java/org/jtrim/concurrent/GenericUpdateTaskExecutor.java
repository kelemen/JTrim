package org.jtrim.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jtrim.utils.*;

/**
 * An {@code UpdateTaskExecutor} implementation which forwards tasks scheduled
 * to it to a given {@code Executor}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently
 * as required by {@code UpdateTaskExecutor}.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class GenericUpdateTaskExecutor implements UpdateTaskExecutor {
    private final Executor taskExecutor;

    private final PollAndExecuteTask pollTask;
    private final AtomicReference<Runnable> currentTask;
    private volatile boolean stopped;

    /**
     * Creates a new {@code GenericUpdateTaskExecutor} which will forward its
     * tasks to the given {@code Executor}.
     *
     * @param taskExecutor the executor to which tasks will be forwarded to.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}.
     */
    public GenericUpdateTaskExecutor(Executor taskExecutor) {
        ExceptionHelper.checkNotNullArgument(taskExecutor, "taskExecutor");

        this.stopped = false;
        this.pollTask = new PollAndExecuteTask();
        this.currentTask = new AtomicReference<>(null);
        this.taskExecutor = taskExecutor;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: Note that this method will not forward the specified
     * task to the underlying executor but a different one which will execute
     * this task.
     */
    @Override
    public void execute(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        // This check is here to not even try to schedule a task to the executor
        // after shutdown() because users may not expect to schedule any task
        // to the underlying executor (even if it is effectievly a no-op.
        if (stopped) {
            return;
        }

        Runnable oldTask = currentTask.getAndSet(task);
        // If oldTask != null, there must be a poll task already scheduled
        // which will execute the task set.
        if (oldTask == null) {
            taskExecutor.execute(pollTask);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This method is actually
     * <I>synchronization transparent</I>.
     */
    @Override
    public void shutdown() {
        stopped = true;
    }

    /**
     * Returns the string representation of this executor in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "GenericUpdateTaskExecutor{" + taskExecutor + '}';
    }

    private class PollAndExecuteTask implements Runnable {
        @Override
        public void run() {
            Runnable task = currentTask.getAndSet(null);

            if (task != null && !stopped) {
                task.run();
            }
        }
    }
}
