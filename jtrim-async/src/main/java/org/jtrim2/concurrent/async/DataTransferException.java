package org.jtrim2.concurrent.async;

/**
 * An exception defining a general failure of transferring a data
 * asynchronously.
 *
 * @see AsyncReport
 *
 * @author Kelemen Attila
 */
public class DataTransferException extends Exception {
    private static final long serialVersionUID = 6330457357379838967L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the cause of the failure of the data transfer. This argument
     *   can be {@code null} if there is no cause but this exception describes
     *   the failure.
     */
    public DataTransferException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the cause of the failure of the data transfer. This argument
     *   can be {@code null} if there is no cause but this exception describes
     *   the failure.
     */
    public DataTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public DataTransferException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public DataTransferException() {
    }
}
