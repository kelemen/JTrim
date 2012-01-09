package org.jtrim.concurrent;

import java.util.concurrent.atomic.*;

import org.jtrim.utils.*;

/**
 * Defines a task which executes a given task exactly once when run, even if
 * run multiple times. That is, only one of the calls (the first one) to the
 * {@link #run() run()} method will execute the {@code run()} method of the
 * underlying task, every other call will be a no-op and returns immediately.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code run()} method of this class derives its
 * <I>synchronization transparency</I> property from the underlying task and
 * safe to execute in contexts where the underlying task is safe to execute.
 * The constructor of this class is <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class IdempotentTask implements Runnable {
    private final AtomicReference<Runnable> task;

    /**
     * Creates a new task which will forward its {@code run()} method to the
     * {@code run()} method of the specified task but only the first time it
     * is called.
     *
     * @param task the underlying task to be used. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public IdempotentTask(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.task = new AtomicReference<>(task);
    }

    /**
     * The first time this method is called, it will call the {@code run()}
     * method of the underlying task specified at construction time. Subsequent
     * calls will do nothing and return immediately. Note that even if this
     * method is called concurrently, the {@code run()} method of the underlying
     * task will still not be called multiple times.
     * <P>
     * This method will throw the exception thrown by the underlying task when
     * it is executed.
     */
    @Override
    public void run() {
        Runnable currentTask = task.getAndSet(null);
        if (currentTask != null) {
            currentTask.run();
        }
    }
}
