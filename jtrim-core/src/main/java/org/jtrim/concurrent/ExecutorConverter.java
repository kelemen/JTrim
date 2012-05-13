package org.jtrim.concurrent;

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
 * For convenience, converting executors back and forth does endlessly wrap
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
 *
 *
 * @author Kelemen Attila
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
     * <h5>Limitations</h5>
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
        if (executor instanceof ExecutorServiceAsTaskExecutorService) {
            return ((ExecutorServiceAsTaskExecutorService)executor).executor;
        }
        else {
            return new TaskExecutorServiceAsExecutorService(executor);
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
     * <h5>Limitations</h5>
     * Note that there is no guarantee in general that an
     * {@code ExecutorService} will execute a task (in fact, it will not execute
     * it after it has been shutted down). Therefore, it is possible that the
     * returned {@code TaskExecutorService} will fail to execute cleanup tasks.
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
            return ((TaskExecutorServiceAsExecutorService)executor).executor;
        }
        else {
            return new ExecutorServiceAsTaskExecutorService(executor);
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
     * <h5>Limitations</h5>
     * Note that there is no guarantee in general that an {@code Executor} will
     * execute a task. Therefore, it is possible that the returned
     * {@code TaskExecutor} will fail to execute cleanup tasks.
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
        if (executor instanceof TaskExecutorAsExecutor) {
            return ((TaskExecutorAsExecutor)executor).executor;
        }
        else {
            return new ExecutorAsTaskExecutor(executor);
        }
    }

    /**
     * Returns an {@code Executor} backed by the specified {@code TaskExecutor}.
     * That is, every task submitted to the returned {@code Executor} will be
     * forwarded to the specified {@code TaskExecutor}.
     * <P>
     * Nota that this method was designed so that:
     * {@code asTaskExecutor(asExecutor(executor)) == executor} holds for every
     * non-null {@code TaskExecutor} instances.
     * <P>
     * The returned {@code Executor} has no limitations as an {@code Executor}
     * (since there is not much thing an {@code Executor} mandates).
     *
     * @param executor the {@code TaskExecutor} to which tasks submitted
     *   to the returned {@code Executor} will be forwarded to. This
     *   argument cannot be {@code null}.
     * @return the {@code Executor} backed by the specified
     *   {@code TaskExecutor}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code TaskExecutor} is {@code null}
     */
    public static Executor asExecutor(TaskExecutor executor) {
        if (executor instanceof ExecutorAsTaskExecutor) {
            return ((ExecutorAsTaskExecutor)executor).executor;
        }
        else {
            return new TaskExecutorAsExecutor(executor);
        }
    }

    private ExecutorConverter() {
        throw new AssertionError();
    }
}