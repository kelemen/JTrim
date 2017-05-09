package org.jtrim2.concurrent.query;

/**
 * An exception describing unexpected failure in one or more
 * {@link AsyncDataListener}. The actual exceptions can be stored as the cause
 * and/or be suppressed using the {@link #addSuppressed(Throwable)} method.
 */
public class SublistenerException extends RuntimeException {
    private static final long serialVersionUID = -2529316001636841487L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the exception thrown by the a {@link AsyncDataListener}.
     *   This argument can be {@code null} if there is no cause and exceptions
     *   will be {@link #addSuppressed(Throwable) suppressed} instead.
     */
    public SublistenerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the exception thrown by the a {@link AsyncDataListener}.
     *   This argument can be {@code null} if there is no cause and exceptions
     *   will be {@link #addSuppressed(Throwable) suppressed} instead.
     */
    public SublistenerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public SublistenerException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public SublistenerException() {
    }
}
