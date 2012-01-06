/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingTaskExecutor extends AbstractExecutorService {
    private static volatile ExecutorService defaultInstance
            = ExecutorsEx.asUnstoppableExecutor(new SwingTaskExecutor());

    public static ExecutorService getDefaultInstance() {
        return defaultInstance;
    }

    public static void setDefaultInstance(SwingTaskExecutor defaultInstance) {
        ExceptionHelper.checkNotNullArgument(defaultInstance, "defaultInstance");

        SwingTaskExecutor.defaultInstance = defaultInstance;
    }

    public static Executor getSimpleExecutor(boolean alwaysInvokeLater) {
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : EagerExecutor.INSTANCE;
    }

    public static Executor getStrictExecutor(boolean alwaysInvokeLater) {
        // We silently assume that SwingUtilities.invokeLater
        // invokes the tasks in the order they were scheduled.
        // This is not documented but it still seems safe to assume.
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : new StrictEagerExecutor();
    }

    private enum LazyExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            ExceptionHelper.checkNotNullArgument(command, "command");
            SwingUtilities.invokeLater(command);
        }
    }

    private enum EagerExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            ExceptionHelper.checkNotNullArgument(command, "command");

            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
            }
            else {
                SwingUtilities.invokeLater(command);
            }
        }
    }

    private static class StrictEagerExecutor implements Executor {
        // We assume that there are always less than Integer.MAX_VALUE
        // concurrent tasks.
        // Having more than this would surely make the application unusable
        // anyway (since these tasks run on the singe Event Dispatch Thread,
        // this would make it more outrageous).
        private final AtomicInteger currentlyExecuting = new AtomicInteger(0);

        @Override
        public void execute(final Runnable command) {
            ExceptionHelper.checkNotNullArgument(command, "command");

            boolean canInvokeNow = currentlyExecuting.get() == 0;
            // Tasks that are scheduled concurrently this call,
            // does not matter if they run after this task.
            // This executor only guarantees that A task happens before
            // B task, if scheduling A happens before scheduling B
            // (which implies that they does not run concurrently).

            if (canInvokeNow && SwingUtilities.isEventDispatchThread()) {
                command.run();
            }
            else {
                currentlyExecuting.incrementAndGet();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            command.run();
                        } finally {
                            currentlyExecuting.decrementAndGet();
                        }
                    }
                });
            }
        }
    }

    private final TaskListExecutorImpl impl;

    public SwingTaskExecutor() {
        this(true);
    }

    public SwingTaskExecutor(boolean alwaysInvokeLater) {
        this(alwaysInvokeLater, null);
    }

    public SwingTaskExecutor(boolean alwaysInvokeLater,
            TaskRefusePolicy taskRefusePolicy) {
        this.impl = new TaskListExecutorImpl(
                getStrictExecutor(alwaysInvokeLater), taskRefusePolicy);
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

    private static void checkWaitOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Waiting on the EDT would be a good way to cause a dead-lock.
            throw new IllegalStateException("Cannot wait for termination on the Event Dispatch Thread.");
        }
    }

    public void awaitTermination() throws InterruptedException {
        checkWaitOnEDT();
        impl.awaitTermination();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        checkWaitOnEDT();
        return impl.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        impl.execute(command);
    }
}