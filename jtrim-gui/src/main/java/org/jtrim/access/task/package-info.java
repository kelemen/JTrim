/**
 * This package will be removed since the introduction of {@code TaskExecutor}
 * made classes and interfaces in this package no longer useful.
 *
 * Contains classes and interfaces for executing REW (read, evaluate, write)
 * tasks.
 *
 * <h3>REW tasks</h3>
 * REW tasks are tasks that can be defined by three parts:
 * <ol>
 *   <li>Reading the input</li>
 *   <li>Evaluating the output</li>
 *   <li>Writing/displaying the output</li>
 * </ol>
 * The first part is executed in the context of a read
 * {@link org.jtrim.access.AccessToken AccessToken} while the last part
 * is executed in the context of a write {@code AccessToken}.
 * Additionally all REW tasks can write (or display) the current
 * progress of their evaluate part in the context of the write
 * {@code AccessToken}.
 * <P>
 * The base interface which must be implemented by REW task is:
 * {@link org.jtrim.access.task.RewTask RewTask}.
 *
 * <h3>REW task executors</h3>
 * Normally all REW tasks are executed by a REW task executor. These
 * executors manage the life cycle of a REW task: reading the input, calculating
 * the output and then writing/displaying it.
 * <P>
 * The base interface which must be implemented by REW task executors is:
 * {@link org.jtrim.access.task.RewTaskExecutor RewTaskExecutor}.
 *
 * @see org.jtrim.access.task.RewTask
 * @see org.jtrim.access.task.RewTaskExecutor
 * @see org.jtrim.access.task.GenericRewTaskExecutor
 */
package org.jtrim.access.task;
