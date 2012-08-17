package org.jtrim.concurrent;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.utils.ExceptionHelper;

/**
 * Allows tasks to be {@link #scheduleTask(Runnable) scheduled} then be
 * {@link #dispatchTasks() executed} later by an {@link Executor} or
 * {@link TaskExecutor}. The tasks will be submitted to the executor in the
 * order they were scheduled.
 * <P>
 * The main benefit of using this class is that scheduling a task is
 * <I>synchronization transparent</I> and therefore can be called while a lock
 * is held. Since submitting task happens in the same order as the scheduling
 * this can be used to invoke event handlers because with events it is usually
 * important to notify the listeners in the order the events actually occurred.
 * <P>
 * It is important to note that to achieve the aforementioned goals tasks will
 * never be submitted concurrently, they will be executed one after another.
 * An additional useful side-effect is that tasks will not even be submitted
 * while another task is being executed on the current thread avoiding another
 * possible source of problems. To clarify this, see the following example:
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
 *   final TaskScheduler scheduler;
 *   scheduler = new TaskScheduler(SyncTaskExecutor.getSimpleExecutor());
 *   scheduler.scheduleTask(new Runnable() {
 *     public void run() {
 *       System.out.print("2");
 *       scheduler.scheduleTask(new PrintTask("4"));
 *       scheduler.dispatchTasks(); // In this case, this is a no-op
 *       System.out.print("3");
 *     }
 *   });
 *   System.out.print("1");
 *   // The next method call will execute both the above scheduled tasks.
 *   scheduler.dispatchTasks();
 *   System.out.print("5");
 * }
 * </PRE>
 * The above {@code doPrint()} method will always print "12345" and the
 * {@code scheduler.dispatchTasks()} call in the scheduled task will actually
 * do nothing in this case but return immediately.
 * <P>
 * The behaviour of this class can also be exploited to "synchronize" actions
 * without locking, so even tasks not being <I>synchronization transparent</I>
 * and also not thread-safe can be used safely using this class. This is the
 * feature what the {@link TaskExecutors#inOrderExecutor(TaskExecutor)} and
 * {@link TaskExecutors#inOrderSyncExecutor()} classes provide.
 *
 * <h3>Dangers of using this class</h3>
 * At first blink it seems tempting to use this class instead of locks because
 * unlike with locks, methods of this class never block and cannot cause
 * dead-locks unless the submitted tasks wait for each other. While this is
 * true, there are three issues to consider:
 * <ul>
 *  <li>
 *   Using {@code TaskScheduler} has a higher per task overhead than using a
 *   lock.
 *  </li>
 *  <li>
 *   Usually there is no telling in which {@code dispatchTasks()} method will
 *   a particular task execute. This may add some additional non-determinism,
 *   making debugging possibly harder.
 *  </li>
 *  <li>
 *   Using locks cause tasks (and the executing threads) to wait for each other,
 *   resulting in a natural throttling. {@code TaskScheduler} however maintains
 *   a list of not yet executed tasks and this can lead to
 *   {@code OutOfMemoryError} if tasks are scheduled faster than can be
 *   submitted to the executor by a single thread. Notice that with locks
 *   the number of such tasks waiting to be executed is limited by the number
 *   of threads (which should not be too high).
 *  </li>
 * </ul>
 * Therefore only use this class when locks are not an option. That is, favor
 * locks when they are safe to use. One of the good use of this class is to
 * notify event listeners.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Other than the {@link #dispatchTasks() dispatchTasks()} method, methods of
 * this class are <I>synchronization transparent</I>. The
 * {@code dispatchTasks()} method is not <I>synchronization transparent</I>
 * only because it submits tasks to the underlying {@code Executor}. If
 * submitting tasks to the underlying executor is
 * <I>synchronization transparent</I> then even this method is
 * <I>synchronization transparent</I>.
 *
 * @see TaskExecutors#inOrderExecutor(TaskExecutor)
 * @see TaskExecutors#inOrderSyncExecutor()
 * @author Kelemen Attila
 */
public final class TaskScheduler {
    /**
     * A convenience method effectively equivalent to
     * {@code new TaskScheduler(SyncTaskExecutor.getSimpleExecutor())}.
     *
     * @return a new task scheduler with a
     *   {@code SyncTaskExecutor.getSimpleExecutor()} underlying executor. This
     *   method never returns {@code null} and always returns a new instance.
     */
    public static TaskScheduler newSyncScheduler() {
        return new TaskScheduler(PlainSyncExecutor.INSTANCE);
    }

    private final Executor executor;
    private final ReentrantLock dispatchLock;
    private final BlockingQueue<Runnable> toDispatch;

    /**
     * Creates a new task scheduler (without any task scheduled) with the given
     * backing executor.
     *
     * @param executor the executor to which tasks will be submitted to by the
     *   {@link #dispatchTasks() dispatchTasks()} method. This argument cannot
     *   be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public TaskScheduler(Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.dispatchLock = new ReentrantLock();
        this.toDispatch = new LinkedBlockingQueue<>();
    }

    /**
     * Creates a new task scheduler (without any task scheduled) with the given
     * backing executor.
     *
     * @param executor the executor to which tasks will be submitted to by the
     *   {@link #dispatchTasks() dispatchTasks()} method. This argument cannot
     *   be {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public TaskScheduler(TaskExecutor executor) {
        this(ExecutorConverter.asExecutor(executor));
    }

    /**
     * Schedules a single task for submitting to the executor specified at
     * construction time.
     * <P>
     * This method will not actually submit the task, to
     * do this call the {@link #dispatchTasks() dispatchTasks()} method.
     *
     * @param task the task to be scheduled for submitting to the executor
     *   specified at construction time. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public void scheduleTask(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        toDispatch.add(task);
    }

    /**
     * Schedules a list of tasks for submitting to the executor specified at
     * construction time. The tasks will be scheduled in the order defined by
     * the given list.
     * <P>
     * This method will not actually submit the task, to
     * do this call the {@link #dispatchTasks() dispatchTasks()} method.
     *
     * @param tasks the list of tasks to be scheduled for submitting to the
     *   executor specified at construction time. This argument cannot be
     *   {@code null} and cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the specified {@code tasks} is
     *   {@code null} or contains {@code null} elements
     */
    public void scheduleTasks(List<? extends Runnable> tasks) {
        ExceptionHelper.checkNotNullElements(tasks, "tasks");

        for (Runnable task: tasks) {
            scheduleTask(task);
        }
    }

    /**
     * Calling this method ensures that previously scheduled tasks will be
     * submitted to the executor specified at construction time. Note that tasks
     * may actually be submitted in different {@code dispatchTasks()} method
     * call but calling this method ensures that they will be submitted.
     * <P>
     * In case submitting a task causes an exception to be thrown, the
     * {@code dispatchTasks()} method actually submitting that task will
     * propagate that exception to the caller. Note however that a single
     * {@code dispatchTasks()} call can submit multiple tasks and if more than
     * one throws an exception, the exceptions after the first one will be
     * suppressed (See: {@link Throwable#addSuppressed(Throwable)}).
     */
    public void dispatchTasks() {
        if (isCurrentThreadDispatching()) {
            // Tasks will be dispatched there.
            return;
        }

        Throwable toThrow = null;
        while (!toDispatch.isEmpty()) {
            if (dispatchLock.tryLock()) {
                try {
                    Runnable task = toDispatch.poll();
                    if (task != null) {
                        executor.execute(task);
                    }
                } catch (Throwable ex) {
                    if (toThrow == null) {
                        toThrow = ex;
                    }
                    else {
                        toThrow.addSuppressed(ex);
                    }
                } finally {
                    dispatchLock.unlock();
                }
            }
            else {
                return;
            }
        }
        if (toThrow != null) {
            ExceptionHelper.rethrow(toThrow);
        }
    }

    /**
     * Checks whether this method was invoked from a
     * {@link #dispatchTasks() dispatchTasks()} method call (i.e.: a task is
     * being submitted on the current calling thread).
     *
     * @return {@code true} if this method was invoked from a
     *   {@code dispatchTasks()}, {@code false} otherwise
     */
    public boolean isCurrentThreadDispatching() {
        return dispatchLock.isHeldByCurrentThread();
    }

    /**
     * Returns the string representation of this {@code TaskScheduler} in no
     * particular format. The string representation will contain the number of
     * tasks current waiting to be submitted.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "TaskScheduler{Tasks to be executed: " + toDispatch.size() + '}';
    }

    private enum PlainSyncExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
