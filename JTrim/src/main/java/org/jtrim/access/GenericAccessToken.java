package org.jtrim.access;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.ExecutorShutdownListener;
import org.jtrim.concurrent.TaskListExecutorImpl;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AccessTokens#createToken(java.lang.Object, java.util.concurrent.Executor)
 * @author Kelemen Attila
 */
final class GenericAccessToken<IDType> extends AbstractAccessToken<IDType> {
    private final IDType accessID;
    private final TaskListExecutorImpl impl;

    public GenericAccessToken(IDType accessID, Executor backingExecutor) {
        ExceptionHelper.checkNotNullArgument(accessID, "accessID");
        ExceptionHelper.checkNotNullArgument(backingExecutor, "backingExecutor");

        this.accessID = accessID;

        ExecutorShutdownListener listener;
        listener = new ExecutorShutdownListener() {
            @Override
            public void onTerminate() {
                GenericAccessToken.this.onTerminate();
            }
        };

        this.impl = new TaskListExecutorImpl(backingExecutor, null, listener);
    }

    @Override
    public IDType getAccessID() {
        return accessID;
    }

    @Override
    public void shutdown() {
        impl.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return impl.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return impl.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return impl.isTerminated();
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        impl.awaitTermination();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        return impl.awaitTermination(timeout, unit);
    }

    @Override
    public boolean executeNow(Runnable task) {
        return impl.executeNow(task);
    }

    @Override
    public void execute(Runnable command) {
        impl.execute(command);
    }

    @Override
    public String toString() {
        return "GenericAccessToken{" + accessID + '}';
    }
}
