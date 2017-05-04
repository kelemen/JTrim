package org.jtrim2.swing.concurrent;

import org.jtrim2.concurrent.GenericUpdateTaskExecutor;
import org.jtrim2.concurrent.UpdateTaskExecutor;

/**
 * An {@code UpdateTaskExecutor} implementation which executes tasks on the
 * <I>AWT Event Dispatch Thread</I>.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently
 * as required by {@code UpdateTaskExecutor}.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class SwingUpdateTaskExecutor implements UpdateTaskExecutor {
    private final UpdateTaskExecutor executor;

    /**
     * Creates a new {@code SwingUpdateTaskExecutor} which will execute
     * submitted tasks on the <I>AWT Event Dispatch Thread</I>.
     * <P>
     * This constructor is equivalent to calling
     * {@code SwingUpdateTaskExecutor(true)}.
     */
    public SwingUpdateTaskExecutor() {
        this(true);
    }

    /**
     * Creates a new {@code SwingUpdateTaskExecutor} which will execute
     * submitted tasks on the <I>AWT Event Dispatch Thread</I>.
     *
     * @param alwaysInvokeLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   they are always submitted as {@code SwingUtilities.invokeLater} does
     *   it). In case this argument is {@code false}, tasks submitted from the
     *   <I>AWT Event Dispatch Thread</I> will be executed immediately on the
     *   calling thread (this may not always possible to execute tasks in the
     *   order they were submitted).
     */
    public SwingUpdateTaskExecutor(boolean alwaysInvokeLater) {
        this.executor = new GenericUpdateTaskExecutor(
                SwingTaskExecutor.getStrictExecutor(alwaysInvokeLater));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
