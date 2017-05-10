package org.jtrim2.swing.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CleanupTask;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;

/**
 * Defines factory methods for executor implementations executing scheduled
 * tasks on the <I>AWT Event Dispatch Thread</I>.
 */
public final class SwingExecutors {
    private static final TaskExecutorService DEFAULT_INSTANCE
            = TaskExecutors.asUnstoppableExecutor(swingExecutorService(true));

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
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns an {@code UpdateTaskExecutor} implementation which executes tasks on the
     * <I>AWT Event Dispatch Thread</I>. The returned executor will not call the scheduled
     * tasks on the calling thread even if the calling thread is the
     * <I>AWT Event Dispatch Thread</I>.
     * <P>
     * This method is effectively the same as calling
     * {@link #newSwingUpdateExecutor(boolean) newSwingUpdateExecutor(true)}.
     *
     * @return an {@code UpdateTaskExecutor} implementation which executes tasks on the
     *   <I>AWT Event Dispatch Thread</I>. This method never returns {@code null}.
     */
    public static UpdateTaskExecutor newSwingUpdateExecutor() {
        return newSwingUpdateExecutor(true);
    }

    /**
     * Returns an {@code UpdateTaskExecutor} implementation which executes tasks on the
     * <I>AWT Event Dispatch Thread</I>.
     *
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread (this may not always possible to execute tasks in the
     *   order they were submitted).
     *
     * @return an {@code UpdateTaskExecutor} implementation which executes tasks on the
     *   <I>AWT Event Dispatch Thread</I>. This method never returns {@code null}.
     */
    public static UpdateTaskExecutor newSwingUpdateExecutor(boolean alwaysInvokeLater) {
        return new GenericUpdateTaskExecutor(getStrictExecutor(alwaysInvokeLater));
    }

    /**
     * Returns a {@code TaskExecutorService} which executes submitted tasks on the
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
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread (this may not always possible to execute tasks in the
     *   order they were submitted).
     * @return a {@code TaskExecutorService} which executes submitted tasks on the
     *   <I>AWT Event Dispatch Thread</I>. This method never returns {@code null}.
     *
     * @see #getDefaultInstance()
     * @see #getSimpleExecutor(boolean)
     * @see #getStrictExecutor(boolean)
     */
    public static TaskExecutorService swingExecutorService(boolean alwaysInvokeLater) {
        return new SwingTaskExecutor(alwaysInvokeLater);
    }

    /**
     * Returns a {@code TaskExecutor} which executes tasks submitted to
     * them on the <I>AWT Event Dispatch Thread</I>. The returned executor does
     * not necessarily executes tasks in the same order as the tasks were
     * submitted. In case tasks needed to be executed in the same order as they
     * were submitted to the executor: Use the
     * {@link #getStrictExecutor(boolean)} method.
     * <P>
     * The returned executor is more efficient than an instance returned by
     * {@link #swingExecutorService(boolean)}.
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
     * The returned executor is more efficient than an instance returned by
     * {@link #swingExecutorService(boolean)}.
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
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(task, "task");

            SwingUtilities.invokeLater(() -> {
                TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                executor.execute(cancelToken, task, cleanupTask);
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

            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(task, "task");

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
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(task, "task");

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

                SwingUtilities.invokeLater(() -> {
                    try {
                        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                        executor.execute(cancelToken, task, cleanupTask);
                    } finally {
                        currentlyExecuting.decrementAndGet();
                    }
                });
            }
        }
    }

    private SwingExecutors() {
        throw new AssertionError();
    }
}
