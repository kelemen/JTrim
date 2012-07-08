package org.jtrim.swing.concurrent;

import org.jtrim.cancel.CancellationToken;

/**
 *
 * @author Kelemen Attila
 */
public interface BackgroundTask {
    public void execute(CancellationToken cancelToken, SwingReporter reporter);
}
