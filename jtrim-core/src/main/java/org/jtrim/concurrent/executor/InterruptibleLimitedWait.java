package org.jtrim.concurrent.executor;

/**
 * @see CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleLimitedWait)
 * @see InterruptibleWait
 *
 * @author Kelemen Attila
 */
public interface InterruptibleLimitedWait {
    public boolean await(long nanosToWait) throws InterruptedException;
}
