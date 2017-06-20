package org.jtrim2.executor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see ExecutorConverter#asExecutorService(TaskExecutorService)
 */
final class TaskExecutorServiceAsExecutorService extends AbstractExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final TaskExecutorService executor;
    private final boolean mayInterruptTasks;

    public TaskExecutorServiceAsExecutorService(TaskExecutorService executor, boolean mayInterruptTasks) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.mayInterruptTasks = mayInterruptTasks;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        executor.shutdownAndCancel();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    private void executeUninterruptibly(final Runnable command) {
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            command.run();
        });
    }

    private void executeInterruptibly(final Runnable command) {
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            Cancellation.doAsCancelable(cancelToken, (CancellationToken taskCancelToken) -> {
                command.run();
                return null;
            });
        });
    }

    @Override
    public void execute(final Runnable command) {
        Objects.requireNonNull(command, "command");

        if (mayInterruptTasks) {
            executeInterruptibly(command);
        } else {
            executeUninterruptibly(command);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        final RunnableFuture<T> result = super.newTaskFor(runnable, value);
        if (mayInterruptTasks) {
            return result;
        } else {
            return new NotInterruptingRunnableFuture<>(result);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        final RunnableFuture<T> result = super.newTaskFor(callable);
        if (mayInterruptTasks) {
            return result;
        } else {
            return new NotInterruptingRunnableFuture<>(result);
        }
    }

    private static class NotInterruptingRunnableFuture<T> implements RunnableFuture<T> {
        private final RunnableFuture<T> result;

        public NotInterruptingRunnableFuture(RunnableFuture<T> result) {
            this.result = result;
        }

        @Override
        public void run() {
            result.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return result.cancel(false);
        }
        @Override
        public boolean isCancelled() {
            return result.isCancelled();
        }

        @Override
        public boolean isDone() {
            return result.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return result.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result.get(timeout, unit);
        }
    }
}
