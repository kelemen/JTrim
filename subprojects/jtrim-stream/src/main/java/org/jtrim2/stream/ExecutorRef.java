package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ExecutorRef {
    private final TaskExecutor executor;
    private final Runnable finishExecutorTask;

    private ExecutorRef(TaskExecutor executor, Runnable finishExecutorTask) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.finishExecutorTask = Objects.requireNonNull(finishExecutorTask, "finishExecutorTask");
    }

    public static Supplier<ExecutorRef> owned(Supplier<? extends TaskExecutorService> executorFactory) {
        Objects.requireNonNull(executorFactory, "executorFactory");
        return () -> {
            TaskExecutorService executor = executorFactory.get();
            return new ExecutorRef(executor, () -> {
                executor.shutdown();
                executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
            });
        };
    }

    public static Supplier<ExecutorRef> owned(String name, int consumerThreadCount) {
        Objects.requireNonNull(name, "name");
        ExceptionHelper.checkArgumentInRange(consumerThreadCount, 1, Integer.MAX_VALUE, "consumerThreadCount");
        return owned(() -> {
            return consumerThreadCount == 1
                    ? new SingleThreadedExecutor(name)
                    : new ThreadPoolTaskExecutor(name, consumerThreadCount);
        });
    }

    public static Supplier<ExecutorRef> external(TaskExecutor executor) {
        ExecutorRef executorRef = new ExecutorRef(executor, Tasks.noOpTask());
        return () -> executorRef;
    }

    public TaskExecutor getExecutor() {
        return executor;
    }

    public void finishUsage() {
        finishExecutorTask.run();
    }

}
