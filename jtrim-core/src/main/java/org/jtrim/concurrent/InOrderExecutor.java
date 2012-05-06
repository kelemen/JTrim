package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an executor which forwards task to a given executor and executes
 * tasks without running them concurrently. The tasks will be executed in the
 * order the they were submitted to the {@link #execute(Runnable) execute}
 * method. Subsequent tasks trying to be executed while another one scheduled to
 * this executor is running will be queued and be executed when the running task
 * terminates. Note that even if a tasks schedules a task to this executor, the
 * scheduled task will only be called after the scheduling task terminate.
 * See the following code for clarification:
 * <PRE>
 * class PrintTask implements Runnable {
 *   private final String message;
 *
 *   public PrintTask(String message) {
 *     this.message = message;
 *   }
 *
 *   public void run() {
 *     System.out.print(message);
 *   }
 * }
 *
 * void doPrint() {
 *   final InOrderExecutor executor = ...;
 *   executor.execute(new Runnable() {
 *     public void run() {
 *       System.out.print("1");
 *       executor.execute(new PrintTask("3"));
 *       System.out.print("2");
 *     }
 *   });
 * }
 * </PRE>
 * The {@code doPrint()} method will always print "123", regardless what the
 * underlying executor is.
 * <P>
 * This executor is useful to call tasks which are not safe to be called
 * concurrently. This executor will effectively serialize the calls as if
 * all the tasks were executed by a single thread even if the underlying
 * executor uses multiple threads to execute tasks.
 * <P>
 * Note that this implementation does not expect the tasks to be
 * <I>synchronization transparent</I> but of course, they cannot wait for each
 * other. If a tasks executed by this executor submits a task to this same
 * executor and waits for this newly submitted tasks, it will dead-lock always.
 * This is because no other tasks may run concurrently with the already running
 * tasks and therefore the newly submitted task has no chance to start.
 * <P>
 * <B>Warning</B>: Instances of this class use an internal queue for tasks
 * yet to be executed and if tasks are submitted to executor faster than it
 * can actually execute it will eventually cause the internal buffer to overflow
 * and throw an {@link OutOfMemoryError}. This can occur even if the underlying
 * executor does not execute tasks scheduled to them because tasks will be
 * queued immediately by the {@code execute} method before actually executing
 * the task.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see InOrderScheduledSyncExecutor
 * @see UpgraderExecutor
 * @see TaskScheduler
 * @author Kelemen Attila
 */
public final class InOrderExecutor implements Executor {
    private final Executor executor;
    private final DispatchTask dispatchTask;
    private final TaskScheduler taskScheduler;

    /**
     * Creates a new {@code InOrderExecutor} with the given backing executor.
     * All tasks will be executed in the context of this given executor.
     *
     * @param executor the executor to which tasks will be eventually forwarded
     *   to. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public InOrderExecutor(Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.taskScheduler
                = new TaskScheduler(SyncTaskExecutor.getSimpleExecutor());
        this.dispatchTask = new DispatchTask(taskScheduler);
    }

    /**
     * Enqueues the submitted task and schedules a task to the underlying
     * executor which will eventually execute the task and possibly other
     * submitted tasks. Note that there is no telling which task submitted to
     * the underlying executor will execute the given task only that it will
     * be executed eventually.
     *
     * @param command the task to be executed by the backing executor. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    @Override
    public void execute(Runnable command) {
        taskScheduler.scheduleTask(command);
        executor.execute(dispatchTask);
    }

    /**
     * Checks whether the calling code is running in a task scheduled to this
     * executor.
     *
     * @return {@code true} if the calling code is running in a task scheduled
     *   to this executor, {@code false} otherwise
     */
    public boolean isCurrentThreadExecuting() {
        return taskScheduler.isCurrentThreadDispatching();
    }

    private static class DispatchTask implements Runnable {
        private final TaskScheduler taskScheduler;

        public DispatchTask(TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        @Override
        public void run() {
            taskScheduler.dispatchTasks();
        }
    }
}
