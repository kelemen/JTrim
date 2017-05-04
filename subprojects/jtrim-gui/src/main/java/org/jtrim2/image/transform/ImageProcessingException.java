package org.jtrim2.image.transform;

/**
 * Thrown when an image cannot be transformed due to some reason. This exception
 * is meant to be thrown by {@link ImageTransformer} implementations when they
 * encounter an unexpected transformation issue. This might be due to an
 * unexpected {@code NaN} value or due to some some other unexpected properties.
 *
 * @author Kelemen Attila
 */
public class ImageProcessingException extends RuntimeException {
    private static final long serialVersionUID = 6999620545186048398L;

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public ImageProcessingException() {
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public ImageProcessingException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the cause of the {@code ImageProcessingException}. This
     *   exception can later be retrieved by the {@link #getCause() getCause()}
     *   method. This argument can be {@code null} if there is no such cause.
     */
    public ImageProcessingException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the cause of the {@code ImageProcessingException}. This
     *   exception can later be retrieved by the {@link #getCause() getCause()}
     *   method. This argument can be {@code null} if there is no such cause.
     */
    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
