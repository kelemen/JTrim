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
 *   {@link org.jtrim.concurrent.InOrderExecutor} and
 *   {@link org.jtrim.concurrent.InOrderScheduledSyncExecutor}: To "synchronize"
 *   tasks without using locks.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.UpgraderExecutor}: To create an
 *   {@code ExecutorService} from a simple {@code Executor}.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.TaskScheduler}: To invoke event listeners
 *   safely in a multi-threaded context.
 *  </li>
 *  <li>
 *   {@link org.jtrim.concurrent.ExecutorsEx}: For various convenient static
 *   helper methods.
 *  </li>
 * </ul>
 *
 * @see org.jtrim.concurrent.InOrderExecutor
 * @see org.jtrim.concurrent.InOrderScheduledSyncExecutor
 * @see org.jtrim.concurrent.TaskScheduler
 * @see org.jtrim.concurrent.UpdateTaskExecutor
 * @see org.jtrim.concurrent.UpgraderExecutor
 */
package org.jtrim.concurrent;
