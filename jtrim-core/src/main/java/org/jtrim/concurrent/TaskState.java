package org.jtrim.concurrent;

/**
 * Defines the possible states of an asynchronously executed task.
 * <P>
 * When submitting a task, it starts its life in the {@link #NOT_STARTED} state.
 * After actually being started it will enter the {@link #RUNNING} state (at
 * this point, the task will always be invoked). Once the task completed, it
 * may enter any of the remaining states: {@link #DONE_CANCELED},
 * {@link #DONE_ERROR} or {@link #DONE_COMPLETED}. They mean, that the task has
 * been canceled, returned by throwing an exception and returned normally in
 * this order. It is possible for tasks to immediately enter the
 * {@code DONE_CANCELED} state if they are canceled before actually being
 * executed.
 * <P>
 * To determine if an instance of {@code TaskState} represents that the task has
 * terminated, use the {@link #isDone()} method.
 *
 * <h3>Thread safety</h3>
 * Instances of this class can be safely accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see TaskExecutorService
 * @see TaskFuture
 *
 * @author Kelemen Attila
 */
public enum TaskState {
    /**
     * The state in which every task starts its life. This state means, that
     * no code of the task had been executed. The next state after this state is
     * either {@link #RUNNING} or {@link #DONE_CANCELED}.
     * <P>
     * The {@link #isDone()} method of this instance returns {@code false}.
     */
    NOT_STARTED(false),

    /**
     * The state meaning that the task is currently being executed. The next
     * state after this state might be any of the following:
     * {@link #DONE_CANCELED}, {@link #DONE_ERROR} or {@link #DONE_COMPLETED}.
     * <P>
     * The {@link #isDone()} method of this instance returns {@code false}.
     */
    RUNNING(false),

    /**
     * The state meaning that the task has been canceled. It may mean, that
     * the task was never attempted to be executed (due to it being canceled
     * prior to being executed) or that it has been canceled while it was
     * executing and thrown an
     * {@link org.jtrim.cancel.OperationCanceledException}. Tasks in this state
     * will remain in this state forever.
     * <P>
     * The {@link #isDone()} method of this instance returns {@code true}.
     */
    DONE_CANCELED(true),

    /**
     * The state meaning that the task has terminated by throwing an exception.
     * Tasks in this state will remain in this state forever.
     * <P>
     * The {@link #isDone()} method of this instance returns {@code true}.
     */
    DONE_ERROR(true),

    /**
     * The state meaning that the task has completed normally without throwing
     * an exception. Tasks in this state will remain in this state forever.
     * <P>
     * The {@link #isDone()} method of this instance returns {@code true}.
     */
    DONE_COMPLETED(true);

    private final boolean done;

    private TaskState(boolean done) {
        this.done = done;
    }

    /**
     * Returns {@code true} if the task is in any of the following states:
     * {@link #DONE_CANCELED}, {@link #DONE_ERROR} or {@link #DONE_COMPLETED};
     * and returns {@code false} otherwise.
     *
     * @return {@code true} if the task has terminated, {@code false} otherwise
     */
    public boolean isDone() {
        return done;
    }
}
