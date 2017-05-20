/**
 * Contains classes and interfaces for executing tasks asynchronously.
 * <P>
 * The main features are:
 * <ul>
 *  <li>
 *   {@link org.jtrim2.executor.UpdateTaskExecutor}: To submit tasks where only
 *   the last submitted is relevant.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.executor.TaskExecutors#inOrderExecutor(org.jtrim2.executor.TaskExecutor)} and
 *   {@link org.jtrim2.executor.TaskExecutors#inOrderSyncExecutor()}: To
 *   "synchronize" tasks without using locks.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.executor.TaskExecutors#upgradeToStoppable(org.jtrim2.executor.TaskExecutor)}:
 *   To create a {@code TaskExecutorService} from a simple {@code TaskExecutor}.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.executor.TaskScheduler}: To invoke event listeners
 *   safely in a multi-threaded context.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.executor.ExecutorsEx}, {@link org.jtrim2.executor.TaskExecutors}:
 *   For various convenient static helper methods.
 *  </li>
 * </ul>
 *
 * @see org.jtrim2.executor.TaskExecutors#inOrderExecutor(org.jtrim2.executor.TaskExecutor)
 * @see org.jtrim2.executor.TaskExecutors#inOrderSyncExecutor()
 * @see org.jtrim2.executor.TaskScheduler
 * @see org.jtrim2.executor.UpdateTaskExecutor
 * @see org.jtrim2.executor.TaskExecutors#upgradeToStoppable(org.jtrim2.executor.TaskExecutor)
 */
package org.jtrim2.executor;
