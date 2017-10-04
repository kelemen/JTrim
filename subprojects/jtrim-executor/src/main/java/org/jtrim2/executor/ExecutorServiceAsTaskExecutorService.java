package org.jtrim2.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @see ExecutorConverter#asTaskExecutorService(ExecutorService)
 */
final class ExecutorServiceAsTaskExecutorService extends DelegatedTaskExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final ExecutorService executor;

    private final AtomicBoolean shutdownAndCancelInitiated;
    private final AtomicBoolean shutdownInitiated;

    public ExecutorServiceAsTaskExecutorService(ExecutorService executor, boolean mayInterruptTasks) {
        super(new UpgradedTaskExecutor(new ExecutorAsTaskExecutor(executor, mayInterruptTasks)));
        this.executor = executor;
        this.shutdownAndCancelInitiated = new AtomicBoolean();
        this.shutdownInitiated = new AtomicBoolean();
    }

    @Override
    public void shutdownAndCancel() {
        if (!shutdownAndCancelInitiated.compareAndSet(false, true)) {
            return;
        }

        wrappedExecutor.shutdownAndCancel();

        // We can't call shutdownNow because we could violate the specification of TaskExecutor
        shutdownJdkExecutor();
    }

    @Override
    public void shutdown() {
        if (!shutdownInitiated.compareAndSet(false, true)) {
            return;
        }

        wrappedExecutor.shutdown();
        shutdownJdkExecutor();
    }

    private void shutdownJdkExecutor() {
        // We can't just shutdown the wrapped ExecutorService because a JDK executor
        // might throw exceptions after shutdown which is not expected by the wrapper.
        wrappedExecutor.addTerminateListener(() -> {
            executor.shutdown();
        });
    }

}
