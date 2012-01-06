/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.List;
import java.util.concurrent.*;

/**
 *
 * @author Kelemen Attila
 */
public class UpgraderExecutor extends AbstractExecutorService {
    private final TaskListExecutorImpl impl;

    public UpgraderExecutor(Executor backingExecutor) {
        this(backingExecutor, null, null);
    }

    public UpgraderExecutor(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy) {

        this(backingExecutor, taskRefusePolicy, null);
    }

    public UpgraderExecutor(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {

        this.impl = new TaskListExecutorImpl(
                backingExecutor, taskRefusePolicy, shutdownListener);
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
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        return impl.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        impl.execute(command);
    }

    @Override
    public String toString() {
        return "UpgradedExecutor{" + impl.getBackingExecutor() + '}';
    }
}
