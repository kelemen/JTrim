/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jtrim.utils.*;

/**
 *
 * @author Kelemen Attila
 */
public final class MultiPhaseTask<ResultType> {
    private final Future<ResultType> future;

    private final AtomicReference<FinishResult<ResultType>> finishResult;
    private final ExecutorService syncExecutor;

    public static interface TerminateListener<ResultType> {
        public void onTerminate(ResultType result, Throwable exception,
            boolean canceled);
    }

    public MultiPhaseTask(
            final TerminateListener<? super ResultType> terminateListener) {

        this.finishResult = new AtomicReference<>(null);
        this.future = new MultiPhaseFuture();

        TaskRefusePolicy refusePolicy = SilentTaskRefusePolicy.INSTANCE;
        if (terminateListener != null) {
            this.syncExecutor = new SyncTaskExecutor(refusePolicy,
                    new TerminateEventForwarder(terminateListener));
        }
        else {
            this.syncExecutor = new SyncTaskExecutor(refusePolicy);
        }
    }

    public Future<ResultType> getFuture() {
        return future;
    }

    public void executeSubTask(final UpdateTaskExecutor executor, final Runnable task) {
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                executor.execute(new SubTaskExecutor(task));
            }
        });
    }

    public void executeSubTask(final Executor executor, final Runnable task) {
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                executor.execute(new SubTaskExecutor(task));
            }
        });
    }

    public Future<?> submitSubTask(final ExecutorService executor,
            final Runnable task) {

        final ObjectRef<Future<?>> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executor.submit(new SubTaskExecutor(task)));
            }
        });

        return result.getValue();
    }

    public <V> Future<V> submitSubTask(final ExecutorService executor,
            final Callable<V> task) {
        final ObjectRef<Future<V>> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executor.submit(new SubCallableExecutor<>(task)));
            }
        });

        return result.getValue();
    }

    public void executeSubTask(final Runnable task) {
        executeSubTask(new RunnableWrapper(task));
    }

    public <V> V executeSubTask(final Callable<V> task) {
        final ObjectRef<V> result = new ObjectRef<>(null);
        syncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.setValue(executeSubTaskAlways(task));
            }
        });

        return result.getValue();
    }

    // This method must be called in the context of the "syncExecutor"
    private <V> V executeSubTaskAlways(Callable<V> task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        try {
            return task != null ? task.call() : null;
        } catch (InterruptedException ex) {
            finishTask(null, ex, false);
            Thread.currentThread().interrupt();
            return null;
        } catch (Throwable ex) {
            finishTask(null, ex, false);
            return null;
        }
    }

    public boolean finishTask(
            ResultType result,
            Throwable exception,
            boolean canceled) {

        final FinishResult<ResultType> completeResult
                = new FinishResult<>(result, exception, canceled);

        if (!finishResult.compareAndSet(null, completeResult)) {
            return false;
        }
        syncExecutor.shutdown();

        return syncExecutor.isTerminated();
    }

    public void cancel() {
        future.cancel(true);
    }

    public boolean isDone() {
        return future.isDone();
    }

    private class MultiPhaseFuture implements Future<ResultType> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (isDone()) {
                return isCancelled();
            }

            return finishTask(null, null, true);
        }

        @Override
        public boolean isCancelled() {
            FinishResult<?> result = finishResult.get();

            return result != null && result.isCanceled();
        }

        @Override
        public boolean isDone() {
            return syncExecutor.isTerminated();
        }

        public ResultType fetchResult() throws InterruptedException, ExecutionException {
            FinishResult<ResultType> result = finishResult.get();

            Throwable asyncException = result.getException();
            if (asyncException != null) {
                throw new ExecutionException(asyncException);
            }

            if (result.isCanceled()) {
                throw new CancellationException();
            }

            return result.getResult();
        }

        @Override
        public ResultType get() throws InterruptedException, ExecutionException {
            ExecutorsEx.awaitExecutor(syncExecutor);
            return fetchResult();
        }

        @Override
        public ResultType get(long timeout, TimeUnit unit) throws
                InterruptedException, ExecutionException, TimeoutException {

            syncExecutor.awaitTermination(timeout, unit);
            return fetchResult();
        }
    }

    private static class RunnableWrapper implements Callable<Object> {
        private final Runnable task;

        public RunnableWrapper(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public Object call() {
            task.run();
            return null;
        }
    }

    private class SubTaskExecutor implements Runnable {
        private final Runnable task;

        public SubTaskExecutor(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public void run() {
            executeSubTask(new RunnableWrapper(task));
        }
    }

    private class SubCallableExecutor<V> implements Callable<V> {
        private final Callable<V> task;

        public SubCallableExecutor(Callable<V> task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public V call() {
            return executeSubTask(task);
        }
    }

    private static class FinishResult<ResultType> {
        private final ResultType result;
        private final Throwable exception;
        private final boolean canceled;

        public FinishResult(ResultType result, Throwable exception, boolean canceled) {
            this.result = result;
            this.exception = exception;
            this.canceled = canceled;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public Throwable getException() {
            return exception;
        }

        public ResultType getResult() {
            return result;
        }
    }

    private static class ObjectRef<T> {
        private T value;

        public ObjectRef(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    private class TerminateEventForwarder implements ExecutorShutdownListener {
        private final TerminateListener<? super ResultType> terminateListener;

        public TerminateEventForwarder(TerminateListener<? super ResultType> terminateListener) {
            this.terminateListener = terminateListener;
        }

        @Override
        public void onTerminate() {
            FinishResult<ResultType> result = finishResult.get();
            terminateListener.onTerminate(
                    result.getResult(),
                    result.getException(),
                    result.isCanceled());
        }
    }
}
