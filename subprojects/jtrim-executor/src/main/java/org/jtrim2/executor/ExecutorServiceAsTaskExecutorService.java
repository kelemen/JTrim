package org.jtrim2.executor;

import java.util.concurrent.ExecutorService;

/**
 * @see ExecutorConverter#asTaskExecutorService(ExecutorService)
 */
final class ExecutorServiceAsTaskExecutorService extends DelegatedTaskExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final ExecutorService executor;

    public ExecutorServiceAsTaskExecutorService(ExecutorService executor, boolean mayInterruptTasks) {
        super(new UpgradedTaskExecutor(new ExecutorServiceAsTaskExecutor(executor, mayInterruptTasks)));
        this.executor = executor;
    }
}
