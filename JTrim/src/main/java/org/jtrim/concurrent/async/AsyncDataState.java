package org.jtrim.concurrent.async;

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
 * @author Kelemen Attila
 */
public interface AsyncDataState {
    public double getProgress();
}
