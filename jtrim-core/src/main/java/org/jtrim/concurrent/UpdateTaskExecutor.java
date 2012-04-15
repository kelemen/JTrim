package org.jtrim.concurrent;

/**
 * An interface for executing tasks when only the last task is important.
 * Implementations of this interface are therefore allowed to silently
 * discard tasks that where {@link #execute(Runnable) scheduled} to them
 * previously. That is if scheduling {@code task1} <I>happen-before</I>
 * scheduling {@code task2}, {@code task1} may be silently discarded and never
 * executed.
 * <P>
 * For example see the following code:
 * <code><PRE>
 * class PrintTask implements Runnable {
 *   private final String text;
 *
 *   public PrintTask(String text) {
 *     this.text = text;
 *   }
 *
 *   public void run() {
 *     System.out.print(text);
 *   }
 * }
 *
 * void testExecute() {
 *   UpdateTaskExecutor executor = ...;
 *   executor.execute(new PrintTask("1"));
 *   executor.execute(new PrintTask("2"));
 *   executor.execute(new PrintTask("3"));
 * }
 * </PRE></code>
 * The method {@code testExecute} may either print "3" or "23" or "123",
 * depending on the implementation and other external conditions.
 * <P>
 * The {@code UpdateTaskExecutor} is particularly good when reporting progress
 * to be displayed. Since in this case previously reported progresses would
 * have been overwritten anyway, so it is safe to discard old tasks.
 * <P>
 * Once the {@code UpdateTaskExecutor} must be denied to execute tasks,
 * {@link #shutdown() shutdown()} can be called to stop allowing other tasks
 * to be executed. If tasks are submitted after the {@code UpdateTaskExecutor}
 * was shutted down, they need to be silently discarded by the executor and
 * these discarded tasks are not allowed to overwrite previously submitted
 * tasks. The executor may also discard tasks not yet executed but scheduled
 * previously when shutted down.
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
 * @author Kelemen Attila
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

    /**
     * Prevents executing more tasks by this {@code UpdateTaskExecutor}.
     * This executor must discard tasks that were scheduled to it after this
     * method call (that is, if {@code shutdown()} <I>happen-before</I>
     * {@code execute}). However previously submitted tasks may also be
     * discarded if not yet executed.
     */
    public void shutdown();
}
