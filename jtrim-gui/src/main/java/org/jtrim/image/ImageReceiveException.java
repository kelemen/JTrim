package org.jtrim.image;

import org.jtrim.concurrent.async.DataTransferException;

/**
 * @deprecated This exception is needed only for {@link ImageData} which is
 *   deprecated.
 *
 * Defines the cause of a failure of an image retrieval attempt.
 *
 * @author Kelemen Attila
 */
@Deprecated
public class ImageReceiveException extends DataTransferException {
    private static final long serialVersionUID = -8807903975010525439L;

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public ImageReceiveException() {
    }


    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public ImageReceiveException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the cause of the {@code ImageReceiveException}. This
     *   exception can later be retrieved by the {@link #getCause() getCause()}
     *   method. This argument can be {@code null} if there is no such cause.
     */
    public ImageReceiveException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the cause of the {@code ImageReceiveException}. This
     *   exception can later be retrieved by the {@link #getCause() getCause()}
     *   method. This argument can be {@code null} if there is no such cause.
     */
    public ImageReceiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
