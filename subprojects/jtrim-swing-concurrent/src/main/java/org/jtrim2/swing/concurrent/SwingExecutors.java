package org.jtrim2.swing.concurrent;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.jtrim2.access.AccessManager;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.AbstractTaskExecutor;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.ui.concurrent.BackgroundTaskExecutor;
import org.jtrim2.ui.concurrent.UiExecutorProvider;

/**
 * Defines factory methods for executor implementations executing scheduled
 * tasks on the <I>AWT Event Dispatch Thread</I>.
 */
public final class SwingExecutors {
    private static final TaskExecutorService DEFAULT_INSTANCE
            = TaskExecutors.asUnstoppableExecutor(getSwingExecutorService(true));

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
     * {@link #getSwingUpdateExecutor(boolean) getSwingUpdateExecutor(true)}.
     *
     * @return an {@code UpdateTaskExecutor} implementation which executes tasks on the
     *   <I>AWT Event Dispatch Thread</I>. This method never returns {@code null}.
     */
    public static UpdateTaskExecutor getSwingUpdateExecutor() {
        return getSwingUpdateExecutor(true);
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
    public static UpdateTaskExecutor getSwingUpdateExecutor(boolean alwaysInvokeLater) {
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
    public static TaskExecutorService getSwingExecutorService(boolean alwaysInvokeLater) {
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
     * {@link #getSwingExecutorService(boolean)}.
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
     * {@link #getSwingExecutorService(boolean)}.
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

    /**
     * Returns an implementation of {@code UiExecutorProvider} executing
     * scheduled tasks on the <I>AWT Event Dispatch Thread</I>.
     *
     * @return an implementation of {@code UiExecutorProvider} executing
     *   scheduled tasks on the <I>AWT Event Dispatch Thread</I>. This
     *   method never returns {@code null}.
     */
    public static UiExecutorProvider getSwingExecutorProvider() {
        return SwingUiExecutorProvider.INSTANCE;
    }

    /**
     * Creates a new {@code BackgroundTaskExecutor} to be used with <I>Swing</I> with the given access
     * manager and {@code TaskExecutor}.
     * <P>
     * The specified {@code TaskExecutor} is recommended to execute tasks on a
     * separate thread instead of the calling thread, however for debugging
     * purposes it may be beneficial to use the {@code SyncTaskExecutor}. The
     * executor should execute tasks on a separate thread to allow methods of
     * this class to be called from the <I>AWT Event Dispatch Thread</I> without
     * actually blocking the EDT.
     *
     * @param <IDType> the type of the request ID of the underlying access manager
     * @param <RightType> the type of the rights handled by the underlying access
     *   manager
     * @param accessManager the {@code AccessManager} from which access tokens
     *   are requested to execute tasks in their context. This argument cannot
     *   be {@code null}.
     * @param executor the {@code TaskExecutor} which actually executes
     *   submitted tasks. This argument cannot be {@code null}.
     * @return a new {@code BackgroundTaskExecutor} to be used with <I>Swing</I> with the given access
     *   manager and {@code TaskExecutor}. This method never returns {@code null}.
     */
    public static <IDType, RightType> BackgroundTaskExecutor<IDType, RightType> getSwingBackgroundTaskExecutor(
            AccessManager<IDType, RightType> accessManager,
            TaskExecutor executor) {
        return new BackgroundTaskExecutor<>(accessManager, executor, getSwingExecutorProvider());
    }

    private static final class LazyExecutor extends AbstractTaskExecutor {
        private static final TaskExecutor INSTANCE = new LazyExecutor();

        @Override
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            SwingUtilities.invokeLater(() -> {
                submittedTask.execute(cancelToken);
            });
        }
    }

    private enum EagerExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {

            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(function, "function");

            if (SwingUtilities.isEventDispatchThread()) {
                return SyncTaskExecutor.getSimpleExecutor().executeFunction(cancelToken, function);
            }
            else {
                return LazyExecutor.INSTANCE.executeFunction(cancelToken, function);
            }
        }
    }

    private static final class StrictEagerExecutor extends AbstractTaskExecutor {
        // We assume that there are always less than Integer.MAX_VALUE
        // concurrent tasks.
        // Having more than this would surely make the application unusable
        // anyway (since these tasks run on the single Event Dispatch Thread,
        // this would make it more outrageous).
        private final AtomicInteger currentlyExecuting = new AtomicInteger(0);

        @Override
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            boolean canInvokeNow = currentlyExecuting.get() == 0;
            // Tasks that are scheduled concurrently this call,
            // does not matter if they run after this task.
            // This executor only guarantees that A task happens before
            // B task, if scheduling A happens before scheduling B
            // (which implies that they does not run concurrently).

            if (canInvokeNow && SwingUtilities.isEventDispatchThread()) {
                submittedTask.execute(cancelToken);
            }
            else {
                currentlyExecuting.incrementAndGet();

                SwingUtilities.invokeLater(() -> {
                    try {
                        submittedTask.execute(cancelToken);
                    } finally {
                        currentlyExecuting.decrementAndGet();
                    }
                });
            }
        }
    }

    private enum SwingUiExecutorProvider implements UiExecutorProvider {
        INSTANCE;

        @Override
        public TaskExecutor getSimpleExecutor(boolean alwaysExecuteLater) {
            return SwingExecutors.getSimpleExecutor(alwaysExecuteLater);
        }

        @Override
        public TaskExecutor getStrictExecutor(boolean alwaysExecuteLater) {
            return SwingExecutors.getStrictExecutor(alwaysExecuteLater);
        }

    }

    private SwingExecutors() {
        throw new AssertionError();
    }
}
