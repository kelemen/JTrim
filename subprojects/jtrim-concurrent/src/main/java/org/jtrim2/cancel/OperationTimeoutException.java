package org.jtrim2.cancel;

/**
 * Defines an exception for when an operation was canceled due to timing out.
 */
public class OperationTimeoutException extends OperationCanceledException {
    private static final long serialVersionUID = 6512650623463624418L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the causing exception of the {@code OperationTimeoutException}.
     *   The cause of {@code OperationTimeoutException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public OperationTimeoutException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the causing exception of the {@code OperationTimeoutException}.
     *   The cause of {@code OperationTimeoutException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public OperationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public OperationTimeoutException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public OperationTimeoutException() {
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
    protected OperationTimeoutException(String message, Throwable cause, boolean writableStackTrace) {
        super(message, cause, writableStackTrace);
    }

    /**
     * Returns a new instance of {@code OperationTimeoutException} without stack trace
     * information. The stack trace cannot be set later.
     *
     * @return a new instance of {@code OperationTimeoutException} without stack trace
     *   information. This method never returns {@code null}.
     */
    public static OperationTimeoutException withoutStackTrace() {
        return withoutStackTrace("canceled", null);
    }

    /**
     * Returns a new instance of {@code OperationTimeoutException} without stack trace
     * information. The stack trace cannot be set later.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause cause the cause (which is saved for later retrieval by the
     *   {@link #getCause()} method). (A {@code null} value is
     *   permitted, and indicates that the cause is nonexistent or
     *   unknown.)
     * @return a new instance of {@code TaskGraphExecutionException} without stack trace
     *   information. This method never returns {@code null}.
     */
    public static OperationTimeoutException withoutStackTrace(String message, Throwable cause) {
        return new OperationTimeoutException(message, cause, false);
    }
}
