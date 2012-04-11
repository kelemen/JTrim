package org.jtrim.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link ExecutorService} which upgrades a basic
 * {@link Executor Executor} to provide all the features of an
 * {@code ExecutorService}.
 * <P>
 * Note that this implementation keeps a list of net yet executed tasks and
 * canceling a task does not immediately removes it from this list.
 * <P>
 * The following addition features are provided by this {@code ExecutorService}:
 * <ul>
 *  <li>
 *   A user defined policy can be specified what to do when a task cannot
 *   be executed due to the {@code UpgraderExecutor} had been shutted down.
 *  </li>
 *  <li>
 *   A listener can be specified which will be notified when the
 *   {@code UpgraderExecutor} has been terminated and will no longer execute
 *   any task scheduled to it (even previously scheduled tasks).
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>.
 *
 * @see TaskListExecutorImpl
 * @author Kelemen Attila
 */
public class UpgraderExecutor extends AbstractExecutorService {
    private final TaskListExecutorImpl impl;

    /**
     * Creates a new {@code UpgraderExecutor} backed by the specified
     * {@code Executor}. The new {@code UpgraderExecutor} will simply discard
     * tasks silently if it had been shutted down.
     *
     * @param backingExecutor the {@code Executor} which will actually execute
     *   tasks scheduled to the new {@code UpgraderExecutor}. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public UpgraderExecutor(Executor backingExecutor) {
        this(backingExecutor, null, null);
    }

    /**
     * Creates a new {@code UpgraderExecutor} backed by the specified
     * {@code Executor} with the given policy what to do when a task is refused
     * to be submitted because the {@code UpgraderExecutor} has terminated.
     *
     * @param backingExecutor the {@code Executor} which will actually execute
     *   tasks scheduled to the new {@code UpgraderExecutor}. This argument
     *   cannot be {@code null}.
     * @param taskRefusePolicy the policy what to do when a task is refused to
     *   be submitted because the {@code UpgraderExecutor} has terminated. This
     *   argument can be {@code null} and in this case refused tasks will be
     *   discarded silently.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public UpgraderExecutor(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy) {

        this(backingExecutor, taskRefusePolicy, null);
    }

    /**
     * Creates a new {@code UpgraderExecutor} backed by the specified
     * {@code Executor} with the given policy what to do when a task is refused
     * to be submitted because the {@code UpgraderExecutor} has terminated and a
     * listener which will be notified when the new {@code UpgraderExecutor} has
     * terminated.
     *
     * @param backingExecutor the {@code Executor} which will actually execute
     *   tasks scheduled to the new {@code UpgraderExecutor}. This argument
     *   cannot be {@code null}.
     * @param taskRefusePolicy the policy what to do when a task is refused to
     *   be submitted because the {@code UpgraderExecutor} has terminated. This
     *   argument can be {@code null} and in this case refused tasks will be
     *   discarded silently.
     * @param shutdownListener the listener to be notified when the new
     *   {@code UpgraderExecutor} has terminated. This argument can be
     *   {@code null} if no such notification is required.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public UpgraderExecutor(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {
        ExceptionHelper.checkNotNullArgument(backingExecutor, "backingExecutor");

        this.impl = new TaskListExecutorImpl(
                backingExecutor, taskRefusePolicy, shutdownListener);
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
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        return impl.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(Runnable command) {
        impl.execute(command);
    }

    /**
     * Returns the string representation of this executor in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "UpgradedExecutor{" + impl.getBackingExecutor() + '}';
    }
}
