/**
 * Contains classes and interfaces for generic multi-threading tasks.
 * <P>
 * The main features are:
 * <ul>
 *  <li>
 *   {@link org.jtrim.concurrent.UpdateTaskExecutor}: To submit tasks where only
 *   the last submitted is relevant.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.TaskExecutors#inOrderExecutor(org.jtrim.concurrent.TaskExecutor)} and
 *   {@link org.jtrim.concurrent.TaskExecutors#inOrderSyncExecutor()}: To
 *   "synchronize" tasks without using locks.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.TaskExecutors#upgradeExecutor(org.jtrim.concurrent.TaskExecutor)}:
 *   To create a {@code TaskExecutorService} from a simple {@code TaskExecutor}.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.TaskScheduler}: To invoke event listeners
 *   safely in a multi-threaded context.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.ExecutorsEx}, {@link org.jtrim.concurrent.TaskExecutors}:
 *   For various convenient static helper methods.
 *  </li>
 * </ul>
 *
 * @see org.jtrim.concurrent.TaskExecutors#inOrderExecutor(org.jtrim.concurrent.TaskExecutor)
 * @see org.jtrim.concurrent.TaskExecutors#inOrderSyncExecutor()
 * @see org.jtrim.concurrent.TaskScheduler
 * @see org.jtrim.concurrent.UpdateTaskExecutor
 * @see org.jtrim.concurrent.TaskExecutors#upgradeExecutor(org.jtrim.concurrent.TaskExecutor)
 */
package org.jtrim.concurrent;
