package org.jtrim2.swing.concurrent;

import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.DelegatedTaskExecutorService;
import org.jtrim2.executor.TaskExecutors;

final class SwingTaskExecutor extends DelegatedTaskExecutorService {
    public SwingTaskExecutor(boolean alwaysInvokeLater) {
        super(TaskExecutors.upgradeToStoppable(SwingExecutors.getStrictExecutor(alwaysInvokeLater)));
    }

    private static void checkWaitOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Waiting on the EDT would be a good way to cause a dead-lock.
            throw new IllegalStateException("Cannot wait for termination on the Event Dispatch Thread.");
        }
    }

    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        checkWaitOnEDT();
        wrappedExecutor.awaitTermination(cancelToken);
    }

    @Override
    public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        checkWaitOnEDT();
        return wrappedExecutor.tryAwaitTermination(cancelToken, timeout, unit);
    }
}
