package org.jtrim.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.jtrim.collections.*;
import org.jtrim.utils.*;

/**
 * Defines complementary functionality to {@link AbstractExecutorService}
 * required by {@link ExecutorService} implementations and some other useful
 * features. To provide all the services a default backing executor is provided
 * for convenience.
 * <P>
 * The following features are provided by this class:
 * <ul>
 *  <li>
 *   Methods of {@link ExecutorService} not provided by
 *   {@link AbstractExecutorService}.
 *  </li>
 *  <li>
 *   Listener to be notified when the executor has been terminated and will no
 *   longer execute tasks.
 *  </li>
 *  <li>
 *   Execute a task synchronously instead of on the backing executor.
 *  </li>
 *  <li>
 *   Execute a task on any executor.
 *  </li>
 *  <li>
 *   A user defined policy can be specified what to do when a task cannot be
 *   executed because {@link #shutdown() shutdown()} or
 *   {@link #shutdownNow() shutdownNow()} has been called.
 *  </li>
 * </ul>
 * <P>
 * This implementation maintains a list of the tasks not currently executed but
 * scheduled to an executor (possibly the backing executor). The elements from
 * this list will only be removed when the executor will attempt to execute
 * the scheduled task even if the task was already canceled. Note that the
 * executor will not directly execute user submitted task, so when canceled
 * tasks will not actually be executed (only a no-op wrapper code). Users
 * are provided with means to actually clean this list on demand by calling
 * {@link #purge() purge()} or removing the reference returned by the
 * {@code executeAndGetRef} methods. Note however that it is rarely worthwhile
 * to actually remove these references.
 * <P>
 * Note that in most cases this class is not needed to be used directly because
 * {@link UpgraderExecutor} can be used to upgrade an {@code Executor} to an
 * {@code ExecutorService}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see ExecutorShutdownListener
 * @see UpgraderExecutor
 * @author Kelemen Attila
 */
public final class TaskListExecutorImpl {
    private static final int STATE_RUNNING = 0;
    private static final int STATE_TERMINATING = 1;
    private static final int STATE_TERMINATED = 2;

    private volatile int state;

    private final Executor backingExecutor; // can be null
    private final ReentrantLock mainLock;
    private final Condition endSignal;

    private final RefList<Runnable> tasks;
    private int activeCount;

    private final TaskRefusePolicy taskRefusePolicy;
    private final ExecutorShutdownListener shutdownListener; // can be null

    /**
     * Creates a new {@code TaskListExecutorImpl} with a given backing executor
     * and a default task refuse policy. The default task refuse policy does
     * nothing and silently ignores the refusal of executing a task.
     * <P>
     * Specifying the backing executor is optional (i.e.: can be {@code null},
     * however when unspecified the {@link #execute(Runnable) execute(Runnable)}
     * and {@link #executeAndGetRef(Runnable) executeAndGetRef(Runnable)}
     * methods cannot be used.
     *
     * @param backingExecutor the backing executor to be used by the
     *   {@code execute(Runnable)} and {@code executeAndGetRef(Runnable)}
     *   methods. This argument can be {@code null} if the previously mentioned
     *   methods does not need to be used.
     */
    public TaskListExecutorImpl(Executor backingExecutor) {
        this(backingExecutor, null);
    }

    /**
     * Creates a new {@code TaskListExecutorImpl} with a given backing executor
     * and task refuse policy.
     * <P>
     * Specifying the backing executor is optional (i.e.: can be {@code null},
     * however when unspecified the {@link #execute(Runnable) execute(Runnable)}
     * and {@link #executeAndGetRef(Runnable) executeAndGetRef(Runnable)}
     * methods cannot be used.
     * <P>
     * Specifying the task refuse policy is also optional (i.e.: can be
     * {@code null}. When unspecified, the default task refuse policy does
     * nothing and silently ignores the refusal of executing a task.
     *
     * @param backingExecutor the backing executor to be used by the
     *   {@code execute(Runnable)} and {@code executeAndGetRef(Runnable)}
     *   methods. This argument can be {@code null} if the previously mentioned
     *   methods does not need to be used.
     * @param taskRefusePolicy the policy what to do when a task is refused due
     *   to {@link #shutdown() shutdown()} or
     *   {@link #shutdownNow() shutdownNow()} being called.
     */
    public TaskListExecutorImpl(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy) {
        this(backingExecutor, taskRefusePolicy, null);
    }

    /**
     * Creates a new {@code TaskListExecutorImpl} with a given backing executor,
     * task refuse policy and a listener which will be notified when no more
     * task can be executed by this implementation. Note that the listener
     * can be invoked in many contexts: In a task scheduled to an executor
     * or in a method call of this {@code TaskListExecutorImpl}.
     * <P>
     * Specifying the backing executor is optional (i.e.: can be {@code null},
     * however when unspecified the {@link #execute(Runnable) execute(Runnable)}
     * and {@link #executeAndGetRef(Runnable) executeAndGetRef(Runnable)}
     * methods cannot be used.
     * <P>
     * Specifying the task refuse policy is also optional (i.e.: can be
     * {@code null}. When unspecified, the default task refuse policy does
     * nothing and silently ignores the refusal of executing a task.
     *
     * @param backingExecutor the backing executor to be used by the
     *   {@code execute(Runnable)} and {@code executeAndGetRef(Runnable)}
     *   methods. This argument can be {@code null} if the previously mentioned
     *   methods does not need to be used.
     * @param taskRefusePolicy the policy what to do when a task is refused due
     *   to {@link #shutdown() shutdown()} or
     *   {@link #shutdownNow() shutdownNow()} being called.
     * @param shutdownListener the listener to be notified when no more task can
     *   be executed by this implementation. This argument can be {@code null}
     *   if no such notification is required.
     */
    public TaskListExecutorImpl(Executor backingExecutor,
            TaskRefusePolicy taskRefusePolicy,
            ExecutorShutdownListener shutdownListener) {

        this.activeCount = 0;
        this.state = STATE_RUNNING;
        this.mainLock = new ReentrantLock();
        this.endSignal = this.mainLock.newCondition();
        this.backingExecutor = backingExecutor;
        this.tasks = new RefLinkedList<>();
        this.shutdownListener = shutdownListener;
        this.taskRefusePolicy = taskRefusePolicy != null
                ? taskRefusePolicy
                : SilentTaskRefusePolicy.INSTANCE;
    }

    private void onTerminate() {
        if (shutdownListener != null) {
            shutdownListener.onTerminate();
        }
    }

    private boolean tryTerminate() {
        assert mainLock.isHeldByCurrentThread();
        assert activeCount >= 0;

        if (state == STATE_TERMINATING && activeCount == 0 && tasks.isEmpty()) {
            state = STATE_TERMINATED;
            endSignal.signalAll();
            return true;
        }
        else {
            return false;
        }
    }

    private static class ElementAndRef {
        public final RefList.ElementRef<?> ref;
        public final Object element;

        public ElementAndRef(RefList.ElementRef<?> ref) {
            this.ref = ref;
            this.element = ref.getElement();
        }
    }

    /**
     * Removes every task from the underlying list which has been canceled.
     * This method can only detect that tasks have been canceled if they
     * implement the {@link Future} interface. If they do their
     * {@link Future#isCancelled() isCancelled()} method will be inspected to
     * check if the task has been canceled. Note that by default tasks
     * passed to the {@link ExecutorService#execute(Runnable)} method by
     * {@link AbstractExecutorService} will implement the {@code Future}
     * interface.
     * <P>
     * This method takes linear time in the number of not yet executed but
     * already scheduled tasks. Note however that this method is only rarely
     * worthwhile to call for two reasons:
     * <ul>
     *  <li>
     *   The canceled tasks will be removed relatively fast when they actually
     *   tried to be executed by the underlying {@code Executor}.
     *  </li>
     *  <li>
     *   The underlying {@code Executor} likely has a reference to the
     *   underlying tasks as well, so removing them for the list maintained by
     *   this implementation does not have much impact on the memory
     *   consumption of the code.
     *  </li>
     * </ul>
     */
    public void purge() {
        List<ElementAndRef> taskRefs = new LinkedList<>();

        mainLock.lock();
        try {
            RefList.ElementRef<?> ref = tasks.getFirstReference();
            while (ref != null) {
                taskRefs.add(new ElementAndRef(ref));
                ref = ref.getNext(1);
            }
        } finally {
            mainLock.unlock();
        }

        Iterator<ElementAndRef> taskRefItr = taskRefs.iterator();
        while (taskRefItr.hasNext()) {
            ElementAndRef ref = taskRefItr.next();
            Object element = ref.element;

            if (element instanceof Future
                    && ((Future<?>)element).isCancelled()) {
                taskRefItr.remove();
            }
        }

        if (!taskRefs.isEmpty()) {
            boolean terminatedNow = false;
            mainLock.lock();
            try {
                for (ElementAndRef ref: taskRefs) {
                    ref.ref.remove();
                }

                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }
    }

    /**
     * Returns the backing executor specified at construction time.
     *
     * @return the backing executor specified at construction time. This method
     *   may return {@code null} if no such executor was specified.
     */
    public Executor getBackingExecutor() {
        return backingExecutor;
    }

    /**
     * Disallows executing tasks scheduled through this implementation.
     * Previously scheduled tasks will not be canceled, only task submitted
     * after this call are denied to be executed. Note that tasks submitted
     * for execution concurrently with this call may or may not execute.
     * <P>
     * This method call is idempotent, calling it multiple times has the same
     * effect as calling it once.
     */
    public void shutdown() {
        boolean terminatedNow = false;
        mainLock.lock();
        try {
            if (state < STATE_TERMINATING) {
                state = STATE_TERMINATING;
                terminatedNow = tryTerminate();
            }
        } finally {
            mainLock.unlock();

            if (terminatedNow) {
                onTerminate();
            }
        }
    }

    /**
     * Disallows executing tasks scheduled through this implementation and
     * cancels already scheduled tasks if possible. This method returns the list
     * of tasks successfully canceled.
     * <P>
     * Subsequent calls to this method will always return an empty list but
     * otherwise has no further effects.
     * <P>
     * Note that, as of the current implementation already running tasks will
     * not be {@link Thread#interrupt() interrupted}. However, this may change
     * in the future.
     *
     * @return the list of task, successfully canceled before they were actually
     *   executed. This method never returns {@code null}.
     */
    public List<Runnable> shutdownNow() {
        boolean terminatedNow = false;

        List<Runnable> remaining = Collections.emptyList();

        mainLock.lock();
        try {
            if (state < STATE_TERMINATED) {
                remaining = new ArrayList<>(tasks);
                tasks.clear();

                // If we cannot terminate now (i.e.: there are active tasks)
                // the last active task will terminate.
                // Note that state is not greater than STATE_TERMINATING
                // because of the outer check.
                state = STATE_TERMINATING;
                terminatedNow = tryTerminate();
            }
        } finally {
            mainLock.unlock();

            if (terminatedNow) {
                onTerminate();
            }
        }

        return remaining;
    }

    /**
     * Checks whether this implementation allows new tasks to be scheduled.
     * That is, checks if the {@code shutdown} or {@code shutdownNow} methods
     * have been called.
     * <P>
     * If this method signals that this implementation was shutted down,
     * subsequent tries to schedule a new task will always fail an invoke the
     * underlying task refuse policy specified at construction time.
     *
     * @return {@code true} if this implementation has been shutted down and
     *   does not allow new task to be scheduled, {@code false} otherwise
     */
    public boolean isShutdown() {
        return state >= STATE_TERMINATING;
    }

    /**
     * Checks whether it is possible that tasks scheduled to this implementation
     * may execute or not. In case this method signals that this implementation
     * has been terminated, no more tasks will be executed by this
     * implementation even if it were scheduled previously. Also subsequent
     * tries to schedule a new task will always fail an invoke the underlying
     * task refuse policy specified at construction time.
     *
     * @return {@code true} if this implementation has been terminated and will
     *   no longer execute tasks scheduled through it, {@code false} otherwise
     */
    public boolean isTerminated() {
        return state == STATE_TERMINATED;
    }

    /**
     * Waits until this implementation reaches the
     * {@link #isTerminated() terminated} state. Once this method returns
     * normally the {@code isTerminated()} method will always return
     * {@code true}.
     *
     * @throws InterruptedException thrown if the current thread was
     *   interrupted. When this exception is thrown the interrupted status of
     *   the current thread will be cleared.
     */
    public void awaitTermination() throws InterruptedException {
        mainLock.lock();
        try {
            while (state != STATE_TERMINATED) {
                endSignal.await();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Waits until this implementation reaches the
     * {@link #isTerminated() terminated} state or the given timeout elapses.
     * Once this method returns successfully the {@code isTerminated()} method
     * will always return {@code true}.
     *
     * @param timeout the maximum time to wait for this implementation to
     *   terminate in the given time unit. This argument must be greater than or
     *   equal to zero.
     * @param unit the time unit of the {@code timeout} argument. This argument
     *   cannot be {@code null}.
     * @return {@code true} if this implementation has reached the terminated
     *   state before the given timeout was elapsed, {@code false} otherwise
     *
     * @throws IllegalArgumentException thrown if {@code timeout &lt; 0}
     * @throws InterruptedException thrown if the current thread was
     *   interrupted. When this exception is thrown the interrupted status of
     *   the current thread will be cleared.
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {

        ExceptionHelper.checkArgumentInRange(timeout, 0, Long.MAX_VALUE, "timeout");

        long nanosToWait = unit.toNanos(timeout);
        mainLock.lock();
        try {
            while (nanosToWait > 0 && state != STATE_TERMINATED) {
                nanosToWait = endSignal.awaitNanos(nanosToWait);
            }
            return state == STATE_TERMINATED;
        } finally {
            mainLock.unlock();
        }
    }

    private boolean executeActive(Runnable task) {
        assert !mainLock.isHeldByCurrentThread();

        boolean terminatedNow = false;
        boolean executed = false;

        try {
            if (task != null) {
                task.run();
                executed = true;
            }
        } finally {
            mainLock.lock();
            try {
                activeCount--;
                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }
        return executed;
    }

    /**
     * Executes the given task synchronously on the calling task if this
     * implementation has not yet been terminated.
     * <P>
     * Once this method returns (even by throwing an exception), the specified
     * task has either been executed or never will be. If executed, it will
     * always be done on the current thread synchronously. Exception thrown by
     * the specified task will be propagated to the caller of this method.
     *
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return {@code true} if the given task was successfully executed,
     *   {@code false} if it was refused to be executed because this
     *   implementation was shutted down
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public boolean executeNow(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        mainLock.lock();
        try {
            if (isShutdown()) {
                return false;
            }

            activeCount++;
        } finally {
            mainLock.unlock();
        }

        return executeActive(task);
    }

    private RefList.ElementRef<Runnable> tryExecute(Runnable task, Executor executor) {
        ExceptionHelper.checkNotNullArgument(task, "task");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        RefList.ElementRef<Runnable> taskRef;

        mainLock.lock();
        try {
            if (state >= STATE_TERMINATING) {
                return null;
            }

            taskRef = tasks.addLastGetReference(task);
        } finally {
            mainLock.unlock();
        }

        executor.execute(new SingleTaskExecutor(taskRef));
        return taskRef;
    }

    private void checkBackingExecutor() {
        if (backingExecutor == null) {
            throw new IllegalStateException("execute method cannot be called"
                    + " if the backing executor was not specified.");
        }
    }

    /**
     * Submits a task to the backing executor specified at construction time.
     * <P>
     * If this implementation has been shutted down, the task will not be
     * scheduled and the task refuse policy specified at construction time will
     * be invoked. This method may not be called if the backing executor was
     * not specified.
     * <P>
     * This method is equivalent to:
     * {@code impl.execute(task, impl.getBackingExecutor())} except that this
     * method will throw an {@code IllegalStateException} rather than a
     * {@code NullPointerException} if the backing executor is {@code null}.
     *
     * @param task the task to be scheduled to the backing executor. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalStateException thrown if the backing executor was not
     *   specified at construction time
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public void execute(Runnable task) {
        checkBackingExecutor();
        execute(task, backingExecutor);
    }

    /**
     * Submits a task to the backing executor specified at construction time
     * and returns a reference which can be used to remove the task from the
     * underlying list (which also cancels the task).
     * <P>
     * If this implementation has been shutted down, the task will not be
     * scheduled and the task refuse policy specified at construction time will
     * be invoked. This method may not be called if the backing executor was
     * not specified.
     * <P>
     * This method is equivalent to:
     * {@code impl.executeAndGetRef(task, impl.getBackingExecutor())} except
     * that this method will throw an {@code IllegalStateException} rather than
     * a {@code NullPointerException} if the backing executor is {@code null}.
     *
     * @param task the task to be scheduled to the backing executor. This
     *   argument cannot be {@code null}.
     * @return a reference which can be used to remove the scheduled task from
     *   the underlying list (which also cancels the task). The returned
     *   reference is never {@code null}, safe to use by multiple thread
     *   concurrently and <I>synchronization transparent</I>. The returned
     *   reference does not support the {@code setElement} method.
     *
     * @throws IllegalStateException thrown if the backing executor was not
     *   specified at construction time
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    public RefCollection.ElementRef<Runnable> executeAndGetRef(
            Runnable task) {
        checkBackingExecutor();
        return executeAndGetRef(task, backingExecutor);
    }

    /**
     * Submits a task to the given executor.
     * <P>
     * If this implementation has been shutted down, the task will not be
     * scheduled and the task refuse policy specified at construction time will
     * be invoked.
     *
     * @param task the task to be scheduled to the backing executor. This
     *   argument cannot be {@code null}.
     * @param executor the executor which will execute the specified task.
     *   This argument cannot be {@code null}
     *
     * @throws NullPointerException thrown if the specified task or the
     *   executor is {@code null}
     */
    public void execute(Runnable task, Executor executor) {
        if (tryExecute(task, executor) == null) {
            taskRefusePolicy.refuseTask(task);
        }
    }

    /**
     * Submits a task to the given executor and returns a reference which can be
     * used to remove the task from the underlying list (which also cancels the
     * task).
     * <P>
     * If this implementation has been shutted down, the task will not be
     * scheduled and the task refuse policy specified at construction time will
     * be invoked.
     *
     * @param task the task to be scheduled to the backing executor. This
     *   argument cannot be {@code null}.
     * @param executor the executor which will execute the specified task.
     *   This argument cannot be {@code null}
     * @return a reference which can be used to remove the scheduled task from
     *   the underlying list (which also cancels the task). The returned
     *   reference is never {@code null}, safe to use by multiple thread
     *   concurrently and <I>synchronization transparent</I>. The returned
     *   reference does not support the {@code setElement} method.
     *
     * @throws NullPointerException thrown if the specified task or the
     *   executor is {@code null}
     */
    public RefCollection.ElementRef<Runnable> executeAndGetRef(
            Runnable task, Executor executor) {

        RefList.ElementRef<Runnable> ref = tryExecute(task, executor);
        if (ref == null) {
            taskRefusePolicy.refuseTask(task);
            return null;
        }
        else {
            return new TaskRefRemover(ref);
        }
    }

    private class SingleTaskExecutor implements Runnable {
        private final RefList.ElementRef<Runnable> task;

        public SingleTaskExecutor(RefList.ElementRef<Runnable> task) {
            this.task = task;
        }

        @Override
        public void run() {
            Runnable currentTask = null;

            mainLock.lock();
            try {
                if (!task.isRemoved()) {
                    currentTask = task.getElement();
                    task.remove();
                }

                activeCount++;
            } finally {
                mainLock.unlock();
            }

            executeActive(currentTask);
        }
    }

    private class TaskRefRemover implements RefCollection.ElementRef<Runnable> {
        private final RefList.ElementRef<Runnable> taskRef;

        public TaskRefRemover(RefList.ElementRef<Runnable> taskRef) {
            assert taskRef != null;
            this.taskRef = taskRef;
        }

        @Override
        public boolean isRemoved() {
            mainLock.lock();
            try {
                return taskRef.isRemoved();
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void remove() {
            boolean terminatedNow = false;
            mainLock.lock();
            try {
                taskRef.remove();
                terminatedNow = tryTerminate();
            } finally {
                mainLock.unlock();

                if (terminatedNow) {
                    onTerminate();
                }
            }
        }

        @Override
        public Runnable setElement(Runnable newElement) {
            throw new UnsupportedOperationException("Task cannot be replaced");
        }

        @Override
        public Runnable getElement() {
            // This lock is not required because we do not update
            // the element. Still the lock will remain because this way we don't
            // have to worry about future changes.
            mainLock.lock();
            try {
                return taskRef.getElement();
            } finally {
                mainLock.unlock();
            }
        }
    }
}
