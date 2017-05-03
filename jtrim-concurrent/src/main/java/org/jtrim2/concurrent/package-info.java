/**
 * Contains classes and interfaces for generic multi-threading tasks.
 * <P>
 * The main features are:
 * <ul>
 *  <li>
 *   {@link org.jtrim2.concurrent.UpdateTaskExecutor}: To submit tasks where only
 *   the last submitted is relevant.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.concurrent.TaskExecutors#inOrderExecutor(org.jtrim2.concurrent.TaskExecutor)} and
 *   {@link org.jtrim2.concurrent.TaskExecutors#inOrderSyncExecutor()}: To
 *   "synchronize" tasks without using locks.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.concurrent.TaskExecutors#upgradeExecutor(org.jtrim2.concurrent.TaskExecutor)}:
 *   To create a {@code TaskExecutorService} from a simple {@code TaskExecutor}.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.concurrent.TaskScheduler}: To invoke event listeners
 *   safely in a multi-threaded context.
 *  </li>
 *  <li>
 *   {@link org.jtrim2.concurrent.ExecutorsEx}, {@link org.jtrim2.concurrent.TaskExecutors}:
 *   For various convenient static helper methods.
 *  </li>
 * </ul>
 *
 * @see org.jtrim2.concurrent.TaskExecutors#inOrderExecutor(org.jtrim2.concurrent.TaskExecutor)
 * @see org.jtrim2.concurrent.TaskExecutors#inOrderSyncExecutor()
 * @see org.jtrim2.concurrent.TaskScheduler
 * @see org.jtrim2.concurrent.UpdateTaskExecutor
 * @see org.jtrim2.concurrent.TaskExecutors#upgradeExecutor(org.jtrim2.concurrent.TaskExecutor)
 */
package org.jtrim2.concurrent;
