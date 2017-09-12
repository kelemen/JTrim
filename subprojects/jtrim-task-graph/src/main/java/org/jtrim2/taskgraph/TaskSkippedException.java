package org.jtrim2.taskgraph;

import org.jtrim2.cancel.OperationCanceledException;

/**
 * Defines an exception to be thrown when a task execution is skipped but failing to
 * compute a task is not considered an error for the whole computation. That is, tasks
 * depending on the task throwing this exception will also be skipped but unrelated tasks
 * will continue as if no error occurred.
 */
public class TaskSkippedException extends OperationCanceledException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the causing exception of the {@code TaskSkippedException}.
     *   The cause of {@code TaskSkippedException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public TaskSkippedException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the causing exception of the {@code TaskSkippedException}.
     *   The cause of {@code TaskSkippedException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public TaskSkippedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public TaskSkippedException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public TaskSkippedException() {
    }
}
