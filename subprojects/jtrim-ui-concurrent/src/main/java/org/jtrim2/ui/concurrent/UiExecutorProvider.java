package org.jtrim2.ui.concurrent;

import org.jtrim2.executor.TaskExecutor;

/**
 * Defines a factory for executors scheduling events on the UI thread of the
 * underlying UI framework (such as <I>Swing</I> or <I>Java FX</I>).
 *
 * <h2>Thread safety</h2>
 * The methods of this interface are expected to be safely accessible from multiple
 * threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are required to be <I>synchronization transparent</I>.
 */
public interface UiExecutorProvider {
    /**
     * Returns a {@code TaskExecutor} which executes tasks submitted to
     * them on the UI thread of the associated UI framework. The returned executor
     * does not necessarily executes tasks in the same order as the tasks were
     * submitted. In case tasks needed to be executed in the same order as they
     * were submitted to the executor: Use the
     * {@link #getStrictExecutor(boolean)} method.
     *
     * @param alwaysExecuteLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   if the calling thread is the UI thread, the framework must execute the scheduled
     *   task later). In case this argument is {@code false}, tasks submitted from an UI
     *   thread will be executed immediately on the calling thread.
     * @return a {@code TaskExecutor} which executes tasks submitted to
     *   them on the UI thread of the associated UI framework. This method never returns
     *   {@code null}.
     */
    public TaskExecutor getSimpleExecutor(boolean alwaysExecuteLater);

    /**
     * Returns a {@code TaskExecutor} which executes tasks submitted to
     * them on the UI thread of the associated UI framework. The returned executor
     * executes tasks in the same order as the tasks were submitted. If you
     * don't need to execute them in the same order, consider using the
     * {@link #getSimpleExecutor(boolean)} method.
     * <P>
     * The returned executor is more efficient than an instance of
     * {@code SwingTaskExecutor}.
     *
     * @param alwaysExecuteLater if this argument is {@code true}, submitted
     *   tasks are never executed synchronously on the calling thread (i.e.:
     *   if the calling thread is the UI thread, the framework must execute the scheduled
     *   task later). In case this argument is {@code false}, tasks submitted from the
     *   UI thread will be executed immediately on the calling thread
     *   (this may not always possible to execute tasks in the order they were submitted).
     * @return a {@code TaskExecutor} which executes tasks submitted to
     *   them on the UI thread of the associated UI framework. This method never returns
     *   {@code null}.
     */
    public TaskExecutor getStrictExecutor(boolean alwaysExecuteLater);
}
