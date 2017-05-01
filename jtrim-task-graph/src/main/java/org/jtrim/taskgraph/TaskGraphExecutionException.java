package org.jtrim.taskgraph;

/**
 * Defines an exception meaning that a task graph execution failed due to
 * a node's action throwing an exception.
 *
 * @see TaskGraphExecutor
 */
public class TaskGraphExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private static final StackTraceElement[] NO_STACK = new StackTraceElement[0];

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public TaskGraphExecutionException() {
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public TaskGraphExecutionException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause cause the cause (which is saved for later retrieval by the
     *   {@link #getCause()} method).  (A <tt>null</tt> value is
     *   permitted, and indicates that the cause is nonexistent or
     *   unknown.)
     */
    public TaskGraphExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new runtime exception with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *   {@link #getCause()} method).  (A <tt>null</tt> value is
     *   permitted, and indicates that the cause is nonexistent or
     *   unknown.)
     */
    public TaskGraphExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail
     * message, cause, and writable stack trace enabled or disabled.
     *
     * @param message the detail message.
     * @param cause the cause.  (A {@code null} value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @param writableStackTrace whether or not the stack trace should
     *   be writable
     */
    protected TaskGraphExecutionException(String message, Throwable cause, boolean writableStackTrace) {
        super(message, cause, true, writableStackTrace);
    }

    /**
     * Returns a new instance of {@code TaskGraphExecutionException} without stack trace
     * information. The stack trace cannot be set later.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause cause the cause (which is saved for later retrieval by the
     *   {@link #getCause()} method).  (A <tt>null</tt> value is
     *   permitted, and indicates that the cause is nonexistent or
     *   unknown.)
     * @return a new instance of {@code TaskGraphExecutionException} without stack trace
     *   information. This method never returns {@code null}.
     */
    public static TaskGraphExecutionException withoutStackTrace(String message, Throwable cause) {
        return new TaskGraphExecutionException(message, cause, false);
    }
}
