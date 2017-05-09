package org.jtrim2.concurrent.query;

/**
 * Defines the progress of the providing of a data provided by an
 * {@link AsyncDataLink}.
 * <P>
 * The minimum information implementations must provide is the estimated
 * progress of the data providing process as a double value between 0.0 and 1.0
 * where 0.0 means that the process just started and 1.0 means that it has been
 * completed or very close to being completed.
 * <P>
 * Instances of this interface must not be a view of the process, they must hold
 * the static state of progress and should mean the exact same progress even if
 * the data providing process has progressed some amount since it was requested.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be immutable and therefore
 * safe to be accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are required to be
 * <I>synchronization transparent</I>.
 *
 * @see AsyncDataController
 * @see SimpleDataState
 */
public interface AsyncDataState {
    /**
     * Returns the estimated state of progress within the range [0.0, 1.0].
     * <P>
     * The progress value 0.0 means that the process just started and 1.0 means
     * that it has been completed or very close to being completed.
     * <P>
     * Note that although this method should return a value within the range
     * [0.0, 1.0], callers should expect any other possible double values
     * (including NaNs) because it is easy to make rounding errors with
     * floating point arithmetic. For values lower than 0.0, callers should
     * assume 0.0 and for values greater than 1.0, they should assume 1.0.
     *
     * @return the estimated state of progress within the range [0.0, 1.0].
     *   Note that callers should expect that this method may return values out
     *   of the designed range.
     */
    public double getProgress();
}
