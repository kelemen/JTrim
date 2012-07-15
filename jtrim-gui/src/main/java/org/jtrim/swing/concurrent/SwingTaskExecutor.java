package org.jtrim.swing.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a {@code TaskExecutorService} which executes submitted tasks on the
 * <I>AWT Event Dispatch Thread</I>. Submitted tasks are executed in the order
 * they were submitted. Unlike {@code TaskExecutorService} implementations in
 * general, {@code SwingTaskExecutor} instances does not need to be shutted down
 * (although shutdown is still possible).
 * <P>
 * In case the services provided by the {@code TaskExecutor} service are
 * adequate, consider using the more efficient implementations returned by the
 * static {@link #getSimpleExecutor(boolean) getSimpleExecutor} or
 * {@link #getStrictExecutor(boolean) getStrictExecutor} methods.
 * <P>
 * A static instance can be retrieved by the
 * {@link #getDefaultInstance() getDefaultInstance} method. The instance
 * returned by this method can be used as a sensible default value.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safely accessible from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Method of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @see #getDefaultInstance()
 * @see #getSimpleExecutor(boolean)
 * @see #getStrictExecutor(boolean)
 *
 * @author Kelemen Attila
 */
public final class SwingTaskExecutor extends DelegatedTaskExecutorService {
    private static volatile TaskExecutorService defaultInstance
            = TaskExecutors.asUnstoppableExecutor(new SwingTaskExecutor());

    /**
     * Returns a {@code TaskExecutorService} which executes tasks submitted to
     * them on the <I>AWT Event Dispatch Thread</I>. This method always returns
     * the same {@code TaskExecutorService} instance and is intended to be used
     * as a sensible default value when an executor is need which executes
     * task on the EDT. The returned {@code TaskExecutorService} cannot be
     * shutted down, attempting to do so will cause an unchecked exception to be
     * thrown.
     *
     * @return a {@code TaskExecutorService} which executes tasks submitted to
     *   them on the <I>AWT Event Dispatch Thread</I>. This method never returns
     *   {@code null}.
     */
    public static TaskExecutorService getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Returns a {@code TaskExecutor} which executes tasks submitted to
     * them on the <I>AWT Event Dispatch Thread</I>. The returned executor does
     * not necessarily executes tasks in the same order as the tasks were
     * submitted. In case tasks needed to be executed in the same order as they
     * were submitted to the executor: Use the
     * {@link #getStrictExecutor(boolean)} method.
     * <P>
     * The returned executor is more efficient than an instance of
     * {@code SwingTaskExecutor}.
     *
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread.
     * @return a {@code TaskExecutor} which executes tasks submitted to
     *   them on the <I>AWT Event Dispatch Thread</I>. This method never returns
     *   {@code null}.
     *
     * @see #getStrictExecutor(boolean)
     */
    public static TaskExecutor getSimpleExecutor(boolean alwaysInvokeLater) {
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : EagerExecutor.INSTANCE;
    }

    /**
     * Returns a {@code TaskExecutor} which executes tasks submitted to
     * them on the <I>AWT Event Dispatch Thread</I>. The returned executor
     * executes tasks in the same order as the tasks were submitted. If you
     * don't need to execute them in the same order, consider using the
     * {@link #getSimpleExecutor(boolean)} method.
     * <P>
     * The returned executor is more efficient than an instance of
     * {@code SwingTaskExecutor}.
     *
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread (this may not always possible to execute tasks in the
     *   order they were submitted).
     * @return a {@code TaskExecutor} which executes tasks submitted to
     *   them on the <I>AWT Event Dispatch Thread</I>. This method never returns
     *   {@code null}.
     *
     * @see #getSimpleExecutor(boolean)
     */
    public static TaskExecutor getStrictExecutor(boolean alwaysInvokeLater) {
        // We silently assume that SwingUtilities.invokeLater
        // invokes the tasks in the order they were scheduled.
        // This is not documented but it still seems safe to assume.
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : new StrictEagerExecutor();
    }

    private enum LazyExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                    executor.execute(cancelToken, task, cleanupTask);
                }
            });
        }
    }

    private enum EagerExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {

            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            if (SwingUtilities.isEventDispatchThread()) {
                TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                executor.execute(cancelToken, task, cleanupTask);
            }
            else {
                LazyExecutor.INSTANCE.execute(cancelToken, task, cleanupTask);
            }
        }
    }

    private static class StrictEagerExecutor implements TaskExecutor {
        // We assume that there are always less than Integer.MAX_VALUE
        // concurrent tasks.
        // Having more than this would surely make the application unusable
        // anyway (since these tasks run on the single Event Dispatch Thread,
        // this would make it more outrageous).
        private final AtomicInteger currentlyExecuting = new AtomicInteger(0);

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            boolean canInvokeNow = currentlyExecuting.get() == 0;
            // Tasks that are scheduled concurrently this call,
            // does not matter if they run after this task.
            // This executor only guarantees that A task happens before
            // B task, if scheduling A happens before scheduling B
            // (which implies that they does not run concurrently).

            if (canInvokeNow && SwingUtilities.isEventDispatchThread()) {
                TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                executor.execute(cancelToken, task, cleanupTask);
            }
            else {
                currentlyExecuting.incrementAndGet();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                            executor.execute(cancelToken, task, cleanupTask);
                        } finally {
                            currentlyExecuting.decrementAndGet();
                        }
                    }
                });
            }
        }
    }

    /**
     * Creates a new {@code SwingTaskExecutor} which will execute submitted
     * tasks on the <I>AWT Event Dispatch Thread</I>.
     * <P>
     * This constructor is equivalent to calling
     * {@code SwingTaskExecutor(true)}.
     */
    public SwingTaskExecutor() {
        this(true);
    }

    /**
     * Creates a new {@code SwingTaskExecutor} which will execute submitted
     * tasks on the <I>AWT Event Dispatch Thread</I>.
     *
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread.
     */
    public SwingTaskExecutor(boolean alwaysInvokeLater) {
        super(TaskExecutors.upgradeExecutor(getStrictExecutor(alwaysInvokeLater)));
    }

    private static void checkWaitOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Waiting on the EDT would be a good way to cause a dead-lock.
            throw new IllegalStateException("Cannot wait for termination on the Event Dispatch Thread.");
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * This method may not be called from the <I>AWT Event Dispatch Thread</I>.
     * This is forbidden because it is very dead-lock prone: If a task was
     * submitted to this executor, it could not complete because this method
     * prevents any task to be executed on the EDT.
     *
     * @throws IllegalStateException thrown if the current calling thread is the
     *   <I>AWT Event Dispatch Thread</I>
     */
    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        checkWaitOnEDT();
        wrappedExecutor.awaitTermination(cancelToken);
    }

    /**
     * {@inheritDoc }
     * <P>
     * This method may not be called from the <I>AWT Event Dispatch Thread</I>.
     * This is forbidden because it is very dead-lock prone: If a task was
     * submitted to this executor, it could not complete because this method
     * prevents any task to be executed on the EDT.
     *
     * @throws IllegalStateException thrown if the current calling thread is the
     *   <I>AWT Event Dispatch Thread</I>
     */
    @Override
    public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        checkWaitOnEDT();
        return wrappedExecutor.awaitTermination(cancelToken, timeout, unit);
    }
}