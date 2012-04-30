package org.jtrim.concurrent.executor;

/**
 *
 * @author Kelemen Attila
 */
public interface CancelableTask {
    public void run(CancellationToken cancelToken);
}
