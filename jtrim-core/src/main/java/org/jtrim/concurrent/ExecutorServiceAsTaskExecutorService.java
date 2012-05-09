package org.jtrim.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * @see ExecutorConverter#asTaskExecutorService(ExecutorService)
 *
 * @author Kelemen Attila
 */
final class ExecutorServiceAsTaskExecutorService extends DelegatedTaskExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final ExecutorService executor;

    public ExecutorServiceAsTaskExecutorService(ExecutorService executor) {
        super(new UpgradedTaskExecutor(new ExecutorServiceAsTaskExecutor(executor)));
        this.executor = executor;
    }
}
