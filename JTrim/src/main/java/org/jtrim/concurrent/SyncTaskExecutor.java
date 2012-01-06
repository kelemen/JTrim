/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author Kelemen Attila
 */
public final class SyncTaskExecutor extends AbstractExecutorService {

    private static final ExecutorService defaultInstance
            = ExecutorsEx.asUnstoppableExecutor(new SyncTaskExecutor(SilentTaskRefusePolicy.INSTANCE));

    public static Executor getSimpleExecutor() {
        return SimpleExecutor.INSTANCE;
    }

    public static ExecutorService getDefaultInstance() {
        return defaultInstance;
    }

    private final TaskListExecutorImpl impl;
    private final TaskRefusePolicy taskRefusePolicy;

    public SyncTaskExecutor(TaskRefusePolicy taskRefusePolicy) {
        this(taskRefusePolicy, null);
    }

    public SyncTaskExecutor(
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {
        this.taskRefusePolicy = taskRefusePolicy;
        this.impl = new TaskListExecutorImpl(
                SyncTaskExecutor.getSimpleExecutor(),
                taskRefusePolicy,
                shutdownListener);
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return impl.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if (!impl.executeNow(command)) {
            taskRefusePolicy.refuseTask(command);
        }
    }

    private enum SimpleExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
