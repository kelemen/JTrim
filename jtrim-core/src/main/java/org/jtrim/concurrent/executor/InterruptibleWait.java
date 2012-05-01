package org.jtrim.concurrent.executor;

/**
 *
 * @see CancelableWaits#await(CancellationToken, InterruptibleWait)
 * @see CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleWait)
 *
 * @author Kelemen Attila
 */
public interface InterruptibleWait {
    public boolean await(long nanosToWait) throws InterruptedException;
}
