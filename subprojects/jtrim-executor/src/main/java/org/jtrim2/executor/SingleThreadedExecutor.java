package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.ObjectFinalizer;
import org.jtrim2.utils.TimeDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@code TaskExecutorService} executing submitted tasks on a single
 * background thread. Tasks are guaranteed to be executed in a FIFO order and
 * they are never executed concurrently, so this class might be conveniently
 * used for synchronization purposes.
 * <P>
 * <B>Note</B>: Consider using {@link ThreadPoolBuilder} instead of directly creating
 * an instance of {@code ThreadPoolTaskExecutor}.
 *
 * <h2>Executing new tasks</h2>
 * Tasks can be submitted by one of the {@code execute} methods.
 * <P>
 * When a new task is submitted, {@code SingleThreadedExecutor} and the queue
 * of this executor is not full: The task is added to the queue. If the worker
 * thread of this executor has already been started, the worker will execute the
 * newly added task after it finishes executing previously submitted tasks.
 * <P>
 * If the queue for tasks is full, the {@code submit} or {@code execute} method
 * will block and wait until the task can be added to the queue.
 *
 * <h2>Cancellation of tasks</h2>
 * Canceling a task which was not yet started and is still in the queue will
 * immediately remove it from the queue and no references will be
 * retained to the task (allowing it to be garbage collected if not referenced
 * by external code).
 * <P>
 * Canceling a task will cause the {@link CancellationToken} passed to it,
 * signal cancellation request. In this case the task may decide if it is to be
 * canceled or not. If the task throws an {@link OperationCanceledException},
 * task execution is considered to be canceled. Note that if the task throws
 * an {@code OperationCanceledException} it is always assumed to be canceled,
 * even if the {@code CancellationToken} does not signal a cancellation request.
 *
 * <h2>Number of referenced tasks</h2>
 * The maximum number of tasks referenced by a {@code SingleThreadedExecutor}
 * at any given time is the maximum size of its queue plus one. The
 * {@code SingleThreadedExecutor} will never reference tasks more than this.
 * Note however, that not yet returned {@code submit} or {@code execute} methods
 * always reference their task specified in their argument (obviously this is
 * unavoidable) and there is no limit on how many times the user can
 * concurrently call these methods.
 *
 * <h2>Terminating {@code SingleThreadedExecutor}</h2>
 * The {@code SingleThreadedExecutor} must always be shut down when no longer
 * needed, so that it may shutdown its worker thread. If the user fails to
 * shut down the {@code SingleThreadedExecutor} (either by calling
 * {@link #shutdown()} or {@link #shutdownAndCancel()}) and the garbage
 * collector notifies the {@code SingleThreadedExecutor} that it has become
 * unreachable (through finalizers), it will be logged as an error using the
 * logging facility of Java (in a {@code Level.SEVERE} log message).
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safely accessible from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Method of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @see ThreadPoolBuilder
 */
public final class SingleThreadedExecutor
extends
        DelegatedTaskExecutorService
implements
        MonitorableTaskExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleThreadedExecutor.class);
    private static final long DEFAULT_THREAD_TIMEOUT_MS = 5000;

    private final ObjectFinalizer finalizer;
    private final Impl impl;

    /**
     * Creates a new {@code SingleThreadedExecutor} initialized with the
     * specified name.
     * <P>
     * The default maximum queue size is {@code Integer.MAX_VALUE} making it
     * effectively unbounded.
     * <P>
     * The default timeout value after idle threads stop is 5 seconds.
     * <P>
     * The newly created {@code SingleThreadedExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     * <P>
     * The default {@code ThreadFactory} will create non-daemon threads and
     * the name of the started threads will contain the name of this executor.
     * <P>
     * <B>Note</B>: Consider using {@link ThreadPoolBuilder} instead of directly creating
     * an instance of {@code ThreadPoolTaskExecutor}.
     *
     * @param poolName the name of this {@code SingleThreadedExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if the specified name for this
     *   {@code SingleThreadedExecutor} is {@code null}
     *
     * @see #setThreadFactory(ThreadFactory)
     */
    public SingleThreadedExecutor(String poolName) {
        this(poolName, Integer.MAX_VALUE);
    }

    /**
     * Creates a new {@code SingleThreadedExecutor} initialized with the given
     * properties.
     * <P>
     * The default timeout value after idle threads stop is 5 seconds.
     * <P>
     * The newly created {@code SingleThreadedExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     * <P>
     * The default {@code ThreadFactory} will create non-daemon threads and
     * the name of the started threads will contain the name of this executor.
     * <P>
     * <B>Note</B>: Consider using {@link ThreadPoolBuilder} instead of directly creating
     * an instance of {@code ThreadPoolTaskExecutor}.
     *
     * @param poolName the name of this {@code SingleThreadedExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     * @param maxQueueSize the maximum size of the internal queue to store tasks
     *   not yet executed due to the worker thread being busy executing tasks.
     *   This argument must greater than or equal to 1.
     *
     * @throws IllegalArgumentException thrown if an illegal value was specified
     *   for any of the {@code int} arguments
     * @throws NullPointerException thrown if the specified name for this
     *   {@code SingleThreadedExecutor} is {@code null}
     *
     * @see #setThreadFactory(ThreadFactory)
     */
    public SingleThreadedExecutor(String poolName, int maxQueueSize) {
        this(poolName, maxQueueSize, DEFAULT_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a new {@code SingleThreadedExecutor} initialized with the given
     * properties.
     * <P>
     * The newly created {@code SingleThreadedExecutor} will not have any thread
     * started. Threads will only be started when submitting tasks
     * (as required).
     * <P>
     * The default {@code ThreadFactory} will create non-daemon threads and
     * the name of the started threads will contain the name of this executor.
     * <P>
     * <B>Note</B>: Consider using {@link ThreadPoolBuilder} instead of directly creating
     * an instance of {@code ThreadPoolTaskExecutor}.
     *
     * @param poolName the name of this {@code SingleThreadedExecutor} for
     *   logging and debugging purposes. Setting a descriptive name might help
     *   when debugging or reading logs. This argument cannot be {@code null}.
     * @param maxQueueSize the maximum size of the internal queue to store tasks
     *   not yet executed due to the worker thread being busy executing tasks.
     *   This argument must greater than or equal to 1..
     * @param idleTimeout the time in the given time unit after idle threads
     *   should stop. That is if a thread goes idle (i.e.: there are no
     *   submitted tasks), it will wait this amount of time before giving up
     *   waiting for submitted tasks. The thread may be restarted if needed
     *   later. It is recommended to use a reasonably low value for this
     *   argument (but not too low), so even if this
     *   {@code SingleThreadedExecutor} is not shut down (due to a bug),
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
     *
     * @see #setThreadFactory(ThreadFactory)
     */
    public SingleThreadedExecutor(
            String poolName,
            int maxQueueSize,
            long idleTimeout,
            TimeUnit timeUnit) {

        this(poolName,
                maxQueueSize,
                new TimeDuration(idleTimeout, timeUnit),
                new ExecutorsEx.NamedThreadFactory(false, poolName)
        );
    }

    SingleThreadedExecutor(
            String poolName,
            int maxQueueSize,
            TimeDuration idleTimeout,
            ThreadFactory threadFactory) {

        this(new Impl(poolName, maxQueueSize, idleTimeout, threadFactory));
    }


    private SingleThreadedExecutor(final Impl impl) {
        super(impl);

        this.impl = impl;
        this.finalizer = new ObjectFinalizer(impl::shutdown, impl.getPoolName() + " SingleThreadedExecutor shutdown");
    }

    /**
     * Specifies that this {@code SingleThreadedExecutor} does not need to be
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
     * Sets the {@code ThreadFactory} which is used to create worker threads
     * for this executor. Already started worker threads are not affected by
     * this method call but workers created after this method call will use the
     * currently set {@code ThreadFactory}.
     * <P>
     * It is recommended to call this method before submitting any task to this
     * executor. Doing so guarantees that all worker threads of this executor
     * will be created by the specified {@code ThreadFactory}.
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
     * Returns the name of this {@code SingleThreadedExecutor} as specified at
     * construction time.
     *
     * @return the name of this {@code SingleThreadedExecutor} as specified at
     *   construction time. This method never returns {@code null}.
     */
    public String getPoolName() {
        return impl.getPoolName();
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
     * <P>
     * <B>Implementation note</B>: This method may only return zero or one.
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

    void setFullQueueHandler(FullQueueHandler fullQueueHandler) {
        impl.fullQueueHandler = fullQueueHandler;
    }

    FullQueueHandler getFullQueueHandler() {
        return impl.fullQueueHandler;
    }

    ThreadFactory getThreadFactory() {
        return impl.threadFactory;
    }

    boolean isFinalized() {
        return finalizer.isFinalized();
    }

    private static final class Impl
    extends
            AbstractTerminateNotifierTaskExecutorService
    implements
            MonitorableTaskExecutor {

        private static final ThreadLocal<Impl> OWNER_EXECUTOR = new ThreadLocal<>();

        private final AtomicReference<Worker> currentWorker;
        private final ReentrantLock mainLock;
        private final RefList<QueuedItem> taskQueue;
        private final String poolName;
        private volatile int maxQueueSize;
        private volatile long idleTimeoutNanos;
        private final CancellationSource globalCancel;
        private final WaitableSignal terminateSignal;
        private FullQueueHandler fullQueueHandler;
        private volatile ThreadFactory threadFactory;
        private volatile ExecutorState state;
        private volatile boolean active;

        private final Condition checkQueueSignal;
        private final Condition checkAddToQueueSignal;

        public Impl(
                String poolName,
                int maxQueueSize,
                TimeDuration idleTimeout,
                ThreadFactory threadFactory) {

            Objects.requireNonNull(poolName, "poolName");
            ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");

            this.state = ExecutorState.RUNNING;
            this.maxQueueSize = maxQueueSize;
            this.poolName = poolName;
            this.idleTimeoutNanos = ExceptionHelper.checkArgumentInRange(
                    idleTimeout.toNanos(),
                    0,
                    Long.MAX_VALUE,
                    "idleTimeout"
            );
            this.mainLock = new ReentrantLock();
            this.checkQueueSignal = mainLock.newCondition();
            this.checkAddToQueueSignal = mainLock.newCondition();
            this.taskQueue = new RefLinkedList<>();
            this.globalCancel = Cancellation.createCancellationSource();
            this.currentWorker = new AtomicReference<>(null);
            this.active = false;
            this.terminateSignal = new WaitableSignal();
            this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
        }

        private Thread createWorkerThread(Runnable task) {
            return threadFactory.newThread(task);
        }

        private Thread createOwnedWorkerThread(final Runnable task) {
            assert task != null;
            return createWorkerThread(() -> {
                try {
                    OWNER_EXECUTOR.set(Impl.this);
                    task.run();
                } finally {
                    OWNER_EXECUTOR.remove();
                }
            });
        }

        public String getPoolName() {
            return poolName;
        }

        public void setThreadFactory(ThreadFactory threadFactory) {
            Objects.requireNonNull(threadFactory, "threadFactory");
            this.threadFactory = threadFactory;
        }

        public void setMaxQueueSize(int maxQueueSize) {
            ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");
            this.maxQueueSize = maxQueueSize;
            mainLock.lock();
            try {
                // Actually it might be full but awaking the waiting thread
                // cause only a performance loss. This performance loss is of
                // little consequence because we don't expect this method to be
                // called that much.
                checkAddToQueueSignal.signalAll();
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

        private RefList.ElementRef<?> tryAddToQueue(CancellationToken cancelToken, QueuedItem queuedTask) {
            FullQueueHandler currentFullQueueHandler = fullQueueHandler;

            mainLock.lock();
            try {
                while (true) {
                    if (isShutdown()) {
                        return null;
                    }

                    if (taskQueue.size() < maxQueueSize) {
                        RefList.ElementRef<?> result = taskQueue.addLastGetReference(queuedTask);
                        checkQueueSignal.signalAll();
                        return result;
                    }

                    if (currentFullQueueHandler != null) {
                        ThreadPoolTaskExecutor.handleFullQueue(mainLock, currentFullQueueHandler, cancelToken);
                        currentFullQueueHandler = null;
                        continue;
                    }
                    CancelableWaits.await(cancelToken, checkAddToQueueSignal);
                }
            } finally {
                mainLock.unlock();
            }
        }

        private void setRemoveFromQueueOnCancel(
                final QueuedItem task,
                final RefList.ElementRef<?> queueRef) {
            task.onCancel(() -> {
                boolean removed;
                mainLock.lock();
                try {
                    removed = queueRef.isRemoved();
                    if (!removed) {
                        queueRef.remove();
                        checkAddToQueueSignal.signal();
                    }
                } finally {
                    mainLock.unlock();
                }

                if (!removed) {
                    try {
                        task.submittedTask.cancel();
                    } finally {
                        tryTerminateNowAndNotify();
                    }
                }
            });
        }

        @Override
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            CancellationToken combinedToken = Cancellation.anyToken(globalCancel.getToken(), cancelToken);
            QueuedItem queuedTask = new QueuedItem(combinedToken, submittedTask);

            RefList.ElementRef<?> queueRef;
            try {
                queueRef = tryAddToQueue(combinedToken, queuedTask);
                if (queueRef == null) {
                    submittedTask.cancel();
                    return;
                }
            } catch (OperationCanceledException ex) {
                submittedTask.completeExceptionally(ex);
                return;
            }

            setRemoveFromQueueOnCancel(queuedTask, queueRef);

            startNewWorkerIfNeeded();
        }

        private void startNewWorkerIfNeeded() {
            if (currentWorker.get() == null) {
                // Obviously, we cannot guarantee that won't two worker
                // start but the very worst case is that the started worker
                // will recognize this and stop immediately.
                Worker worker = new Worker();
                worker.tryStart();
            }
        }

        private boolean tryTerminateNow() {
            assert mainLock.isHeldByCurrentThread();

            if (state == ExecutorState.SHUTTING_DOWN && taskQueue.isEmpty()) {
                if (currentWorker.get() == null) {
                    // 1. Subsequent tasks added to the queue will recognize that
                    //    the executor was shut down and will not be executed.
                    // 2. There is no worker thread, which implies that there is
                    //    no task about to be executed (as only the worker
                    //    thread removes tasks from the queue).
                    //
                    // Therefore: It is guaranteed that from now on, no task
                    //            will be executed.

                    state = ExecutorState.TERMINATED;
                    terminateSignal.signal();
                    return true;
                }
            }

            return false;
        }

        private void initiateTerminate(boolean alreadyTerminated) {
            assert !mainLock.isHeldByCurrentThread();

            if (alreadyTerminated) {
                notifyTerminateListeners();
            } else {
                // This is a rare case, if there is no worker, we could
                // retry terminating now but the caller already did so.
                // Instead of retrying we start a worker which is bound
                // to do the work for us.
                startNewWorkerIfNeeded();
            }
        }

        private void tryTerminateNowAndNotify() {
            if (state != ExecutorState.SHUTTING_DOWN) {
                return;
            }

            boolean terminatedNow;
            mainLock.lock();
            try {
                terminatedNow = tryTerminateNow();
            } finally {
                mainLock.unlock();
            }

            initiateTerminate(terminatedNow);
        }

        @Override
        public void shutdown() {
            boolean terminatedNow;
            mainLock.lock();
            try {
                if (state != ExecutorState.RUNNING) {
                    return;
                }
                state = ExecutorState.SHUTTING_DOWN;
                checkQueueSignal.signalAll();
                checkAddToQueueSignal.signalAll();
                terminatedNow = tryTerminateNow();
            } finally {
                mainLock.unlock();
            }

            initiateTerminate(terminatedNow);
        }

        @Override
        public void shutdownAndCancel() {
            shutdown();
            globalCancel.getController().cancel();
        }

        @Override
        public boolean isShutdown() {
            return state.getStateIndex() >= ExecutorState.SHUTTING_DOWN.getStateIndex();
        }

        @Override
        public boolean isTerminated() {
            return state == ExecutorState.TERMINATED;
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            return terminateSignal.tryWaitSignal(cancelToken, timeout, unit);
        }

        @Override
        public long getNumberOfQueuedTasks() {
            mainLock.lock();
            try {
                return taskQueue.size();
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public long getNumberOfExecutingTasks() {
            return active ? 1 : 0;
        }

        @Override
        public boolean isExecutingInThis() {
            Impl owner = OWNER_EXECUTOR.get();
            if (owner == null) {
                OWNER_EXECUTOR.remove();
                return false;
            }
            return owner == this;
        }

        @Override
        public String toString() {
            int currentQueueSize;
            mainLock.lock();
            try {
                currentQueueSize = taskQueue.size();
            } finally {
                mainLock.unlock();
            }
            return "SingleThreadedExecutor{"
                    + "poolName=" + poolName
                    + ", state=" + state
                    + ", maxQueueSize=" + maxQueueSize
                    + ", idleTimeout=" + TimeUnit.NANOSECONDS.toMillis(idleTimeoutNanos) + " ms"
                    + ", queue=" + currentQueueSize + '}';
        }

        private final class Worker implements Runnable {
            private final AtomicBoolean runCalled;
            private Thread ownerThread;

            public Worker() {
                this.runCalled = new AtomicBoolean(false);
                this.ownerThread = null;
            }

            // This method may only be called once.
            public void tryStart() {
                if (currentWorker.compareAndSet(null, this)) {
                    Thread workerThread;
                    try {
                        workerThread = createOwnedWorkerThread(this);
                        Objects.requireNonNull(workerThread, "workerThread");
                    } catch (Throwable ex) {
                        // Let's hope next time the factory does not fail to
                        // spawn a new thread, so we have chance to recover from
                        // this failure.

                        // Notice that if the thread factory started our worker,
                        // the worker will stop because it will not recognize
                        // "ownerThread".
                        currentWorker.set(null);
                        throw ex;
                    }

                    ownerThread = workerThread;
                    workerThread.start();
                }
            }

            private boolean isQueueEmpty() {
                mainLock.lock();
                try {
                    return taskQueue.isEmpty();
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
                        if (!taskQueue.isEmpty()) {
                            QueuedItem result = taskQueue.remove(0);
                            checkAddToQueueSignal.signal();
                            return result;
                        }

                        // If we have been shutted down and there is no task
                        // in the queue, then we are done.
                        if (isShutdown()) {
                            return null;
                        }

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
                                } else {
                                    if (prevToWaitNanos < toWaitNanos) {
                                        toWaitNanos = 0;
                                    }
                                }
                            }
                        } catch (InterruptedException ex) {
                            // In this thread, we don't care about interrupts but
                            // we need to recalculate the allowed waiting time.
                            toWaitNanos = idleTimeoutNanos - (System.nanoTime() - startTime);
                        }
                    } while (toWaitNanos > 0);
                } finally {
                    mainLock.unlock();
                }
                return null;
            }

            private void executeTask(QueuedItem queuedItem) throws Exception {
                try {
                    active = true;
                    queuedItem.runTask();
                } finally {
                    active = false;
                }
            }

            private void processQueue() throws Exception {
                assert isExecutingInThis();

                QueuedItem queuedItem = pollFromQueue();
                while (queuedItem != null) {
                    executeTask(queuedItem);
                    queuedItem = pollFromQueue();
                }
            }

            @Override
            public void run() {
                // Prevent abuse when calling from a ThreadFactory.
                if (Thread.currentThread() != ownerThread) {
                    LOGGER.error("The worker of {} has been called from the wrong thread.", poolName);
                    throw new IllegalStateException();
                }

                // This may happen if the thread factory calls this task
                // multiple times from the started thread.
                if (!runCalled.compareAndSet(false, true)) {
                    LOGGER.error("The worker of {} has been called multiple times.", poolName);
                    throw new IllegalStateException();
                }

                if (currentWorker.get() != this) {
                    // This path is close to impossible to reliably test but is
                    // possible anyway.
                    LOGGER.error("The thread factory started the worker thread of {} manually.", poolName);
                    throw new IllegalStateException();
                }

                try {
                    processQueue();
                } catch (Throwable ex) {
                    LOGGER.error("Unexpected error in the worker of {}", poolName, ex);
                }

                try {
                    exitWorker();
                } finally {
                    tryTerminateNowAndNotify();
                }
            }

            private void exitWorker() {
                currentWorker.set(null);
                if (!isQueueEmpty()) {
                    new Worker().tryStart();
                }
            }
        }
    }

    private enum ExecutorState {
        RUNNING(0),
        SHUTTING_DOWN(1),
        TERMINATED(2);

        private final int stateIndex;

        private ExecutorState(int stateIndex) {
            this.stateIndex = stateIndex;
        }

        public int getStateIndex() {
            return stateIndex;
        }
    }

    private static final class QueuedItem {
        public final CancellationToken cancelToken;
        public final AbstractTaskExecutor.SubmittedTask<?> submittedTask;

        public QueuedItem(
                CancellationToken cancelToken,
                AbstractTaskExecutor.SubmittedTask<?> submittedTask) {

            this.cancelToken = cancelToken;
            this.submittedTask = submittedTask;
        }

        private void runTask() {
            Thread.interrupted();
            submittedTask.execute(cancelToken);
        }

        public void onCancel(Runnable cancelTask) {
            ListenerRef listenerRef = cancelToken.addCancellationListener(cancelTask);
            submittedTask.getFuture().whenComplete((result, error) -> listenerRef.unregister());
        }
    }
}
