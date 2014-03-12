package org.jtrim.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.ObjectFinalizer;

/**
 * A {@link TaskExecutorService} implementation which executes submitted tasks
 * on a group of threads. This implementation is similar to the
 * {@code java.util.concurrent.ThreadPoolExecutor} in Java but implements
 * {@code TaskExecutorService} instead of {@code ExecutorService}.
 *
 * <h3>Executing new tasks</h3>
 * Tasks can be submitted by one of the {@code submit} or {@code execute}
 * methods.
 * <P>
 * When a new task is submitted, {@code ThreadPoolTaskExecutor} and there is an
 * idle thread waiting for tasks to be executed, an attempt will be made to
 * execute the submitted task on an idle thread and no new thread will be
 * started. This attempt fails only rarely under extreme contention, in which
 * case a new thread is started even though there was an idle thread.
 * <P>
 * In case there is no idle thread waiting and a new thread can be started
 * without exceeding the maximum number of allowed threads, a new thread will be
 * started to execute the submitted task.
 * <P>
 * In case there is no idle thread waiting and there are already as many threads
 * executing tasks as allowed, the submitted task will be added to an internal
 * queue from which the background threads will eventually remove and execute
 * them. Note that the size of the queue can be limited and if this limit is
 * reached, the submitting {@code submit} or {@code execute} method will block
 * and wait until the task can be added to the queue.
 *
 * <h3>Cancellation of tasks</h3>
 * Canceling a task which was not yet started and is still in the queue will
 * immediately remove it from the queue unless it has a cleanup task associated
 * with it. If it has an associated cleanup task, its cleanup task will remain
 * in the queue to be executed but the the task itself will be canceled (and its
 * state will signal {@link TaskState#DONE_CANCELED}) and no references will be
 * retained to the task (allowing it to be garbage collected if not referenced
 * by external code).
 * <P>
 * Canceling a task will cause the {@link CancellationToken} passed to it,
 * signal cancellation request. In this case the task may decide if it is to be
 * canceled or not. If the task throws an {@link OperationCanceledException},
 * the state of the task will be {@link TaskState#DONE_CANCELED}. Note that if
 * the task throws an {@code OperationCanceledException} it is always assumed to
 * be canceled, even if the {@code CancellationToken} does not signal a
 * cancellation request.
 *
 * <h3>Number of referenced tasks</h3>
 * The maximum number of tasks referenced by a {@code ThreadPoolTaskExecutor}
 * at any given time is the maximum size of its queue plus the maximum number of
 * allowed threads. The {@code ThreadPoolTaskExecutor} will never reference
 * tasks more than this. Note however, that not yet returned {@code submit}
 * or {@code execute} methods always reference their task specified in their
 * argument (obviously this is unavoidable) and there is no limit on how many
 * times the user can concurrently call these methods.
 *
 * <h3>Terminating {@code ThreadPoolTaskExecutor}</h3>
 * The {@code ThreadPoolTaskExecutor} must always be shut down when no longer
 * needed, so that it may shutdown its threads. If the user fails to shut down
 * the {@code ThreadPoolTaskExecutor} (either by calling {@link #shutdown()} or
 * {@link #shutdownAndCancel()}) and the garbage collector notifies the
 * {@code ThreadPoolTaskExecutor} that it has become unreachable (through
 * finalizers), it will be logged as an error using the logging facility of Java
 * (in a {@code Level.SEVERE} log message).
 * <P>
 * The {@link TaskExecutorService} requires every implementation to execute
 * cleanup tasks in every case. Therefore this must be done even after, the
 * {@code ThreadPoolTaskExecutor} has terminated. If a task is submitted after
 * the {@code ThreadPoolTaskExecutor} has been shut down, the submitted task will
 * not be executed but its cleanup task will be executed normally as if it was
 * submitted before shutdown with a no-op task. Apart from this, the main
 * difference is between submitting tasks prior and after termination is that,
 * started thread will never go idle after termination. They will always
 * terminate immediately after there are no cleanup tasks for them to execute.
 *
 * <h3>Comparison with ThreadPoolExecutor</h3>
 * <table border="1">
 *  <caption>Table for quick feature comparison</caption>
 *  <tr>
 *   <th>Feature</th>
 *   <th>ThreadPoolTaskExecutor</th>
 *   <th>ThreadPoolExecutor</th>
 *  </tr>
 *  <tr>
 *   <td>Immediate cancellation</td>
 *   <td>Yes, when there is no cleanup task.</td>
 *   <td>No, tasks remain in the queue until attempted to be executed.</td>
 *  </tr>
 *  <tr>
 *   <td>Cancellation strategy of executing tasks</td>
 *   <td>Relies on a {@link CancellationToken}.</td>
 *   <td>Relies on thread interrupts.</td>
 *  </tr>
 *  <tr>
 *   <td>Tracking the state of a submitted task</td>
 *   <td>Possible using the returned {@link TaskFuture}.</td>
 *   <td>Possible using the returned {@code Future}.</td>
 *  </tr>
 *  <tr>
 *   <td>Waiting until the task finished executing</td>
 *   <td>Possible using the returned {@link TaskFuture}.</td>
 *   <td>Not possible, if task was canceled by {@code shutdownNow}.</td>
 *  </tr>
 *  <tr>
 *   <td>Do cleanup even if task was canceled</td>
 *   <td>Yes</td>
 *   <td>No, only if the task was refused when submitting.</td>
 *  </tr>
 *  <tr>
 *   <td>User defined thread factory</td>
 *   <td>Yes</td>
 *   <td>Yes</td>
 *  </tr>
 *  <tr>
 *   <td>Automatically stop idle threads</td>
 *   <td>Yes</td>
 *   <td>Yes</td>
 *  </tr>
 *  <tr>
 *   <td>Limit the number of threads</td>
 *   <td>Yes</td>
 *   <td>Yes</td>
 *  </tr>
 *  <tr>
 *   <td>New thread start policy</td>
 *   <td>
 *    Starts a new thread only if there are no idle threads and the maximum
 *    number of threads was not reached.
 *   </td>
 *   <td>
 *    Starts a new thread always unless the maximum number of threads was
 *    reached.
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>Increase number of threads over the limit, if queue is full</td>
 *   <td>No</td>
 *   <td>Yes and configurable</td>
 *  </tr>
 *  <tr>
 *   <td>User defined implementation for the internal queue</td>
 *   <td>No</td>
 *   <td>Yes</td>
 *  </tr>
 *  <tr>
 *   <td>Throttle, when the task queue is large</td>
 *   <td>Yes</td>
 *   <td>Yes, with some limitations</td>
 *  </tr>
 *  <tr>
 *   <td>Asynchronous notification of termination</td>
 *   <td>Yes</td>
 *   <td>Only to subclasses</td>
 *  </tr>
 *  <tr>
 *   <td>Shut down and cancel submitted tasks</td>
 *   <td>Yes</td>
 *   <td>Yes</td>
 *  </tr>
 * </table>
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safely accessible from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Method of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @author Kelemen Attila
 */
public final class ThreadPoolTaskExecutor
extends
        DelegatedTaskExecutorService
implements
        MonitorableTaskExecutorService {

    private static final Logger LOGGER = Logger.getLogger(ThreadPoolTaskExecutor.class.getName());
    private static final long DEFAULT_THREAD_TIMEOUT_MS = 5000;

    private final ObjectFinalizer finalizer;
    private final ThreadPoolTaskExecutorImpl impl;

    /**
     * Creates a new {@code ThreadPoolTaskExecutor} initialized with specified
     * name.
     * <P>
     * The default maximum number of threads is
     * {@code Runtime.getRuntime().availableProcessors()}.
     * <P>
     * The default maximum queue size is {@code Integer.MAX_VALUE} making it
     * effectively unbounded.
     * <P>
     * The default timeout value after idle threads stop is 5 seconds.
     * <P>
     * The newly created {@code ThreadPoolTaskExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     *
     * @param poolName the name of this {@code ThreadPoolTaskExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if the specified name for this
     *   {@code ThreadPoolTaskExecutor} is {@code null}
     */
    public ThreadPoolTaskExecutor(String poolName) {
        this(poolName, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a new {@code ThreadPoolTaskExecutor} initialized with the given
     * properties.
     * <P>
     * The default maximum queue size is {@code Integer.MAX_VALUE} making it
     * effectively unbounded.
     * <P>
     * The default timeout value after idle threads stop is 5 seconds.
     * <P>
     * The newly created {@code ThreadPoolTaskExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     *
     * @param poolName the name of this {@code ThreadPoolTaskExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     * @param maxThreadCount the maximum number of threads to be executing
     *   submitted tasks concurrently. The {@code ThreadPoolTaskExecutor} will
     *   never execute more tasks (including cleanup tasks) concurrently as this
     *   number. This argument must be greater than or equal to 1.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if the specified name for this
     *   {@code ThreadPoolTaskExecutor} is {@code null}
     */
    public ThreadPoolTaskExecutor(String poolName, int maxThreadCount) {
        this(poolName, maxThreadCount, Integer.MAX_VALUE);
    }

    /**
     * Creates a new {@code ThreadPoolTaskExecutor} initialized with the given
     * properties.
     * <P>
     * The default timeout value after idle threads stop is 5 seconds.
     * <P>
     * The newly created {@code ThreadPoolTaskExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     *
     * @param poolName the name of this {@code ThreadPoolTaskExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     * @param maxThreadCount the maximum number of threads to be executing
     *   submitted tasks concurrently. The {@code ThreadPoolTaskExecutor} will
     *   never execute more tasks (including cleanup tasks) concurrently as this
     *   number. This argument must be greater than or equal to 1.
     * @param maxQueueSize the maximum size of the internal queue to store tasks
     *   not yet executed due to all threads being busy executing tasks. This
     *   argument must be greater than or equal to 1 and is recommended to be
     *   (but not required) greater than or equal to {@code maxThreadCount}.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if the specified name for this
     *   {@code ThreadPoolTaskExecutor} is {@code null}
     */
    public ThreadPoolTaskExecutor(
            String poolName,
            int maxThreadCount,
            int maxQueueSize) {

        this(poolName, maxThreadCount, maxQueueSize,
                DEFAULT_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new {@code ThreadPoolTaskExecutor} initialized with the given
     * properties.
     * <P>
     * The newly created {@code ThreadPoolTaskExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     *
     * @param poolName the name of this {@code ThreadPoolTaskExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     * @param maxThreadCount the maximum number of threads to be executing
     *   submitted tasks concurrently. The {@code ThreadPoolTaskExecutor} will
     *   never execute more tasks (including cleanup tasks) concurrently as this
     *   number. This argument must be greater than or equal to 1.
     * @param maxQueueSize the maximum size of the internal queue to store tasks
     *   not yet executed due to all threads being busy executing tasks. This
     *   argument must be greater than or equal to 1 and is recommended to be
     *   (but not required) greater than or equal to {@code maxThreadCount}.
     * @param idleTimeout the time in the given time unit after idle threads
     *   should stop. That is if a thread goes idle (i.e.: there are no
     *   submitted tasks), it will wait this amount of time before giving up
     *   waiting for submitted tasks. The thread may be restarted if needed
     *   later. It is recommended to use a reasonably low value for this
     *   argument (but not too low), so even if this
     *   {@code ThreadPoolTaskExecutor} has not been shut down (due to a bug),
     *   threads will still terminate allowing the JVM to terminate as well (if
     *   there are no more non-daemon threads). This argument must be greater
     *   than or equal to zero.
     * @param timeUnit the time unit of the {@code idleTimeout} argument. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public ThreadPoolTaskExecutor(
            String poolName,
            int maxThreadCount,
            int maxQueueSize,
            long idleTimeout,
            TimeUnit timeUnit) {
        this(new ThreadPoolTaskExecutorImpl(
                poolName, maxThreadCount, maxQueueSize, idleTimeout, timeUnit));

    }

    private ThreadPoolTaskExecutor(final ThreadPoolTaskExecutorImpl impl) {
        super(impl);
        this.impl = impl;
        this.finalizer = new ObjectFinalizer(new Runnable() {
            @Override
            public void run() {
                impl.shutdown();
            }
        }, impl.getPoolName() + " ThreadPoolTaskExecutor shutdown");
    }

    /**
     * Specifies that this {@code ThreadPoolTaskExecutor} does not need to be
     * shut down. Calling this method prevents this executor to be shut
     * down automatically when there is no more reference to this executor,
     * which also prevents logging a message if this executor has not been shut
     * down. This method might be called if you do not plan to shutdown this
     * executor but instead want to rely on the threads of this executor to
     * automatically shutdown after a <I>small</I> timeout.
     */
    public void dontNeedShutdown() {
        finalizer.markFinalized();
    }

    /**
     * Sets the {@code ThreadFactory} which is used to create worker threads
     * for this executor. Already started workers are not affected by this
     * method call but workers created after this method call will use the
     * currently set {@code ThreadFactory}.
     * <P>
     * It is recommended to call this method before submitting any task to this
     * executor. Doing so guarantees that all worker threads of this executor
     * will be created by the specified {@code ThreadFactory}.
     * <P>
     * The default {@code ThreadFactory} used by this executor is a
     * {@link ExecutorsEx.NamedThreadFactory} creating non-daemon threads.
     *
     * @param threadFactory the {@code ThreadFactory} which is used to create
     *   worker threads for this executor. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified thread factory is
     *   {@code null}
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        impl.setThreadFactory(threadFactory);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        finalizer.markFinalized();
        wrappedExecutor.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdownAndCancel() {
        finalizer.markFinalized();
        wrappedExecutor.shutdownAndCancel();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public long getNumberOfQueuedTasks() {
        return impl.getNumberOfQueuedTasks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public long getNumberOfExecutingTasks() {
        return impl.getNumberOfExecutingTasks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isExecutingInThis() {
        return impl.isExecutingInThis();
    }

    /**
     * Sets the maximum number of threads allowed to execute submitted
     * tasks (and cleanup tasks) concurrently.
     * <P>
     * Setting this property may not have an immediate effect.
     * Setting it to a higher value as was set previously, will not cause new
     * thread to be started if the internal queue is not empty but subsequent
     * {@code submit} and {@code execute} methods will detect that they can
     * start new threads. Setting this property to a lower value as was set
     * previously, will not cause threads to stop even if they are currently
     * idle. It just prevents new threads to be created until the number of
     * currently running threads drops below this limit (as threads stop due
     * to too long idle time).
     * <P>
     * Note that setting this property before a task was submitted to this
     * {@code ThreadPoolTaskExecutor} is guaranteed to have immediate effect.
     *
     * @param maxThreadCount the maximum number of threads allowed to be
     *   executing submitted tasks (and cleanup tasks) concurrently. This
     *   argument must be greater than or equal to 1.
     *
     * @throws IllegalArgumentException if the specified {@code maxThreadCount}
     *   is less than 1
     */
    public void setMaxThreadCount(int maxThreadCount) {
        impl.setMaxThreadCount(maxThreadCount);
    }

    /**
     * Returns the currently set maximum thread to be used by this executor.
     * <P>
     * The return value of this method is for information purpose only. Due to
     * concurrent sets and already started threads, there is no guarantee that
     * the return value is truly being honored at the moment. See
     * {@link #setMaxThreadCount(int) setMaxThreadCount} for details on how this
     * property works.
     *
     * @return the currently set maximum thread to be used by this executor.
     *   This value is always greater than or equal to one.
     */
    public int getMaxThreadCount() {
        return impl.maxThreadCount;
    }

    /**
     * Sets the maximum number of tasks allowed to be stored in the internal
     * queue.
     * <P>
     * Setting this property higher than it was set previously will have an
     * immediate effect and currently blocking {@code submit} and
     * {@code execute} will recheck if they can add the submitted task to the
     * queue. Setting this property lower, however, will not remove tasks from
     * the queue but will prevent more tasks to be added to the queue before
     * the number of tasks in the queue drops below this limit.
     *
     * @param maxQueueSize the maximum number of tasks allowed to be stored in
     *   the internal queue. This argument must be greater than or equal to 1.
     *
     * @throws IllegalArgumentException if the specified {@code maxQueueSize}
     *   is less than 1
     */
    public void setMaxQueueSize(int maxQueueSize) {
        impl.setMaxQueueSize(maxQueueSize);
    }

    /**
     * Returns the currently set maximum size for the queue of tasks scheduled
     * to be executed.
     * <P>
     * The return value of this method is for information purpose only. Due to
     * concurrent sets and already queued tasks, there is no guarantee that
     * the return value is truly being honored at the moment. See
     * {@link #setMaxQueueSize(int) setMaxQueueSize} for details on how this
     * property works.
     *
     * @return the currently set maximum size for the queue of tasks scheduled
     *   to be executed. This value is always greater than or equal to one.
     */
    public int getMaxQueueSize() {
        return impl.maxQueueSize;
    }

    /**
     * Sets the timeout value after idle threads should terminate. That is,
     * threads will terminate if they waited for at least this much time and
     * there was no submitted task for them to execute.
     * <P>
     * Setting this property has an immediate effect.
     *
     * @param idleTimeout the timeout value in the given time unit after idle
     *   threads should terminate. This argument must be greater than or equal
     *   to zero.
     * @param timeUnit the time unit of the {@code idleTimeout} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   less than zero
     * @throws NullPointerException thrown if the specified time unit argument
     *   is {@code null}
     */
    public void setIdleTimeout(long idleTimeout, TimeUnit timeUnit) {
        impl.setIdleTimeout(idleTimeout, timeUnit);
    }

    /**
     * Returns the currently set timeout value after idle threads should stop.
     * <P>
     * The return value of this method is for information purpose only. Due to
     * concurrent sets, there is no guarantee that the return value is truly
     * being honored at the moment. See {@link #setIdleTimeout(long, TimeUnit) setIdleTimeout}
     * for details on how this property works.
     *
     * @param timeUnit the time unit in which the result is request. This
     *   argument cannot be {@code null}.
     * @return the currently set timeout value after idle threads should stop.
     *   The return value might not be exactly what was set by the previous
     *   invocation to {@code setIdleTimeout} due to rounding errors. This
     *   method always returns a values greater than or equal to zero.
     *
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public long getIdleTimeout(TimeUnit timeUnit) {
        return timeUnit.convert(impl.idleTimeoutNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the name of this {@code ThreadPoolTaskExecutor} as specified at
     * construction time.
     *
     * @return the name of this {@code ThreadPoolTaskExecutor} as specified at
     *   construction time. This method never returns {@code null}.
     */
    public String getPoolName() {
        return impl.getPoolName();
    }

    /**
     * Returns the string representation of this executor in no particular
     * format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return impl.toString();
    }

    private static final class ThreadPoolTaskExecutorImpl
    extends
            AbstractTerminateNotifierTaskExecutorService
    implements
            MonitorableTaskExecutor {
        private static final ThreadLocal<ThreadPoolTaskExecutorImpl> OWNER_EXECUTOR = new ThreadLocal<>();

        private final String poolName;

        private volatile int maxQueueSize;
        private volatile long idleTimeoutNanos;
        private volatile int maxThreadCount;

        private final Lock mainLock;

        private int idleWorkerCount;

        // activeWorkerCount counts workers which may execute tasks and not only
        // cleanup tasks.
        private int activeWorkerCount;
        private int runningWorkerCount;
        private final RefList<QueuedItem> queue; // the oldest task is the head of the queue
        // Canceled when shutdownAndCancel is called.
        private final CancellationSource executorCancelSource;
        private final Condition notFullQueueSignal;
        private final Condition checkQueueSignal;
        private final Condition terminateSignal;
        private volatile ExecutorState state;
        private ThreadFactory threadFactory;
        private final AtomicInteger currentlyExecuting;

        public ThreadPoolTaskExecutorImpl(
                String poolName,
                int maxThreadCount,
                int maxQueueSize,
                long idleTimeout,
                TimeUnit timeUnit) {

            ExceptionHelper.checkNotNullArgument(poolName, "poolName");
            ExceptionHelper.checkArgumentInRange(maxThreadCount, 1, Integer.MAX_VALUE, "maxThreadCount");
            ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");
            ExceptionHelper.checkArgumentInRange(idleTimeout, 0, Long.MAX_VALUE, "idleTimeout");
            ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

            this.poolName = poolName;
            this.idleWorkerCount = 0;
            this.executorCancelSource = Cancellation.createCancellationSource();
            this.maxThreadCount = maxThreadCount;
            this.maxQueueSize = maxQueueSize;
            this.idleTimeoutNanos = timeUnit.toNanos(idleTimeout);
            this.state = ExecutorState.RUNNING;
            this.activeWorkerCount = 0;
            this.runningWorkerCount = 0;
            this.queue = new RefLinkedList<>();
            this.mainLock = new ReentrantLock();
            this.notFullQueueSignal = mainLock.newCondition();
            this.checkQueueSignal = mainLock.newCondition();
            this.terminateSignal = mainLock.newCondition();
            this.currentlyExecuting = new AtomicInteger();
            this.threadFactory = new ExecutorsEx.NamedThreadFactory(false, poolName);
        }

        @Override
        public long getNumberOfQueuedTasks() {
            mainLock.lock();
            try {
                return queue.size();
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public long getNumberOfExecutingTasks() {
            return currentlyExecuting.get();
        }

        @Override
        public boolean isExecutingInThis() {
            ThreadPoolTaskExecutorImpl owner = OWNER_EXECUTOR.get();
            if (owner == null) {
                OWNER_EXECUTOR.remove();
                return false;
            }
            return owner == this;
        }

        public void setThreadFactory(ThreadFactory threadFactory) {
            ExceptionHelper.checkNotNullArgument(threadFactory, "threadFactory");
            this.threadFactory = threadFactory;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setMaxThreadCount(int maxThreadCount) {
            ExceptionHelper.checkArgumentInRange(maxThreadCount, 1, Integer.MAX_VALUE, "maxThreadCount");
            this.maxThreadCount = maxThreadCount;
        }

        public void setMaxQueueSize(int maxQueueSize) {
            ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");
            this.maxQueueSize = maxQueueSize;
            mainLock.lock();
            try {
                // Actually it might be full but awaking all the waiting threads
                // cause only a performance loss. This performance loss is of
                // little consequence becuase we don't expect this method to be
                // called that much.
                notFullQueueSignal.signalAll();
            } finally {
                mainLock.unlock();
            }
        }

        public void setIdleTimeout(long idleTimeout, TimeUnit timeUnit) {
            ExceptionHelper.checkArgumentInRange(idleTimeout, 0, Long.MAX_VALUE, "idleTimeout");
            this.idleTimeoutNanos = timeUnit.toNanos(idleTimeout);
            mainLock.lock();
            try {
                checkQueueSignal.signalAll();
            } finally {
                mainLock.unlock();
            }
        }

        private void setRemoveFromQueueOnCancel(
                final QueuedItem task,
                final RefCollection.ElementRef<?> queueRef) {
            final ListenerRef cancelRef = task.cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    boolean removed;
                    mainLock.lock();
                    try {
                        removed = queueRef.isRemoved();
                        if (!removed) {
                            queueRef.remove();
                        }
                    } finally {
                        mainLock.unlock();
                    }

                    if (!removed) {
                        try {
                            task.cleanup();
                        } finally {
                            tryTerminateAndNotify();
                        }
                    }
                }
            });

            task.addNewCleanupTask(new Runnable() {
                @Override
                public void run() {
                    cancelRef.unregister();
                }
            });
        }

        @Override
        protected void submitTask(
                CancellationToken cancelToken,
                CancelableTask task,
                Runnable cleanupTask,
                boolean hasUserDefinedCleanup) {

            CancellationToken combinedToken = Cancellation.anyToken(
                    cancelToken, executorCancelSource.getToken());

            QueuedItem newItem = new QueuedItem(combinedToken, task, cleanupTask);

            CancellationToken waitQueueCancelToken = hasUserDefinedCleanup
                    ? Cancellation.UNCANCELABLE_TOKEN
                    : combinedToken;

            final RefCollection.ElementRef<?> queueRef;
            queueRef = submitQueueItem(newItem, waitQueueCancelToken);

            // If we do not have a user defined cleanup, then when a task gets
            // canceled we can immediately remove the item from the queue because
            // then we may execute the cleanup task wherever we desire (in this
            // case, in the cancellation listener).
            if (!hasUserDefinedCleanup && queueRef != null) {
                setRemoveFromQueueOnCancel(newItem, queueRef);
            }
        }

        private RefCollection.ElementRef<?> submitQueueItem(
                QueuedItem newItem,
                CancellationToken waitQueueCancelToken) {

            try {
                mainLock.lock();
                try {
                    // Do not start a new thread if there is enough idle thread
                    // to handle the tasks in the queue.
                    int currentQueueSize = queue.size();
                    if (idleWorkerCount > currentQueueSize
                            && currentQueueSize < maxQueueSize) {

                        RefCollection.ElementRef<?> queueRef;
                        queueRef = queue.addLastGetReference(newItem);
                        checkQueueSignal.signal();
                        return queueRef;
                    }
                } finally {
                    mainLock.unlock();
                }

                while (true) {
                    Worker newWorker = new Worker();
                    if (newWorker.tryStartWorker(newItem)) {
                        return null;
                    }
                    else {
                        mainLock.lock();
                        try {
                            if (runningWorkerCount != 0) {
                                if (queue.size() < maxQueueSize) {
                                    RefCollection.ElementRef<?> queueRef;
                                    queueRef = queue.addLastGetReference(newItem);
                                    checkQueueSignal.signal();
                                    return queueRef;
                                }
                                else {
                                    CancelableWaits.await(waitQueueCancelToken, notFullQueueSignal);
                                }
                            }
                        } finally {
                            mainLock.unlock();
                        }
                    }
                }
            } catch (OperationCanceledException ex) {
                // Notice that this can only be thrown if there
                // is no user defined cleanup task and only when waiting for the
                // queue to allow adding more elements.
                newItem.cleanup();
            }
            return null;
        }

        @Override
        public void shutdown() {
            mainLock.lock();
            try {
                if (state == ExecutorState.RUNNING) {
                    state = ExecutorState.SHUTTING_DOWN;
                    checkQueueSignal.signalAll();
                }
            } finally {
                mainLock.unlock();
                tryTerminateAndNotify();
            }
        }

        private void setTerminating() {
            mainLock.lock();
            try {
                if (!isTerminating()) {
                    state = ExecutorState.TERMINATING;
                }
                // All the workers must wake up, so that they can detect that
                // the executor is shutting down and so must they.
                checkQueueSignal.signalAll();
            } finally {
                mainLock.unlock();
                tryTerminateAndNotify();
            }
        }

        @Override
        public void shutdownAndCancel() {
            // First, stop allowing submitting anymore tasks.
            shutdown();

            setTerminating();
            executorCancelSource.getController().cancel();
        }

        @Override
        public boolean isShutdown() {
            return state.getStateIndex() >= ExecutorState.SHUTTING_DOWN.getStateIndex();
        }

        private boolean isTerminating() {
            return state.getStateIndex() >= ExecutorState.TERMINATING.getStateIndex();
        }

        @Override
        public boolean isTerminated() {
            return state == ExecutorState.TERMINATED;
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            if (!isTerminated()) {
                long startTime = System.nanoTime();
                long timeoutNanos = unit.toNanos(timeout);
                mainLock.lock();
                try {
                    while (!isTerminated()) {
                        long elapsed = System.nanoTime() - startTime;
                        long toWaitNanos = timeoutNanos - elapsed;
                        if (toWaitNanos <= 0) {
                            return false;
                        }
                        CancelableWaits.await(cancelToken,
                                toWaitNanos, TimeUnit.NANOSECONDS, terminateSignal);
                    }
                } finally {
                    mainLock.unlock();
                }
            }
            return true;
        }

        private void tryTerminateAndNotify() {
            if (tryTerminate()) {
                notifyTerminateListeners();
            }
        }

        private boolean tryTerminate() {
            if (isShutdown() && state != ExecutorState.TERMINATED) {
                mainLock.lock();
                try {
                    // Notice that if activeWorkerCount is not zero, we will
                    // call this method. This is easy to prove because everywhere
                    // where activeWorkerCount is decremented, this method is
                    // called after it.
                    if (isShutdown() && activeWorkerCount == 0 && queue.isEmpty()) {
                        if (state != ExecutorState.TERMINATED) {
                            state = ExecutorState.TERMINATED;
                            terminateSignal.signalAll();
                            return true;
                        }
                    }
                } finally {
                    mainLock.unlock();
                }
            }
            return false;
        }

        private void writeLog(Level level, String prefix, Throwable ex) {
            if (LOGGER.isLoggable(level)) {
                LOGGER.log(level, prefix + " in the " + poolName
                        + " ThreadPoolTaskExecutor", ex);
            }
        }

        @Override
        public String toString() {
            int currentIdleWorkerCount;
            int currentActiveWorkerCount;
            int currentRunningWorkerCount;
            int currentQueueSize;
            mainLock.lock();
            try {
                currentIdleWorkerCount = idleWorkerCount;
                currentActiveWorkerCount = activeWorkerCount;
                currentRunningWorkerCount = runningWorkerCount;
                currentQueueSize = queue.size();
            } finally {
                mainLock.unlock();
            }
            return "ThreadPoolTaskExecutor{"
                    + "poolName=" + poolName
                    + ", state=" + state
                    + ", maxQueueSize=" + maxQueueSize
                    + ", idleTimeout=" + TimeUnit.NANOSECONDS.toMillis(idleTimeoutNanos) + " ms"
                    + ", maxThreadCount=" + maxThreadCount
                    + ", idleWorkerCount=" + currentIdleWorkerCount
                    + ", activeWorkers=" + currentActiveWorkerCount
                    + ", runningWorkers=" + currentRunningWorkerCount
                    + ", queue=" + currentQueueSize + '}';
        }

        private class Worker implements Runnable {
            // if activeWorkerCount has been incremented by the worker
            private boolean incActiveWorkerCount;
            // if runningWorkerCount has been incremented by the worker
            private boolean incRunningWorkerCount;

            private QueuedItem firstTask;
            private Thread ownerThread;
            private final AtomicBoolean runCalled;

            public Worker() {
                this.firstTask = null;
                this.incActiveWorkerCount = false;
                this.incRunningWorkerCount = false;
                this.runCalled = new AtomicBoolean(false);
            }

            private Thread createWorkerThread(Runnable task) {
                return threadFactory.newThread(task);
            }

            private Thread createOwnedWorkerThread(final Runnable task) {
                assert task != null;
                return createWorkerThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OWNER_EXECUTOR.set(ThreadPoolTaskExecutorImpl.this);
                            task.run();
                        } finally {
                            OWNER_EXECUTOR.remove();
                        }
                    }
                });
            }

            // May only be called once and must be the first method call
            // after creating this Worker instance.
            public boolean tryStartWorker(QueuedItem firstTask) {
                mainLock.lock();
                try {
                    assert !incRunningWorkerCount && !incActiveWorkerCount;

                    if (firstTask == null && queue.isEmpty()) {
                        return false;
                    }

                    if (runningWorkerCount >= maxThreadCount) {
                        return false;
                    }

                    if (!isTerminated()) {
                        activeWorkerCount++;
                        incActiveWorkerCount = true;
                    }
                    runningWorkerCount++;
                    incRunningWorkerCount = true;
                } finally {
                    mainLock.unlock();
                }

                try {
                    this.firstTask = firstTask;
                    Thread newThread = createOwnedWorkerThread(this);

                    ownerThread = newThread;
                    newThread.start();
                } catch (Throwable ex) {
                    finishWorkingAndTryTerminate();
                    throw ex;
                }
                return true;
            }

            private void executeTask(QueuedItem item) throws Exception {
                currentlyExecuting.incrementAndGet();
                try {
                    try {
                        if (!isTerminating()) {
                            item.runTask();
                        }
                    } finally {
                        item.cleanup();
                    }
                } finally {
                    currentlyExecuting.decrementAndGet();
                }
            }

            private void restartIfNeeded() {
                Worker newWorker = null;
                mainLock.lock();
                try {
                    if (!queue.isEmpty()) {
                        newWorker = new Worker();
                    }
                } finally {
                    mainLock.unlock();
                }

                if (newWorker != null) {
                    newWorker.tryStartWorker(null);
                }
            }

            private void finishWorkingAndTryTerminate() {
                try {
                    finishWorking();
                } finally {
                    tryTerminateAndNotify();
                }
            }

            private void finishWorking() {
                mainLock.lock();
                try {
                    if (incActiveWorkerCount) {
                        activeWorkerCount--;
                        incActiveWorkerCount = false;
                    }
                    if (incRunningWorkerCount) {
                        runningWorkerCount--;
                        incRunningWorkerCount = false;
                    }
                } finally {
                    mainLock.unlock();
                }
            }

            private QueuedItem pollFromQueue() {
                long startTime = System.nanoTime();
                long usedIdleTimeoutNanos = idleTimeoutNanos;
                long toWaitNanos = usedIdleTimeoutNanos;
                mainLock.lock();
                try {
                    do {
                        if (!queue.isEmpty()) {
                            QueuedItem result = queue.remove(0);
                            notFullQueueSignal.signal();
                            return result;
                        }

                        if (isShutdown()) {
                            return null;
                        }

                        idleWorkerCount++;
                        try {
                            toWaitNanos = checkQueueSignal.awaitNanos(toWaitNanos);
                            if (usedIdleTimeoutNanos != idleTimeoutNanos) {
                                long inc;
                                inc = idleTimeoutNanos - usedIdleTimeoutNanos;
                                long prevToWaitNanos = toWaitNanos;
                                toWaitNanos += inc;
                                // Check for overflow in the above addition
                                if (inc > 0) {
                                    if (toWaitNanos < 0) {
                                        toWaitNanos = Long.MAX_VALUE;
                                    }
                                }
                                else {
                                    if (prevToWaitNanos < toWaitNanos) {
                                        toWaitNanos = 0;
                                    }
                                }
                            }
                        } catch (InterruptedException ex) {
                            // In this thread, we don't care about interrupts but
                            // we need to recalculate the allowed waiting time.
                            toWaitNanos = idleTimeoutNanos - (System.nanoTime() - startTime);
                        } finally {
                            idleWorkerCount--;
                        }
                    } while (toWaitNanos > 0);
                } finally {
                    mainLock.unlock();
                }
                return null;
            }

            private void processQueue() throws Exception {
                QueuedItem itemToProcess = pollFromQueue();
                while (itemToProcess != null) {
                    executeTask(itemToProcess);
                    itemToProcess = pollFromQueue();
                }
            }

            @Override
            public void run() {
                // Prevent abuse when calling from a ThreadFactory.
                if (Thread.currentThread() != ownerThread) {
                    LOGGER.log(Level.SEVERE,
                            "The worker of {0} has been called from the wrong thread.",
                            poolName);
                    throw new IllegalStateException();
                }

                // This may happen if the thread factory calls this task
                // multiple times from the started thread.
                if (!runCalled.compareAndSet(false, true)) {
                    LOGGER.log(Level.SEVERE,
                            "The worker of {0} has been called multiple times.",
                            poolName);
                    throw new IllegalStateException();
                }

                try {
                    if (firstTask != null) {
                        executeTask(firstTask);
                        firstTask = null;
                    }
                    processQueue();
                } catch (Throwable ex) {
                    writeLog(Level.SEVERE, "Unexpected exception", ex);
                } finally {
                    try {
                        finishWorkingAndTryTerminate();
                    } finally {
                        restartIfNeeded();
                    }
                }
            }
        }

        private static class QueuedItem {
            public final CancellationToken cancelToken;
            public final CancelableTask task;
            public final AtomicReference<Runnable> cleanupTaskRef;

            public QueuedItem(
                    CancellationToken cancelToken,
                    CancelableTask task,
                    Runnable cleanupTask) {

                this.cancelToken = cancelToken;
                this.task = task;
                this.cleanupTaskRef = new AtomicReference<>(cleanupTask);
            }

            public void runTask() throws Exception {
                if (!cancelToken.isCanceled()) {
                    clearInterrupt();
                    task.execute(cancelToken);
                }
            }

            public void addNewCleanupTask(final Runnable newTask) {
                final Runnable currentCleanupTask = cleanupTaskRef.get();
                if (currentCleanupTask == null) {
                    newTask.run();
                }

                Runnable newCleanupTask = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            currentCleanupTask.run();
                        } finally {
                            newTask.run();
                        }
                    }
                };

                if (!cleanupTaskRef.compareAndSet(currentCleanupTask, newCleanupTask)) {
                    newTask.run();
                }
            }

            private void cleanup() {
                clearInterrupt();

                Runnable cleanupTask = cleanupTaskRef.getAndSet(null);
                // This must never be null because we only call cleanup once.
                cleanupTask.run();
            }

            private void clearInterrupt() {
                Thread.interrupted();
            }
        }

        private enum ExecutorState {
            RUNNING(0),
            SHUTTING_DOWN(1),
            TERMINATING(2), // = tasks are to be canceled
            TERMINATED(3);

            private final int stateIndex;

            private ExecutorState(int stateIndex) {
                this.stateIndex = stateIndex;
            }

            public int getStateIndex() {
                return stateIndex;
            }
        }
    }
}
