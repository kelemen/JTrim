package org.jtrim.concurrent.executor;

/**
 * Defines a generic interface to wait for an event to occur. This interface was
 * designed for the static
 * {@link CancelableWaits#await(CancellationToken, InterruptibleWait)} method.
 * See its documentation for further reference.
 *
 * @see CancelableWaits#await(CancellationToken, InterruptibleWait)
 * @see InterruptibleLimitedWait
 *
 * @author Kelemen Attila
 */
public interface InterruptibleWait {
    /**
     * Waits until the implementation defined event occurs. The static
     * {@link CancelableWaits#await(CancellationToken, InterruptibleWait)}
     * method calls this method, see its documentation how it uses this method.
     *
     * @throws InterruptedException thrown if the current thread was
     *   interrupted
     */
    public void await() throws InterruptedException;
}
