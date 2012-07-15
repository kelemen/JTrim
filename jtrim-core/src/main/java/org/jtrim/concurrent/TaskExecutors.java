package org.jtrim.concurrent;

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
 *
 * @author Kelemen Attila
 */
public final class TaskExecutors {
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
        }
        else {
            return new UnstoppableTaskExecutor(executor);
        }
    }

    /**
     * Returns an executor which forwards task to a given executor and executes
     * tasks without running them concurrently. The tasks will be executed in
     * the order the they were submitted to the
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask, CleanupTask) execute}
     * method of the returned {@code TaskExecutor}. Subsequent tasks, trying to
     * be executed while another one scheduled to this executor is running will
     * be queued and be executed when the running task terminates. Note that
     * even if a tasks schedules a task to this executor, the scheduled task
     * will only be called after the scheduling task terminates. See the
     * following code for clarification:
     * <PRE>
     * class PrintTask implements CancelableTask {
     *   private final String message;
     *
     *   public PrintTask(String message) {
     *     this.message = message;
     *   }
     *
     *   public void execute(CancellationToken cancelToken) {
     *     System.out.print(message);
     *   }
     * }
     *
     * void doPrint(TaskExecutor executor) {
     *   final TaskExecutor inOrderExec = TaskExecutors.inOrderExecutor(executor);
     *   executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
     *     public void execute(CancellationToken cancelToken) {
     *       System.out.print("1");
     *       executor.execute(new PrintTask("3"));
     *       System.out.print("2");
     *     }
     *   }, null);
     * }
     * </PRE>
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
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public static TaskExecutor inOrderExecutor(TaskExecutor executor) {
        return new InOrderTaskExecutor(executor);
    }

    /**
     * Returns an executor which invokes tasks in the order they were scheduled
     * to it. The tasks will execute in one of the
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask, CleanupTask) execute}
     * calls of the same executor but the user has no influence in which one it
     * will actually be called.
     * <P>
     * This method is effectively equivalent to calling
     * {@code inOrderExecutor(SyncTaskExecutor.getSimpleExecutor())}.
     *
     * @return an executor which invokes tasks in the order they were scheduled
     *   to it. This method never returns {@code null}.
     */
    public static TaskExecutor inOrderSyncExecutor() {
        return inOrderExecutor(SyncTaskExecutor.getSimpleExecutor());
    }

    /**
     * Returns a {@code TaskExecutorService} which upgrades the specified
     * {@link TaskExecutor TaskExecutor} to provide all the features of a
     * {@code TaskExecutorService}. Tasks submitted to the returned
     * {@code TaskExecutorService} will be forwarded to the {@code execute}
     * method of the specified {@code TaskExecutor}.
     * <P>
     * Shutting down the returned executor has no effect on the specified
     * {@code TaskExecutor}, even if already implemented the
     * {@code TaskExecutorService} interface.
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
     */
    public static TaskExecutorService upgradeExecutor(TaskExecutor executor) {
        if (executor instanceof UnstoppableTaskExecutor) {
            return (TaskExecutorService)executor;
        }
        else {
            return new UpgradedTaskExecutor(executor);
        }
    }

    private TaskExecutors() {
        throw new AssertionError();
    }
}
