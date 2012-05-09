package org.jtrim.concurrent;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an executor which executes tasks synchronously on the calling thread
 * which submits them (by the {@code execute} or one of the {@code submit}
 * methods). Therefore whenever such {@code execute} or {@code submit} method
 * returns, the submitted task is already executed or never will be (due to the
 * executor being shutted down). Note however, that the cleanup task will always
 * be executed.
 * <P>
 * There are two special instances which can be accessed through static
 * methods:
 * <ul>
 *  <li>
 *   {@link #getSimpleExecutor() getSimpleExecutor()} for a simple and efficient
 *   {@code TaskExecutor} instance which executes tasks synchronously on the
 *   calling thread.
 *  </li>
 *  <li>
 *   {@link #getDefaultInstance() getDefaultInstance()} to use as sensible
 *   default values when an {@code TaskExecutorService} instance is needed.
 *  </li>
 * </ul>
 * Note that unlike general {@code TaskExecutorService} instances, instances of
 * this class does not need to be shutted down (but it is possible to do so).
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class SyncTaskExecutor extends DelegatedTaskExecutorService {
    private static final TaskExecutorService DEFAULT_INSTANCE
            = TaskExecutors.asUnstoppableExecutor(new SyncTaskExecutor());

    /**
     * Returns a plain and efficient {@code TaskExecutor} which executes tasks
     * synchronously on the calling thread. This method always returns the same
     * executor instance and is considerably more efficient than a full-fledged
     * {@link SyncTaskExecutor} implementation (or the one returned by
     * {@link #getDefaultInstance() getDefaultInstance()}.
     *
     * @return a plain and efficient {@code TaskExecutor} which executes tasks
     *   synchronously on the calling thread. This method never returns
     *   {@code null}.
     */
    public static TaskExecutor getSimpleExecutor() {
        return SimpleTaskExecutor.INSTANCE;
    }

    /**
     * Returns an {@code TaskExecutorService} which executes tasks synchronously
     * on the calling thread and cannot be shutted down. Attempting to shutdown
     * the returned {@code TaskExecutorService} will result in an unchecked
     * {@link UnsupportedOperationException} to be thrown.
     * <P>
     * This method always returns the same {@code TaskExecutorService} instance
     * and note that sharing the return value across multiple threads can cause
     * some synchronization overhead. So it is more efficient to create a new
     * instance when this is an issue. Therefore this
     * {@code TaskExecutorService} instance is only intended to be used as a
     * sensible default value.
     *
     * @return an {@code TaskExecutorService} which executes tasks synchronously
     *   on the calling thread and cannot be shutted down. This method never
     *   returns {@code null} and always returns the same instance.
     */
    public static TaskExecutorService getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Creates a new executor which executes tasks synchronously on the calling
     * thread.
     */
    public SyncTaskExecutor() {
        super(new UpgradedTaskExecutor(getSimpleExecutor()));
    }

    private enum SimpleTaskExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            Throwable error = null;
            boolean canceled = true;
            try {
                task.execute(cancelToken);
                canceled = false;
            } catch (OperationCanceledException ex) {
                error = ex;
            } catch (Throwable ex) {
                canceled = false;
                error = ex;
            } finally {
                if (cleanupTask != null) {
                    try {
                        cleanupTask.cleanup(canceled, error);
                    } catch (Throwable ex) {
                        // Note that this should not happen because
                        // UpgradedTaskExecutor protects us from exceptions.
                        ExceptionHelper.rethrow(ex);
                    }
                }
            }
        }
    }
}
