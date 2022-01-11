package org.jtrim2.concurrent.collections;

/**
 * Thrown if a queue was shut down, and still trying to add or remove element.
 *
 * @see TerminableQueue
 */
public class TerminatedQueueException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the causing exception of the {@code TerminatedQueueException}.
     *   The cause of {@code TerminatedQueueException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling termination. This argument
     *   can be {@code null} if there is no cause.
     */
    public TerminatedQueueException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the causing exception of the {@code TerminatedQueueException}.
     *   The cause of {@code TerminatedQueueException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling termination. This argument
     *   can be {@code null} if there is no cause.
     */
    public TerminatedQueueException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public TerminatedQueueException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public TerminatedQueueException() {
    }

    /**
     * Constructs a new exception with the specified detail
     * message, cause, and writable stack trace enabled or disabled.
     *
     * @param message the detail message.
     * @param cause the cause.  (A {@code null} value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @param writableStackTrace whether or not the stack trace should
     *   be writable
     */
    protected TerminatedQueueException(String message, Throwable cause, boolean writableStackTrace) {
        super(message, cause, true, writableStackTrace);
    }

    /**
     * Returns a new instance of {@code TerminatedQueueException} without stack trace
     * information. The stack trace cannot be set later.
     *
     * @return a new instance of {@code TerminatedQueueException} without stack trace
     *   information. This method never returns {@code null}.
     */
    public static TerminatedQueueException withoutStackTrace() {
        return withoutStackTrace("terminated", null);
    }

    /**
     * Returns a new instance of {@code TerminatedQueueException} without stack trace
     * information. The stack trace cannot be set later.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause cause the cause (which is saved for later retrieval by the
     *   {@link #getCause()} method). (A {@code null} value is
     *   permitted, and indicates that the cause is nonexistent or
     *   unknown.)
     * @return a new instance of {@code TerminatedQueueException} without stack trace
     *   information. This method never returns {@code null}.
     */
    public static TerminatedQueueException withoutStackTrace(String message, Throwable cause) {
        return new TerminatedQueueException(message, cause, false);
    }
}
