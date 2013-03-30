package org.jtrim.concurrent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks executor implementations executing tasks in submittation order.
 * That is, this is used by {@link TaskExecutors#inOrderExecutor(TaskExecutor)}
 * to detect that an executor already executes tasks in the order they were
 * submitted to the executor.
 *
 * @see TaskExecutors#inOrderExecutor(TaskExecutor)
 * @author Kelemen Attila
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface FifoExecutor {
}
