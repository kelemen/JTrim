package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@code UpdateTaskExecutor} implementation which forwards tasks scheduled
 * to it to a given {@code Executor} or {@code TaskExecutor}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently
 * as required by {@code UpdateTaskExecutor}.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>.
 */
public final class GenericUpdateTaskExecutor implements UpdateTaskExecutor {
    private final Executor taskExecutor;

    private final Runnable pollTask;
    private final AtomicReference<Runnable> currentTask;

    /**
     * Creates a new {@code GenericUpdateTaskExecutor} which will forward its
     * tasks to the given {@code TaskExecutor}.
     *
     * @param taskExecutor the executor to which tasks will be forwarded to.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}.
     */
    public GenericUpdateTaskExecutor(Executor taskExecutor) {
        Objects.requireNonNull(taskExecutor, "taskExecutor");

        this.currentTask = new AtomicReference<>(null);
        this.taskExecutor = taskExecutor;
        this.pollTask = () -> {
            Runnable task = currentTask.getAndSet(null);
            if (task != null) {
                task.run();
            }
        };
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
        Objects.requireNonNull(task, "task");

        Runnable oldTask = currentTask.getAndSet(task);
        // If oldTask != null, there must be a poll task already scheduled
        // which will execute the task set.
        if (oldTask == null) {
            taskExecutor.execute(pollTask);
        }
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
}
