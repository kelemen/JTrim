package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Kelemen Attila
 */
public final class ExecutorConverter {
    public static ExecutorService asExecutorService(TaskExecutorService executor) {
        if (executor instanceof ExecutorServiceAsTaskExecutorService) {
            return ((ExecutorServiceAsTaskExecutorService)executor).executor;
        }
        else {
            return new TaskExecutorServiceAsExecutorService(executor);
        }
    }

    public static TaskExecutorService asTaskExecutorService(ExecutorService executor) {
        if (executor instanceof TaskExecutorServiceAsExecutorService) {
            return ((TaskExecutorServiceAsExecutorService)executor).executor;
        }
        else {
            return new ExecutorServiceAsTaskExecutorService(executor);
        }
    }

    public static Executor asExecutor(TaskExecutor executor) {
        return new TaskExecutorAsExecutor(executor);
    }

    private ExecutorConverter() {
        throw new AssertionError();
    }
}
