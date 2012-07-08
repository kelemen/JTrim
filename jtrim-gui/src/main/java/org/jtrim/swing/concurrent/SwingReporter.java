package org.jtrim.swing.concurrent;

/**
 *
 * @author Kelemen Attila
 */
public interface SwingReporter {
    public void updateProgress(Runnable task);
    public void writeData(Runnable task);
}
