package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import org.jtrim2.cancel.CancellationToken;

/**
 * Contains static helper and factory methods for various useful
 * {@link TaskExecutor} and {@link TaskExecutorService} implementations.
 * <P>
 * This class cannot be inherited and instantiated.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are
 * <I>synchronization transparent</I>.
 */
public final class TaskExecutors {
    /**
     * Returns a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     *
     * @param daemon the daemon status of the threads running the tasks of the returned executor
     * @return a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     *   This method never returns {@code null}.
     */
    public static TaskExecutor newThreadExecutor(boolean daemon) {
        // There is very little benefit to creating new instances here, because the only thing
        // we would gain is that thread number counters of those executors would not conflict.
        // However, that is practically nothing compared to starting a new thread.
        return daemon
                ? DedicatedThreadTaskExecutor.DEFAULT_DAEMON_EXECUTOR
                : DedicatedThreadTaskExecutor.DEFAULT_NON_DAEMON_EXECUTOR;
    }

    /**
     * Returns a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     *
     * @param daemon the daemon status of the threads running the tasks of the returned executor
     * @param poolName the string to be contained in the name of the threads running the tasks
     *   of the returned executor. The name of the threads will also contain a unique number to distinguish
     *   between threads. This argument cannot be {@code null}.
     * @return a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     *   This method never returns {@code null}.
     */
    public static TaskExecutor newThreadExecutor(boolean daemon, String poolName) {
        return new DedicatedThreadTaskExecutor(new ExecutorsEx.NamedThreadFactory(daemon, poolName));
    }

    /**
     * Returns a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     * <P>
     * The threads are created by the provided {@code ThreadFactory}. The provided thread factory may
     * not return {@code null} object for thread. Doing so will cause a {@link NullPointerException} to be thrown
     * to the submitted of the task.
     *
     * @param threadFactory the {@code ThreadFactory} creating new threads for the submitted tasks.
     *   This argument cannot be {@code null}.
     * @return a {@code TaskExecutor} creating a new dedicated thread for each submitted task.
     *   This method never returns {@code null}.
     */
    public static TaskExecutor newThreadExecutor(ThreadFactory threadFactory) {
        return new DedicatedThreadTaskExecutor(threadFactory);
    }

    /**
     * Returns an {@code TaskExecutorService} forwarding all of its methods to
     * the given {@code TaskExecutorService} but the returned
     * {@code TaskExecutorService} cannot be shutted down. Attempting to
     * shutdown the returned {@code ExecutorService} results in an unchecked
     * {@code UnsupportedOperationException} to be thrown.
     *
     * @param executor the executor to which calls to be forwarded by the
     *   returned {@code TaskExecutorService}. This argument cannot be
     *   {@code null}.
     * @return an {@code TaskExecutorService} which forwards all of its calls to
     *   the specified executor but cannot be shutted down. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static TaskExecutorService asUnstoppableExecutor(
            TaskExecutorService executor) {
        if (executor instanceof UnstoppableTaskExecutor) {
            return executor;
        } else {
            return new UnstoppableTaskExecutor(executor);
        }
    }

    /**
     * Returns an executor executing synchronously on the calling thread without calling task recursively.
     * That is, if a task submitted to this executor submits a task to the same executor, the task will not be
     * called immediately but delayed until the top most call returns.
     * <P>
     * This executor is useful to prevent too deep calls potentially causing {@link StackOverflowError}.
     * <P>
     * For example, consider the following code:
     * <PRE>{@code
     * TaskExecutor executor = TaskExecutors.syncNonRecursiveExecutor();
     * executor.execute(() -> {
     *   System.out.print("1");
     *   executor.execute(() -> System.out.print("3"));
     *   System.out.print("2");
     * });
     * }</PRE>
     * The above code will always print "123".
     * <P>
     * <B>Note on exceptions</B>: This implementation differs from
     * {@link ExecutorsEx#syncNonRecursiveExecutor() ExecutorsEx.syncNonRecursiveExecutor} the way it handles
     * exceptions happening in a task. That is, an exception happening in any of the tasks will not affect other
     * tasks of the same thread, and their exception can only be accessed through the returned {@code CompletionStage}.
     *
     * @return an executor executing synchronously on the calling thread without calling task recursively.
     *   This method never returns {@code null}.
     *
     * @see ExecutorsEx#syncNonRecursiveExecutor()
     */
    public static TaskExecutor syncNonRecursiveExecutor() {
        return new SyncNonRecursiveTaskExecutor();
    }

    /**
     * Returns an executor which forwards task to a given executor and executes
     * tasks without running them concurrently. The tasks will be executed in
     * the order the they were submitted to the
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask) execute}
     * method of the returned {@code TaskExecutor}. Subsequent tasks, attempted to
     * be executed while another one scheduled to this executor is running will
     * be queued and be executed when the running task terminates. Note that
     * even if a tasks schedules a task to this executor, the scheduled task
     * will only be called after the scheduling task terminates. See the
     * following code for clarification:
     * <PRE>{@code
     * void doPrint(TaskExecutor executor) {
     *   TaskExecutor inOrderExec = TaskExecutors.inOrderExecutor(executor);
     *   inOrderExec.execute(() -> {
     *     System.out.print("1");
     *     inOrderExec.execute(() -> System.out.print("3"));
     *     System.out.print("2");
     *   });
     * }
     * }</PRE>
     * The {@code doPrint} method will always print "123", regardless what the
     * passed executor is.
     * <P>
     * The returned executor is useful for calling tasks which are not safe to
     * be called concurrently. This executor will effectively serialize the
     * calls as if all the tasks were executed by a single thread even if the
     * underlying executor uses multiple threads to execute tasks.
     * <P>
     * Note that this implementation does not expect the tasks to be
     * <I>synchronization transparent</I> but of course, they cannot wait for
     * each other. If a tasks executed by this executor submits a task to this
     * same executor and waits for this newly submitted tasks, it will dead-lock
     * always. This is because no other tasks may run concurrently with the
     * already running tasks and therefore the newly submitted task has no
     * chance to start.
     * <P>
     * <B>Note</B>: This method may return the same executor passed in the
     * argument if the specified executor already executes tasks in the order they
     * were submitted.
     * <B>Warning</B>: Instances of this class use an internal queue for tasks
     * yet to be executed and if tasks are submitted to executor faster than it
     * can actually execute it will eventually cause the internal buffer to
     * overflow and throw an {@link OutOfMemoryError}. This can occur even if
     * the underlying executor does not execute tasks scheduled to them because
     * tasks will be queued immediately by the {@code execute} method before
     * actually executing the task.
     *
     * @param executor the executor to which tasks will be eventually forwarded
     *   to. This argument cannot be {@code null}.
     * @return executor which forwards task to a given executor and executes
     *   tasks without running them concurrently. This method never returns
     *   {@code null} and may return the same executor passed in the argument if
     *   the specified executor executes tasks in the order they were submitted.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     *
     * @see SingleThreadedExecutor
     * @see #inOrderSimpleExecutor(TaskExecutor)
     */
    public static MonitorableTaskExecutor inOrderExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");

        if (FifoExecutor.isFifoExecutor(executor)
                && executor instanceof MonitorableTaskExecutor) {
            return (MonitorableTaskExecutor) executor;
        } else {
            return new InOrderTaskExecutor(executor);
        }
    }

    /**
     * Returns an executor which forwards task to a given executor and executes
     * tasks without running them concurrently (this method differs from
     * {@link #inOrderExecutor(TaskExecutor)} only by not necessarily returning
     * a {@link MonitorableTaskExecutor}). The tasks will be executed in the
     * order the they were submitted to the
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask) execute}
     * method of the returned {@code TaskExecutor}. Subsequent tasks, attempted to
     * be executed while another one scheduled to this executor is running will
     * be queued and be executed when the running task terminates. Note that
     * even if a tasks schedules a task to this executor, the scheduled task
     * will only be called after the scheduling task terminates. See the
     * following code for clarification:
     * <PRE>{@code
     * void doPrint(TaskExecutor executor) {
     *   TaskExecutor inOrderExec = TaskExecutors.inOrderSimpleExecutor(executor);
     *   inOrderExec.execute(() -> {
     *     System.out.print("1");
     *     inOrderExec.execute(() -> System.out.print("3"));
     *     System.out.print("2");
     *   });
     * }
     * }</PRE>
     * The {@code doPrint} method will always print "123", regardless what the
     * passed executor is.
     * <P>
     * The returned executor is useful for calling tasks which are not safe to
     * be called concurrently. This executor will effectively serialize the
     * calls as if all the tasks were executed by a single thread even if the
     * underlying executor uses multiple threads to execute tasks.
     * <P>
     * Note that this implementation does not expect the tasks to be
     * <I>synchronization transparent</I> but of course, they cannot wait for
     * each other. If a tasks executed by this executor submits a task to this
     * same executor and waits for this newly submitted tasks, it will dead-lock
     * always. This is because no other tasks may run concurrently with the
     * already running tasks and therefore the newly submitted task has no
     * chance to start.
     * <P>
     * <B>Note</B>: This method may return the same executor passed in the
     * argument if the specified executor already executes tasks in submittation
     * order.
     * <B>Warning</B>: Instances of this class use an internal queue for tasks
     * yet to be executed and if tasks are submitted to executor faster than it
     * can actually execute it will eventually cause the internal buffer to
     * overflow and throw an {@link OutOfMemoryError}. This can occur even if
     * the underlying executor does not execute tasks scheduled to them because
     * tasks will be queued immediately by the {@code execute} method before
     * actually executing the task.
     *
     * @param executor the executor to which tasks will be eventually forwarded
     *   to. This argument cannot be {@code null}.
     * @return executor which forwards task to a given executor and executes
     *   tasks without running them concurrently. This method never returns
     *   {@code null} and may return the same executor passed in the argument if
     *   the specified executor executes tasks in submittation order.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     *
     * @see SingleThreadedExecutor
     * @see #inOrderExecutor(TaskExecutor)
     */
    public static TaskExecutor inOrderSimpleExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");

        if (FifoExecutor.isFifoExecutor(executor)) {
            return executor;
        } else {
            return new InOrderTaskExecutor(executor);
        }
    }

    /**
     * Returns an executor which invokes tasks in the order they were scheduled
     * to it. The tasks will execute in one of the
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask) execute}
     * calls of the same executor but the user has no influence in which one it
     * will actually be called.
     * <P>
     * This method is effectively equivalent to calling
     * {@code inOrderExecutor(SyncTaskExecutor.getSimpleExecutor())}.
     *
     * @return an executor which invokes tasks in the order they were scheduled
     *   to it. This method never returns {@code null}.
     */
    public static MonitorableTaskExecutor inOrderSyncExecutor() {
        return inOrderExecutor(SyncTaskExecutor.getSimpleExecutor());
    }

    /**
     * Returns a {@code TaskExecutorService} which upgrades the specified
     * {@link TaskExecutor TaskExecutor} to provide all the features of a
     * {@code TaskExecutorService}. Tasks submitted to the returned
     * {@code TaskExecutorService} will be forwarded to the {@code execute}
     * method of the specified {@code TaskExecutor}.
     * <P>
     * Shutting down the returned executor will shutdown the returned executor
     * but not the passed {@code TaskExecutor} even if it implemented the
     * {@code TaskExecutorService} interface.
     * <P>
     * <B>Note</B>: If you don't want to shutdown the return
     * {@code TaskExecutorService}, you should rather use the
     * {@link #upgradeToUnstoppable(TaskExecutor) upgradeToUnstoppable} method.
     *
     * @param executor the {@code TaskExecutor} to which the returned
     *   {@code TaskExecutorService} will forward submitted tasks to be
     *   executed. This argument cannot be {@code null}.
     * @return a {@code TaskExecutorService} which upgrades the specified
     *   {@link TaskExecutor TaskExecutor} to provide all the features of a
     *   {@code TaskExecutorService}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified {@code TaskExecutor}
     *   is {@code null}
     *
     * @see #upgradeToUnstoppable(TaskExecutor)
     */
    public static TaskExecutorService upgradeToStoppable(TaskExecutor executor) {
        return new UpgradedTaskExecutor(executor);
    }

    /**
     * Returns a {@code TaskExecutorService} which upgrades the specified
     * {@link TaskExecutor TaskExecutor} to provide all the features of a
     * {@code TaskExecutorService} except that the returned executer cannot be
     * shut down. Tasks submitted to the returned {@code TaskExecutorService}
     * will be forwarded to the {@code execute} method of the specified
     * {@code TaskExecutor}.
     * <P>
     * The returned executor cannot be shut down. Attempting to do so will cause
     * an unchecked {@code UnsupportedOperationException} to be thrown.
     *
     * @param executor the {@code TaskExecutor} to which the returned
     *   {@code TaskExecutorService} will forward submitted tasks to be
     *   executed. This argument cannot be {@code null}.
     * @return a {@code TaskExecutorService} which upgrades the specified
     *   {@link TaskExecutor TaskExecutor} to provide all the features of a
     *   {@code TaskExecutorService}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified {@code TaskExecutor}
     *   is {@code null}
     *
     * @see #upgradeToStoppable(TaskExecutor)
     */
    public static TaskExecutorService upgradeToUnstoppable(TaskExecutor executor) {
        if (executor instanceof UnstoppableTaskExecutor) {
            return (TaskExecutorService) executor;
        } else {
            return new UnstoppableTaskExecutor(new UpgradedTaskExecutor(executor));
        }
    }

    /**
     * Returns an executor which submits tasks to the specified executor and
     * is context aware.
     * <P>
     * Note that, tasks passed to the executor specified in the parameters has
     * no effect on the returned executor.
     *
     * @param executor the specified executor to which the returned executor
     *   will submit tasks to. This argument cannot be {@code null}.
     * @return an executor which submits tasks to the specified executor and
     *   is context aware. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static ContextAwareWrapper contextAware(TaskExecutor executor) {
        return new ContextAwareWrapper(executor);
    }

    /**
     * Returns an executor which submits tasks to the specified executor and
     * is context aware. If the passed executor is already an instance of
     * {@link ContextAwareTaskExecutor} then the passed executor is returned.
     * <P>
     * Note that, tasks passed to the executor specified in the parameters may
     * or may not be count as running in the context of the returned executor.
     *
     * @param executor the specified executor to which the returned executor
     *   will submit tasks to. This argument cannot be {@code null}.
     * @return an executor which submits tasks to the specified executor and
     *   is context aware. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static ContextAwareTaskExecutor contextAwareIfNecessary(TaskExecutor executor) {
        if (executor instanceof ContextAwareTaskExecutor) {
            return (ContextAwareTaskExecutor) executor;
        } else {
            return contextAware(executor);
        }
    }

    /**
     * Returns a {@code TaskExecutorService} which forwards tasks to the
     * specified {@code TaskExecutorService} but always logs exceptions thrown
     * by tasks sheduled to the returned executor. This is useful when debugging
     * because a {@code TaskExecutorService} implementation cannot determine if
     * an exception thrown by a task expected by the client code or not.
     * <P>
     * Other than logging exceptions the returned executor delegates all method
     * calls to the appropriate method of the specified executor.
     * <P>
     * The exceptions are logged in a {@code SEVERE} level logmessage.
     * <P>
     * <B>Warning</B>: The returned executor will not log exceptions thrown by
     * cleanup tasks because {@code TaskExecutorService} implementations are
     * expected to log or rethrow them.
     *
     * @param executor the {@code TaskExecutorService} to which method calls are
     *   forwarded to. This argument cannot be {@code null}.
     * @return a {@code TaskExecutorService} which forwards tasks to the
     *   specified {@code TaskExecutorService} but always logs exceptions thrown
     *   by tasks sheduled to the returned executor. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static TaskExecutorService debugExecutorService(TaskExecutorService executor) {
        return new DebugTaskExecutorService(executor);
    }

    /**
     * Returns a {@code TaskExecutor} which forwards tasks to the specified
     * {@code TaskExecutor} but always logs exceptions thrown by tasks sheduled
     * to the returned executor. This is useful when debugging because a
     * {@code TaskExecutor} implementation cannot determine if an exception
     * thrown by a task expected by the client code or not.
     * <P>
     * Other than logging exceptions the returned executor delegates all method
     * calls to the appropriate method of the specified executor.
     * <P>
     * The exceptions are logged in a {@code SEVERE} level logmessage.
     *
     * @param executor the {@code TaskExecutor} to which method calls are
     *   forwarded to. This argument cannot be {@code null}.
     * @return a {@code TaskExecutor} which forwards tasks to the specified
     *   {@code TaskExecutor} but always logs exceptions thrown by tasks
     *   scheduled to the returned executor. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static TaskExecutor debugExecutor(TaskExecutor executor) {
        return new DebugTaskExecutor(executor);
    }

    private TaskExecutors() {
        throw new AssertionError();
    }
}
