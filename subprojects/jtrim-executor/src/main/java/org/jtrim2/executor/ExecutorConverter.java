package org.jtrim2.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Defines static method to convert between the executors of <I>JTrim</I> and
 * the executors of <I>Java</I>. That is, conversion between
 * {@code ExecutorService} and {@link TaskExecutorService}; and conversion
 * between {@code Executor} and {@link TaskExecutor}.
 * <P>
 * Note that these conversions have limitations and may not provide all the
 * features required for the particular interface. The converter methods
 * document these limitations.
 * <P>
 * For convenience, converting executors back and forth does not endlessly wrap
 * the executors. That is, wrapping and then unwrapping an executor will yield
 * the exact same executor.
 * <P>
 * This class cannot be inherited nor instantiated.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 */
public final class ExecutorConverter {
    /**
     * Returns an {@code ExecutorService} backed by the specified
     * {@code TaskExecutorService}. That is, every task submitted to the
     * returned {@code ExecutorService} will be forwarded to the specified
     * {@code TaskExecutorService}.
     * <P>
     * Nota that this method was designed so that:
     * {@code asTaskExecutorService(asExecutorService(executor)) == executor}
     * holds for every non-null {@code TaskExecutorService} instances.
     *
     * <h3>Limitations</h3>
     * Tasks scheduled to the returned {@code ExecutorService} will never be
     * interrupted, even if the submitted task has been canceled. If the task
     * is not yet executing, it will be canceled.
     * <P>
     * The {@code shutdownNow()} will cancel executing every tasks not yet
     * started executing but will always return an empty list of
     * {@code Runnable} instances.
     *
     * @param executor the {@code TaskExecutorService} to which tasks submitted
     *   to the returned {@code ExecutorService} will be forwarded to. This
     *   argument cannot be {@code null}.
     * @return the {@code ExecutorService} backed by the specified
     *   {@code TaskExecutorService}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code TaskExecutorService} is {@code null}
     */
    public static ExecutorService asExecutorService(TaskExecutorService executor) {
        return asExecutorService(executor, false);
    }

    /**
     * Returns an {@code ExecutorService} backed by the specified
     * {@code TaskExecutorService}. That is, every task submitted to the
     * returned {@code ExecutorService} will be forwarded to the specified
     * {@code TaskExecutorService}.
     * <P>
     * Nota that this method was designed so that:
     * {@code asTaskExecutorService(asExecutorService(executor, b)) == executor}
     * holds for every {@code b} and non-null {@code TaskExecutorService}
     * instances.
     *
     * <h3>Limitations</h3>
     * Tasks scheduled to the returned {@code ExecutorService} will only be
     * interrupted, if {@code mayInterruptTasks} is {@code true}, otherwise
     * cancellation request will be ignored after the task has been started.
     * If the task is not yet executing, it will be canceled.
     * <P>
     * The {@code shutdownNow()} will cancel executing every tasks not yet
     * started executing (and will interrupt them if {@code mayInterruptTasks}
     * is {@code true}) but will always return an empty list of {@code Runnable}
     * instances.
     * <P>
     * <B>Warning</B>: Allowing the tasks to be interrupted can be dangerous in
     * some cases. That is, if it is not known what thread the executor may
     * execute tasks (such is the case for {@code SyncTaskExecutor}), the
     * executing thread may associate unwanted meaning to thread interruption.
     * Allowing thread interruption is generally safe with
     * {@link java.util.concurrent.ThreadPoolExecutor} instances.
     *
     * @param executor the {@code TaskExecutorService} to which tasks submitted
     *   to the returned {@code ExecutorService} will be forwarded to. This
     *   argument cannot be {@code null}.
     * @param mayInterruptTasks if {@code true} the thread executing the task
     *   will be interrupted upon cancellation requests, otherwise the
     *   executing thread will not be interrupted
     * @return the {@code ExecutorService} backed by the specified
     *   {@code TaskExecutorService}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code TaskExecutorService} is {@code null}
     */
    public static ExecutorService asExecutorService(
            TaskExecutorService executor,
            boolean mayInterruptTasks) {
        if (executor instanceof ExecutorServiceAsTaskExecutorService) {
            return ((ExecutorServiceAsTaskExecutorService) executor).executor;
        } else {
            return new TaskExecutorServiceAsExecutorService(executor, mayInterruptTasks);
        }
    }

    /**
     * Returns a {@code TaskExecutorService} backed by the specified
     * {@code ExecutorService}. That is, every task submitted to the
     * returned {@code TaskExecutorService} will be forwarded to the specified
     * {@code ExecutorService}.
     * <P>
     * Nota that this method was designed so that:
     * {@code asExecutorService(asTaskExecutorService(executor)) == executor}
     * holds for every non-null {@code ExecutorService} instances.
     *
     * <h3>Limitations</h3>
     * Note that there is no guarantee in general that an
     * {@code ExecutorService} will execute a task (in fact, it will not execute
     * it after it has been shut down). Therefore, it is possible that the
     * returned {@code TaskExecutorService} will fail to properly complete the
     * submitted tasks.
     *
     * @param executor the {@code ExecutorService} to which tasks submitted
     *   to the returned {@code TaskExecutorService} will be forwarded to. This
     *   argument cannot be {@code null}.
     * @return the {@code TaskExecutorService} backed by the specified
     *   {@code ExecutorService}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code ExecutorService} is {@code null}
     */
    public static TaskExecutorService asTaskExecutorService(ExecutorService executor) {
        if (executor instanceof TaskExecutorServiceAsExecutorService) {
            return ((TaskExecutorServiceAsExecutorService) executor).executor;
        } else {
            return new ExecutorServiceAsTaskExecutorService(executor, false);
        }
    }

    /**
     * Returns a {@code TaskExecutor} backed by the specified {@code Executor}.
     * That is, every task submitted to the returned {@code TaskExecutor} will
     * be forwarded to the specified {@code Executor}.
     * <P>
     * Nota that this method was designed so that:
     * {@code asExecutor(asTaskExecutor(executor)) == executor} holds for every
     * non-null {@code Executor} instances.
     *
     * <h3>Limitations</h3>
     * Note that there is no guarantee in general that an {@code Executor} will
     * execute a task. Therefore, it is possible that the returned
     * {@code TaskExecutor} will fail to properly complete the
     * submitted tasks.
     *
     * @param executor the {@code Executor} to which tasks submitted
     *   to the returned {@code TaskExecutor} will be forwarded to. This
     *   argument cannot be {@code null}.
     * @return the {@code TaskExecutor} backed by the specified
     *   {@code Executor}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code Executor} is {@code null}
     */
    public static TaskExecutor asTaskExecutor(Executor executor) {
        return new ExecutorAsTaskExecutor(executor, false);
    }

    private ExecutorConverter() {
        throw new AssertionError();
    }
}
