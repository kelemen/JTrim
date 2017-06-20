package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;

/**
 * An {@code TaskExecutorService} implementation which delegates all of its
 * methods to another {@code TaskExecutorService} specified at construction
 * time.
 * <P>
 * This implementation does not declare any methods other than the ones
 * {@code TaskExecutorService} offers but implements all of them by forwarding
 * to another {@code TaskExecutorService} implementation specified at
 * construction time.
 * <P>
 * Note that apart from the methods of {@code TaskExecutorService} even the
 * {@code toString()} method is forwarded.
 * <P>
 * This class was designed for two reasons:
 * <ul>
 *  <li>
 *   To allow a safer way of class inheritance, so there can be no unexpected
 *   dependencies on overridden methods. To imitate inheritance of a
 *   {@code TaskExecutorService}: specify the {@code TaskExecutorService} you
 *   want to "subclass" in the constructor and override the required methods or
 *   provide new ones.
 *  </li>
 *  <li>
 *   To hide other public methods of an {@code TaskExecutorService} from
 *   external code. This way, the external code can only access methods which
 *   the {@code TaskExecutorService} interface defines.
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The thread safety properties of this class entirely depend on the wrapped
 * {@code TaskExecutorService} instance.
 *
 * <h4>Synchronization transparency</h4>
 * If instances of this class are <I>synchronization transparent</I> or if its
 * synchronization control can be observed by external code entirely depends on
 * the wrapped {@code TaskExecutorService} instance.
 */
public class DelegatedTaskExecutorService implements TaskExecutorService {
    /**
     * The {@code TaskExecutorService} to which the methods are forwarded.
     * This field can never be {@code null} because the constructor throws
     * {@code NullPointerException} if {@code null} was specified as the
     * {@code TaskExecutorService}.
     */
    protected final TaskExecutorService wrappedExecutor;

    /**
     * Initializes the {@link #wrappedExecutor wrappedExecutor} field with
     * the specified argument.
     *
     * @param wrappedExecutor the {@code TaskExecutorService} to which the
     *   methods are forwarded. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code TaskExecutorService} is {@code null}
     */
    public DelegatedTaskExecutorService(TaskExecutorService wrappedExecutor) {
        Objects.requireNonNull(wrappedExecutor, "wrappedExecutor");
        this.wrappedExecutor = wrappedExecutor;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        return wrappedExecutor.executeFunction(cancelToken, function);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
        return wrappedExecutor.execute(cancelToken, task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(Runnable command) {
        wrappedExecutor.execute(command);
    }

    @Override
    public CompletionStage<Void> executeStaged(Runnable task) {
        return wrappedExecutor.executeStaged(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        wrappedExecutor.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdownAndCancel() {
        wrappedExecutor.shutdownAndCancel();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isShutdown() {
        return wrappedExecutor.isShutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return wrappedExecutor.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        return wrappedExecutor.addTerminateListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        wrappedExecutor.awaitTermination(cancelToken);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean tryAwaitTermination(
            CancellationToken cancelToken, long timeout, TimeUnit unit) {

        return wrappedExecutor.tryAwaitTermination(cancelToken, timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return wrappedExecutor.toString();
    }
}
