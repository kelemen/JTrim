package org.jtrim2.executor;

/**
 * An interface for executing tasks when only the last task is important.
 * Implementations of this interface are therefore allowed to silently
 * discard tasks that where {@link #execute(Runnable) scheduled} to them
 * previously. That is if scheduling {@code task1} <I>happen-before</I>
 * scheduling {@code task2}, {@code task1} may be silently discarded and never
 * executed.
 * <P>
 * For example see the following code:
 * <PRE>{@code
 * void testExecute() {
 *   UpdateTaskExecutor executor = ...;
 *   executor.execute(() -> System.out.print("1"));
 *   executor.execute(() -> System.out.print("2"));
 *   executor.execute(() -> System.out.print("3"));
 * }
 * }</PRE>
 * The method {@code testExecute} may either print "3" or "23" or "123",
 * depending on the implementation and other external conditions.
 * <P>
 * The {@code UpdateTaskExecutor} is particularly good when reporting progress
 * to be displayed. Since in this case previously reported progresses would
 * have been overwritten anyway, so it is safe to discard old tasks.
 * <P>
 * Note that although not strictly required to execute tasks in the order they
 * were scheduled but in most cases an {@code UpdateTaskExecutor} works well
 * only when executes task in order.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see GenericUpdateTaskExecutor
 */
public interface UpdateTaskExecutor {
    /**
     * Submits a task which is to be executed in the future. This task may
     * cause previously submitted tasks to be discarded an never run. Therefore
     * the currently submitted task may get discarded by a subsequent
     * {@code execute} call.
     *
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     */
    public void execute(Runnable task);
}
