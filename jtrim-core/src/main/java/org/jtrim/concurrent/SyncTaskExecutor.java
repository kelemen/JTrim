package org.jtrim.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an executor which executes tasks synchronously on the calling thread
 * which submits them (by the {@code execute} or one of the {@code submit}
 * methods). Therefore whenever such {@code execute} or {@code submit} method
 * returns, the submitted task is already executed or never will be (because it
 * was refused).
 * <P>
 * There are two special instances which can be accessed through static
 * methods:
 * <ul>
 *  <li>
 *   {@link #getSimpleExecutor() getSimpleExecutor()} for a simple and efficient
 *   {@code Executor} instance which executes tasks synchronously on the calling
 *   thread.
 *  </li>
 *  <li>
 *   {@link #getDefaultInstance() getDefaultInstance()} to use as sensible
 *   default values when an {@code ExecutorService} instance is needed.
 *  </li>
 * </ul>
 * Note that unlike general {@code ExecutorService} instances, instances of
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
public final class SyncTaskExecutor extends AbstractExecutorService {
    private static final ExecutorService defaultInstance
            = ExecutorsEx.asUnstoppableExecutor(new SyncTaskExecutor(SilentTaskRefusePolicy.INSTANCE));

    /**
     * Returns a plain and efficient {@code Executor} which executes tasks
     * synchronously on the calling thread. This method always returns the same
     * executor instance and is considerably more efficient than a full-fledged
     * {@link SyncTaskExecutor} implementation (or the one returned by
     * {@link #getDefaultInstance() getDefaultInstance()}.
     *
     * @return a plain and efficient {@code Executor} which executes tasks
     *   synchronously on the calling thread. This method never returns
     *   {@code null}.
     */
    public static Executor getSimpleExecutor() {
        return SimpleExecutor.INSTANCE;
    }

    /**
     * Returns an {@code ExecutorService} which executes tasks synchronously on
     * the calling thread and cannot be shutted down. Attempting to shutdown the
     * returned {@code ExecutorService} will result in an unchecked
     * {@link UnsupportedOperationException} to be thrown.
     * <P>
     * This method always returns the same {@code ExecutorService} instance and
     * note that sharing the return value across multiple threads can cause
     * some synchronization overhead. So it is more efficient to create a new
     * instance when this is an issue. Therefore this {@code ExecutorService}
     * instance is only intended to be used a sensible default value.
     *
     * @return an {@code ExecutorService} which executes tasks synchronously on
     *   the calling thread and cannot be shutted down. This method never
     *   returns {@code null} and always returns the same instance.
     */
    public static ExecutorService getDefaultInstance() {
        return defaultInstance;
    }

    private final TaskListExecutorImpl impl;
    private final TaskRefusePolicy taskRefusePolicy;

    /**
     * Creates a new executor which executes tasks synchronously on the calling
     * thread with the given policy what to do when a task cannot be executed.
     * <P>
     * The only reason why tasks cannot be executed by this executor is because
     * this executor was shutted down.
     *
     * @param taskRefusePolicy the policy what to do when a task was refused to
     *   be executed. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if {@code taskRefusePolicy} is
     *   {@code null}
     */
    public SyncTaskExecutor(TaskRefusePolicy taskRefusePolicy) {
        this(taskRefusePolicy, null);
    }

    /**
     * Creates a new executor which executes tasks synchronously on the calling
     * thread with the given policy what to do when a task cannot be executed
     * and listener which is notified when this executor terminates.
     * <P>
     * The only reason why tasks cannot be executed by this executor is because
     * this executor was shutted down.
     * <P>
     * Note that when the given listener is notified no more tasks will be ever
     * executed by this executor (not even concurrently with the notification).
     *
     * @param taskRefusePolicy the policy what to do when a task was refused to
     *   be executed. This argument cannot be {@code null}.
     * @param shutdownListener the listener to be notified when this executor
     *   terminates. This argument can be {@code null} if no such notification
     *   is required.
     *
     * @throws NullPointerException thrown if {@code taskRefusePolicy} is
     *   {@code null}
     */
    public SyncTaskExecutor(
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {
        ExceptionHelper.checkNotNullArgument(taskRefusePolicy, "taskRefusePolicy");

        this.taskRefusePolicy = taskRefusePolicy;
        this.impl = new TaskListExecutorImpl(
                SyncTaskExecutor.getSimpleExecutor(),
                taskRefusePolicy,
                shutdownListener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        impl.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Runnable> shutdownNow() {
        return impl.shutdownNow();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isShutdown() {
        return impl.isShutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return impl.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return impl.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
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
