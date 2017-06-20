package org.jtrim2.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.TaskExecutionException;
import org.jtrim2.event.ListenerRef;

/**
 * Defines a {@code TaskExecutor} where client code can determine where and when
 * submitted tasks are executed. This task is generally useful for testing.
 * <P>
 * You can submit tasks the usual way as defined by the {@code TaskExecutor}
 * interface and if you actually want to execute them, you have to call the
 * {@link #executeCurrentlySubmitted() executeCurrentlySubmitted} or the
 * {@link #tryExecuteOne() tryExecuteOne} methods. This executor has a FIFO
 * queue and the previously mentioned methods will execute them in the order
 * they were submitted, unless you call them concurrently (in this case no
 * ordering guarantee can be made).
 * <P>
 * <B>Warning</B>: If you fail to execute submitted tasks, then completion handlers
 * will not be notified, so it is the responsibility of the client code to
 * execute them.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 */
public final class ManualTaskExecutor extends AbstractTaskExecutor {
    private final Lock mainLock;
    private final List<TaskExecutorJob> jobs;
    private final boolean eagerCancel;

    /**
     * Creates a new {@code ManualTaskExecutor} with an empty task queue.
     *
     * @param eagerCancel if this argument is {@code true} and a task is
     *   canceled, then the task will be eagerly unreferenced (allowing it to be
     *   garbage collected) and will not be attempted to be executed. In case
     *   this argument is {@code false}, submitted tasks will always be executed
     *   even if canceled (though they can check their cancellation token and
     *   cancel themselves).
     */
    public ManualTaskExecutor(boolean eagerCancel) {
        this.mainLock = new ReentrantLock();
        this.jobs = new LinkedList<>();
        this.eagerCancel = eagerCancel;
    }

    private TaskExecutorJob pollJob() {
        mainLock.lock();
        try {
            return jobs.isEmpty() ? null : jobs.remove(0);
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Removes a single task from the task queue of this executor and executes
     * it (along with its completion handler) unless the queue is empty.
     * <P>
     * Note: Exceptions thrown by the submitted task will not be rethrown but
     * can be detected via the associated {@code CompletionStage}.
     *
     * @return {@code true} if there was a task to be executed in the queue,
     *   {@code false} if this method did nothing because there was no submitted
     *   task
     */
    public boolean tryExecuteOne() {
        TaskExecutorJob job = pollJob();
        if (job != null) {
            job.execute();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Executes every currently submitted task found in the task queue. The
     * tasks are executed in FIFO order. This method will collect tasks from the
     * queue once, if a task is submitted after, it will <I>not</I> be executed.
     * In particular, new tasks submitted by currently executed tasks are
     * guaranteed <I>not</I> to be executed by this method.
     *
     * <blockquote><pre>{@code
     * ManualTaskExecutor executor = new ManualTaskExecutor(false);
     * executor.execute(Cancellation.UNCANCELABLE_TOKEN, (outerCancelToken) -> {
     *   System.out.println("OUTER-TASK");
     *   executor.execute(Cancellation.UNCANCELABLE_TOKEN, (innerCancelToken) -> {
     *     System.out.println("INNER-TASK");
     *   });
     * });
     * executor.executeCurrentlySubmitted();
     * }</pre></blockquote>
     *
     * The above code will only print "OUTER-TASK" and the
     * {@code executeCurrentlySubmitted} method will return 1.
     *
     * @return the number of tasks executed by this method. This includes
     *   canceled tasks as well of which only the completion handler is executed.
     */
    public int executeCurrentlySubmitted() {
        TaskExecutorJob[] currentJobs;
        mainLock.lock();
        try {
            currentJobs = jobs.toArray(new TaskExecutorJob[jobs.size()]);
            jobs.clear();
        } finally {
            mainLock.unlock();
        }

        TaskExecutionException toThrow = null;
        for (TaskExecutorJob job : currentJobs) {
            try {
                job.execute();
            } catch (Throwable ex) {
                if (toThrow == null) toThrow = new TaskExecutionException(ex);
                else toThrow.addSuppressed(ex);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
        return currentJobs.length;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>:
     * Submits the specified task for execution. The submitted task can be
     * executed by subsequent calls to {@code executeCurrentlySubmitted}
     * or {@code tryExecuteOne} methods.
     * <P>
     * This executor implementation ensures that the same
     * {@code CancellationToken} is passed to the task as passed in the
     * argument of the {@code execute} method.
     */
    @Override
    protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
        TaskExecutorJob job = new TaskExecutorJob(cancelToken, submittedTask);

        if (eagerCancel) {
            ListenerRef cancelListenerRef = cancelToken.addCancellationListener(submittedTask::cancel);
            submittedTask.getFuture().whenComplete((result, error) -> {
                cancelListenerRef.unregister();
            });
        }

        mainLock.lock();
        try {
            jobs.add(job);
        } finally {
            mainLock.unlock();
        }
    }

    private static final class TaskExecutorJob {
        private final CancellationToken cancelToken;
        private final SubmittedTask<?> submittedTask;

        public TaskExecutorJob(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            this.cancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
            this.submittedTask = Objects.requireNonNull(submittedTask, "submittedTask");
        }

        public void execute() {
            submittedTask.execute(cancelToken);
        }
    }
}
