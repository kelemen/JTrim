package org.jtrim2.concurrent.async;

/**
 * Defines how an asynchronous data transfer has completed.
 * <P>
 * Note that this class does not have a public constructor and can only be
 * initiated by the static factory method: {@link #getReport(Throwable, boolean)}.
 * <P>
 * The following two properties of the data transfer completion are supported:
 * <ul>
 *  <li>
 *   If it was canceled or not.
 *  </li>
 *  <li>
 *   The exception describing the error occurred while transferring the data.
 *  </li>
 * </ul>
 * When {@link AsyncDataLink} instances report that the data transferring was
 * {@link #isCanceled() canceled}, it should be interpreted as the final data
 * received might not be the fully complete (but can be) data intended to be
 * retrieved. Also when a non-null {@link #getException() exception} is attached
 * to the  {@code AsyncReport} instance, the final data might not be the fully
 * complete data due to some errors while trying to retrieve the required data.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are completely safe to be accessed from multiple
 * threads concurrently. Also, instances are immutable except that the attached
 * exception (as every exception in Java) is mutable. Note however, that
 * exceptions are safe to be accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see #getReport(Throwable, boolean)
 * @see AsyncDataLink
 * @see AsyncDataListener
 * @author Kelemen Attila
 */
public final class AsyncReport {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 128;

    /**
     * A completely successful data transfer. This {@code AsyncReport} instance
     * is not {@link #isCanceled() canceled} and does not have an exception
     * object attached to it.
     * <P>
     * This {@code AsyncReport} object is completely immutable.
     */
    public static final AsyncReport SUCCESS = new AsyncReport(null, false);

    /**
     * A canceled but error free data transfer. This {@code AsyncReport}
     * instance is {@link #isCanceled() canceled} and does not have an exception
     * object attached to it.
     * <P>
     * This {@code AsyncReport} object is completely immutable.
     */
    public static final AsyncReport CANCELED = new AsyncReport(null, true);

    /**
     * Returns an {@code AsyncReport} instance with the specified exception
     * attached to it and with the specified canceled state.
     * <P>
     * Note that this method does not necessarily returns a new unique object if
     * not necessary.
     *
     * @param exception the exception attached to the returned
     *   {@code AsyncReport} instance. This is the exception which will be
     *   returned by the {@link #getException() getException()} method of the
     *   created {@code AsyncReport}. This argument can be {@code null} which
     *   means that no exception is attached to the returned {@code AsyncReport}
     *   instance, that is the data retrieval process has terminate without
     *   errors.
     * @param canceled the canceled state of the returned {@code AsyncReport}
     *   instance. This value will be returned by the
     *   {@link #isCanceled() isCanceled} method of the returned
     *   {@code AsyncReport} instance.
     * @return the {@code AsyncReport} instance with the specified exception
     *   attached to it and with the specified canceled state. This method never
     *   returns {@code null}.
     */
    public static AsyncReport getReport(Throwable exception,
            boolean canceled) {

        if (exception == null) {
            return canceled ? CANCELED : SUCCESS;
        }
        else {
            return new AsyncReport(exception, canceled);
        }
    }

    private final boolean canceled;
    private final Throwable exception;

    private AsyncReport(Throwable exception, boolean canceled) {
        this.exception = exception;
        this.canceled = canceled;
    }

    /**
     * Returns {@code true} if this {@code AsyncReport} defines a completely
     * successful completion of a data transfer. That is, if it was not
     * {@link #isCanceled() canceled} and does not have an exception attached
     * to it.
     * <P>
     * If this method returns {@code true}, a data transfer requested from an
     * {@link AsyncDataLink} must have returned the complete requested data.
     *
     * @return {@code true} if this {@code AsyncReport} defines a completely
     * successful completion of a data transfer, {@code false} otherwise
     *
     * @see #isCanceled()
     * @see #getException()
     */
    public boolean isSuccess() {
        return !canceled && exception == null;
    }

    /**
     * Returns {@code true} if this {@code AsyncReport} defines a canceled
     * data transfer.
     * <P>
     * If this method returns {@code true}, a data transfer requested from an
     * {@link AsyncDataLink} may not have returned the complete requested data.
     *
     * @return {@code true} if this {@code AsyncReport} defines a canceled
     *   data transfer, {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns the exception describing the error occurred during the
     * data transfer of which completion this {@code AsyncReport} represents.
     * <P>
     * If this method returns a non-null exception, a data transfer requested
     * from an {@link AsyncDataLink} may not have returned the complete
     * requested data.
     *
     * @return the exception describing the error occurred during the data
     *   transfer of which completion this {@code AsyncReport} represents or
     *   {@code null} if no error occurred while transferring the data
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * Returns the string representation of this {@code AsyncReport} in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        if (isSuccess()) {
            return "AsyncReport: SUCCESS";
        }

        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);

        result.append("AsyncReport: ");
        if (canceled) {
            result.append("CANCELED");
        }

        if (exception != null) {
            if (canceled) {
                result.append(", ");
            }
            result.append("Exception=");
            result.append(exception);
        }

        return result.toString();
    }
}
