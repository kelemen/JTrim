package org.jtrim.concurrent.executor;

import org.jtrim.event.ListenerRef;

/**
 *
 * @author Kelemen Attila
 */
public interface CancellationToken {
    public ListenerRef<Runnable> addCancellationListener(Runnable task);
    public boolean isCanceled();
    public void checkCanceled();
}
