package org.jtrim2.taskgraph;

/**
 * Defines a reference to the input of a task node. The task node might read
 * its input in its task node action.
 * <P>
 * Note: The executor may decide that it will execute task node again. In this case, the action
 * may request its input again. However, each action call may consume the input at most once.
 *
 * <h2>Thread safety</h2>
 * The method of this interface may not be accessed concurrently by multiple threads.
 * This is also implied that it is not allowed to call {@code consumeInput} multiple times.
 *
 * <h3>Synchronization transparency</h3>
 * The method of this interface is not <I>synchronization transparent</I> and may only
 * be called from the action of a task node.
 *
 * @param <I> the type of the input
 *
 * @see TaskInputBinder
 */
public interface TaskInputRef<I> {
    /**
     * Returns the input bound when creating the task node. This method may not be
     * called multiple times by the same task action call. Attempting to do so is considered
     * an error. This behaviour allows preventing unneeded memory retention.
     *
     * @return the input bound when creating the task node. This method may return {@code null},
     *   if the dependency node action returned {@code null}.
     */
    public I consumeInput();
}
