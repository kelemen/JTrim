package org.jtrim.access;

import java.util.*;
import java.util.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken)
 * @author Kelemen Attila
 */
final class CombinedToken<IDType1, IDType2>
extends
        AbstractAccessToken<MultiAccessID<IDType1, IDType2>> {

    private final MultiAccessID<IDType1, IDType2> accessID;
    private final Executor executor;
    private final AccessToken<IDType1> token1;
    private final AccessToken<IDType2> token2;
    private volatile Executor internalExecutor;

    public CombinedToken(Executor executor,
            AccessToken<IDType1> token1, AccessToken<IDType2> token2) {

        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(token1, "token1");
        ExceptionHelper.checkNotNullArgument(token2, "token2");

        this.accessID = new MultiAccessID<>(
                token1.getAccessID(), token2.getAccessID());

        this.executor = executor;
        this.token1 = token1;
        this.token2 = token2;

        if (token1 == executor && token2 == executor) {
            this.internalExecutor = executor;
        }
        else if (token1 == executor) {
            this.internalExecutor = new UnprotectorExecutor(new DoubleExecutor(token2, token1));
        }
        else if (token1 == executor) {
            this.internalExecutor = new UnprotectorExecutor(new DoubleExecutor(token1, token2));
        }
        else {
            this.internalExecutor = new UnprotectorExecutor(new SafeGenericExecutor());
        }
    }

    @Override
    public MultiAccessID<IDType1, IDType2> getAccessID() {
        return accessID;
    }

    @Override
    public void shutdown() {
        token1.shutdown();
        token2.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> result1 = token1.shutdownNow();
        List<Runnable> result2 = token2.shutdownNow();

        List<Runnable> result = new ArrayList<>(result1.size() + result2.size());
        result.addAll(result1);
        result.addAll(result2);

        return result;
    }

    @Override
    public boolean isShutdown() {
        return token1.isShutdown() || token2.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return token1.isTerminated() && token2.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.nanoTime();
        long nanosToWait = unit.toNanos(timeout);

        token1.awaitTermination(timeout, unit);

        long toWaitNanos = nanosToWait - (System.nanoTime() - startTime);
        if (toWaitNanos > 0) {
            token2.awaitTermination(toWaitNanos, TimeUnit.NANOSECONDS);
        }

        return isTerminated();
    }

    @Override
    public void execute(final Runnable command) {
        ExceptionHelper.checkNotNullArgument(command, "command");
        internalExecutor.execute(command);
    }

    private class UnprotectorExecutor implements Executor {
        private final Executor subExecutor;

        public UnprotectorExecutor(Executor subExecutor) {
            this.subExecutor = subExecutor;
        }

        @Override
        public void execute(final Runnable command) {
            subExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    internalExecutor = new UnsafeGenericExecutor();
                    command.run();
                }
            });
        }
    }

    private class UnsafeGenericExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            executor.execute(new ExecuteNowForwarder(command));
        }
    }

    private class SafeGenericExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            Runnable executeNowTask = new ExecuteNowForwarder(command);
            Runnable executorTask = new ToExecutorForwarder(executeNowTask, executor);
            Runnable token2Task = new ToExecutorForwarder(executorTask, token2);
            token1.execute(token2Task);
        }
    }

    private static class DoubleExecutor implements Executor {
        private final AccessToken<?> firstToken;
        private final AccessToken<?> executorToken;

        public DoubleExecutor(AccessToken<?> firstToken, AccessToken<?> executorToken) {
            this.firstToken = firstToken;
            this.executorToken = executorToken;
        }

        @Override
        public void execute(Runnable command) {
            Runnable finalTask = new ToTokenNowTaskForwarder(command, firstToken);
            Runnable executorTask = new ToExecutorForwarder(finalTask, executorToken);
            firstToken.execute(executorTask);
        }
    }

    @Override
    public boolean executeNow(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        Object result = token1.executeNow(new ToTokenNowForwarder(task, token2));
        return result != null;
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        token1.awaitTermination();
        token2.awaitTermination();
    }

    private static class ToExecutorForwarder implements Runnable {
        private final Runnable task;
        private final Executor executor;

        public ToExecutorForwarder(Runnable task, Executor executor) {
            this.task = task;
            this.executor = executor;
        }

        @Override
        public void run() {
            executor.execute(task);
        }
    }

    private static class ToTokenNowTaskForwarder implements Runnable {
        private final Runnable task;
        private final AccessToken<?> token;

        public ToTokenNowTaskForwarder(Runnable task, AccessToken<?> token) {
            this.task = task;
            this.token = token;
        }

        @Override
        public void run() {
            token.executeNow(task);
        }
    }

    private static class ToTokenNowForwarder implements Callable<Object> {
        private final Runnable task;
        private final AccessToken<?> token;

        public ToTokenNowForwarder(Runnable task, AccessToken<?> token) {
            this.task = task;
            this.token = token;
        }

        @Override
        public Object call() throws Exception {
            return token.executeNow(task) ? task : null;
        }
    }

    private class ExecuteNowForwarder implements Runnable {
        private final Runnable command;

        public ExecuteNowForwarder(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            executeNow(command);
        }
    }
}
