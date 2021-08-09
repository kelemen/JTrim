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
     *   stack trace of other exceptions signaling cancellation. This argument
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
     *   stack trace of other exceptions signaling cancellation. This argument
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
}
