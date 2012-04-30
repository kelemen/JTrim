package org.jtrim.concurrent.executor;

/**
 *
 * @author Kelemen Attila
 */
public interface CleanupTask {
    public void cleanup(boolean canceled, Throwable error);
}
