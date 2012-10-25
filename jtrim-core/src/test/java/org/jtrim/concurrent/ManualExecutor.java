package org.jtrim.concurrent;

import java.util.LinkedList;
import java.util.List;
import org.jtrim.cancel.CancellationToken;

/**
 *
 * @author Kelemen Attila
 */
public final class ManualExecutor implements TaskExecutor {
    private final List<Runnable> submittedTasks = new LinkedList<>();

    public void executeOne() {
        submittedTasks.remove(0).run();
    }

    public void executeAll() {
        while (!submittedTasks.isEmpty()) {
            executeOne();
        }
    }

    @Override
    public void execute(final CancellationToken cancelToken, final CancelableTask task, final CleanupTask cleanupTask) {
        submittedTasks.add(new Runnable() {
            @Override
            public void run() {
                Tasks.executeTaskWithCleanup(cancelToken, task, cleanupTask);
            }
        });
    }
}
