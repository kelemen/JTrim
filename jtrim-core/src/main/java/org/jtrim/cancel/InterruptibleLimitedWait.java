package org.jtrim.cancel;

/**
 * Defines a generic interface to wait for an event to occur or return before
 * the given timeout elapses. This interface was designed for the static
 * {@link CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleLimitedWait)}
 * method. See its documentation for further reference.
 *
 * @see CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleLimitedWait)
 * @see InterruptibleWait
 *
 * @author Kelemen Attila
 */
public interface InterruptibleLimitedWait {
    /**
     * Waits until the implementation defined event occurs or the given timeout
     * elapses. The static
     * {@link CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleLimitedWait)}
     * method calls this method, see its documentation how it uses this method.
     *
     * @param nanosToWait the maximum time to wait in nanoseconds. This argument
     *   must be greater than or equal to zero.
     * @return the return value is implementation specific but usually a
     *   {@code false} return value means that the given maximum time elapsed
     *   without the waited event occurring
     *
     * @throws InterruptedException thrown if the current thread was
     *   interrupted
     */
    public boolean await(long nanosToWait) throws InterruptedException;
}
