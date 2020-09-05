package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadFactory;
import org.jtrim2.cancel.CancellationToken;

final class DedicatedThreadTaskExecutor implements TaskExecutor {
    public static final TaskExecutor DEFAULT_NON_DAEMON_EXECUTOR = TaskExecutors
            .newThreadExecutor(new ExecutorsEx.NamedThreadFactory(false));

    public static final TaskExecutor DEFAULT_DAEMON_EXECUTOR = TaskExecutors
            .newThreadExecutor(new ExecutorsEx.NamedThreadFactory(true));

    private final ThreadFactory threadFactory;

    public DedicatedThreadTaskExecutor(ThreadFactory threadFactory) {
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(function, "function");

        CompletableFuture<V> future = new CompletableFuture<>();

        Thread thread = threadFactory.newThread(() -> {
            CancelableTasks.complete(cancelToken, function, future);
        });
        Objects.requireNonNull(thread, "threadFactory.newThread");
        thread.start();

        return future;
    }

    @Override
    public void execute(Runnable command) {
        Thread thread = threadFactory.newThread(Objects.requireNonNull(command, "command"));
        thread.start();
    }
}
