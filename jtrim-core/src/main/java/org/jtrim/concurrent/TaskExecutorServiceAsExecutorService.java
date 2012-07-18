package org.jtrim.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.InterruptibleTask;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see ExecutorConverter#asExecutorService(TaskExecutorService)
 *
 * @author Kelemen Attila
 */
final class TaskExecutorServiceAsExecutorService extends AbstractExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final TaskExecutorService executor;
    private final boolean mayInterruptTasks;

    public TaskExecutorServiceAsExecutorService(TaskExecutorService executor, boolean mayInterruptTasks) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
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
        return executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    private void executeUninterruptibly(final Runnable command) {
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                command.run();
            }
        }, null);
    }

    private void executeInterruptibly(final Runnable command) {
        if (executor.isShutdown()) {
            return;
        }

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                Cancellation.doAsCancelable(cancelToken, new InterruptibleTask<Void>() {
                    @Override
                    public Void execute(CancellationToken cancelToken) {
                        command.run();
                        return null;
                    }
                });
            }
        }, null);
    }

    @Override
    public void execute(final Runnable command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        if (mayInterruptTasks) {
            executeInterruptibly(command);
        }
        else {
            executeUninterruptibly(command);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        final RunnableFuture<T> result = super.newTaskFor(runnable, value);
        if (mayInterruptTasks) {
            return result;
        }
        else {
            return new NotInterruptingRunnableFuture<>(result);
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        final RunnableFuture<T> result = super.newTaskFor(callable);
        if (mayInterruptTasks) {
            return result;
        }
        else {
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
