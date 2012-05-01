package org.jtrim.concurrent.executor;

/**
 *
 * @see CancelableWaits#await(CancellationToken, InterruptibleWait)
 * @see InterruptibleLimitedWait
 *
 * @author Kelemen Attila
 */
public interface InterruptibleWait {
    public void await() throws InterruptedException;
}
