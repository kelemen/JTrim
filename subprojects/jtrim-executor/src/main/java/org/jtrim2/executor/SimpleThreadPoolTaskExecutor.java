package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.RefCollection;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.ObjectFinalizer;

final class SimpleThreadPoolTaskExecutor
extends
        DelegatedTaskExecutorService
implements
        MonitorableTaskExecutorService {

    private final ObjectFinalizer finalizer;
    private final Impl impl;

    public SimpleThreadPoolTaskExecutor(
            String poolName,
            int maxThreadCount,
            int maxQueueSize,
            ThreadFactory threadFactory) {

        this(new Impl(poolName, maxThreadCount, maxQueueSize, threadFactory));
    }

    private SimpleThreadPoolTaskExecutor(Impl impl) {
        super(impl);
        this.impl = impl;
        this.finalizer = new ObjectFinalizer(impl::shutdown, impl.poolName + " ThreadPoolTaskExecutor shutdown");
    }

    boolean isFifo() {
        return impl.maxThreadCount == 1;
    }

    String getPoolName() {
        return impl.poolName;
    }

    int getMaxThreadCount() {
        return impl.maxThreadCount;
    }

    int getMaxQueueSize() {
        return impl.maxQueueSize;
    }

    ThreadFactory getThreadFactory() {
        return impl.threadFactory;
    }

    boolean isFinalized() {
        return finalizer.isFinalized();
    }

    void setFullQueueHandler(FullQueueHandler fullQueueHandler) {
        impl.fullQueueHandler = fullQueueHandler;
    }

    FullQueueHandler getFullQueueHandler() {
        return impl.fullQueueHandler;
    }

    @Override
    public boolean isExecutingInThis() {
        return impl.isExecutingInThis();
    }

    @Override
    public long getNumberOfQueuedTasks() {
        return impl.getNumberOfQueuedTasks();
    }

    @Override
    public long getNumberOfExecutingTasks() {
        return impl.getNumberOfExecutingTasks();
    }

    @Override
    public void shutdown() {
        finalizer.markFinalized();
        impl.shutdown();
    }

    @Override
    public void shutdownAndCancel() {
        finalizer.markFinalized();
        impl.shutdownAndCancel();
    }

    public void dontNeedShutdown() {
        finalizer.markFinalized();
    }

    @Override
    public String toString() {
        return impl.toString();
    }

    private static final class Impl
    extends
            AbstractTerminateNotifierTaskExecutorService
    implements
            MonitorableTaskExecutor {

        private static final Logger LOGGER = Logger.getLogger(SimpleThreadPoolTaskExecutor.class.getName());

        private static final ThreadLocal<Impl> OWNER_EXECUTOR = new ThreadLocal<>();

        private final String poolName;
        private final Lock mainLock;
        private final ThreadFactory threadFactory;
        private final int maxThreadCount;
        private final int maxQueueSize;
        private final RefList<QueuedItem> queue;
        private final Condition checkQueueSignal;
        private final Condition checkAddToQueueSignal;
        private final Condition terminateSignal;
        private final AtomicInteger currentlyExecuting;
        private volatile ExecutorState state;
        private volatile boolean allThreadsStarted;
        private int createdThreadCount;
        private int activeWorkerCount;
        private final CancellationSource executorCancelSource;
        private FullQueueHandler fullQueueHandler;

        public Impl(
                String poolName,
                int maxThreadCount,
                int maxQueueSize,
                ThreadFactory threadFactory) {

            this.poolName = Objects.requireNonNull(poolName, "poolName");
            this.mainLock = new ReentrantLock();
            this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
            this.maxThreadCount = positive(maxThreadCount, "maxThreadCount");
            this.maxQueueSize = positive(maxQueueSize, "maxQueueSize");
            this.queue = new RefLinkedList<>();
            this.checkQueueSignal = this.mainLock.newCondition();
            this.checkAddToQueueSignal = this.mainLock.newCondition();
            this.terminateSignal = this.mainLock.newCondition();
            this.currentlyExecuting = new AtomicInteger(0);
            this.state = ExecutorState.RUNNING;
            this.allThreadsStarted = false;
            this.executorCancelSource = Cancellation.createCancellationSource();
            this.createdThreadCount = 0;
            this.activeWorkerCount = 0;
        }

        private static int positive(int value, String name) {
            return ExceptionHelper.checkArgumentInRange(value, 1, Integer.MAX_VALUE, name);
        }

        private Worker newWorker() {
            Worker worker = new Worker();
            Thread thread = threadFactory.newThread(worker);
            worker.setOwnerThread(thread);
            return worker;
        }

        private void tryStartThread() {
            if (allThreadsStarted) {
                // Should be a common case under normal use.
                return;
            }

            mainLock.lock();
            try {
                int currentThreadCount = createdThreadCount;
                int neededThreadCount = maxThreadCount - currentThreadCount;
                if (neededThreadCount <= 1) {
                    allThreadsStarted = true;
                    if (neededThreadCount <= 0) {
                        return;
                    }
                }
                createdThreadCount = currentThreadCount + 1;
            } finally {
                mainLock.unlock();
            }

            newWorker().startThread();
        }

        @Override
        protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
            CancellationToken combinedToken = Cancellation.anyToken(cancelToken, executorCancelSource.getToken());
            QueuedItem newItem = new QueuedItem(combinedToken, submittedTask);

            RefList.ElementRef<QueuedItem> queueRef;
            try {
                queueRef = tryAddToQueue(combinedToken, newItem);
                if (queueRef == null) {
                    // Don't even bother to start a new thread.
                    newItem.cancel();
                    return;
                }
            } catch (OperationCanceledException ex) {
                newItem.submittedTask.completeExceptionally(ex);
                return;
            }

            tryStartThread();
            setRemoveFromQueueOnCancel(newItem, queueRef);
        }

        private void setRemoveFromQueueOnCancel(
                final QueuedItem task,
                final RefCollection.ElementRef<?> queueRef) {

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
                        task.cancel();
                    } finally {
                        tryTerminateAndNotify();
                    }
                }
            });
        }

        private RefList.ElementRef<QueuedItem> tryAddToQueue(
                CancellationToken cancelToken,
                QueuedItem newItem) {

            FullQueueHandler currentFullQueueHandler = fullQueueHandler;

            mainLock.lock();
            try {
                while (state == ExecutorState.RUNNING) {
                    if (queue.size() < maxQueueSize) {
                        RefList.ElementRef<QueuedItem> queueRef = queue.addLastGetReference(newItem);
                        checkQueueSignal.signal();
                        return queueRef;
                    }

                    if (currentFullQueueHandler != null) {
                        ThreadPoolTaskExecutor.handleFullQueue(mainLock, currentFullQueueHandler, cancelToken);
                        currentFullQueueHandler = null;
                        continue;
                    }

                    CancelableWaits.await(cancelToken, checkAddToQueueSignal);
                }
                return null;
            } finally {
                mainLock.unlock();
            }
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

        private static boolean isTerminationNeededState(ExecutorState state) {
            return state != ExecutorState.RUNNING && state != ExecutorState.TERMINATED;
        }

        private void tryTerminateAndNotify() {
            if (tryTerminate()) {
                notifyTerminateListeners();
            }
        }

        private boolean tryTerminate() {
            if (!isTerminationNeededState(state)) {
                return false;
            }

            mainLock.lock();
            try {
                ExecutorState currentState = state;
                if (isTerminationNeededState(currentState) && activeWorkerCount == 0 && queue.isEmpty()) {
                    state = ExecutorState.TERMINATED;
                    terminateSignal.signalAll();
                    return true;
                }
            } finally {
                mainLock.unlock();
            }
            return false;
        }

        @Override
        public void shutdown() {
            mainLock.lock();
            try {
                if (state != ExecutorState.RUNNING) {
                    return;
                }
                state = ExecutorState.SHUTTING_DOWN;
                checkQueueSignal.signalAll();
                checkAddToQueueSignal.signalAll();
            } finally {
                mainLock.unlock();
            }

            tryTerminateAndNotify();
        }

        @Override
        public void shutdownAndCancel() {
            shutdown();

            mainLock.lock();
            try {
                if (state.ordinal() >= ExecutorState.TERMINATING.ordinal()) {
                    return;
                }

                state = ExecutorState.TERMINATING;
                checkQueueSignal.signalAll();
                checkAddToQueueSignal.signalAll();
            } finally {
                mainLock.unlock();
            }

            tryTerminateAndNotify();

            executorCancelSource.getController().cancel();
        }

        @Override
        public boolean isShutdown() {
            return state != ExecutorState.RUNNING;
        }

        @Override
        public boolean isTerminated() {
            return state == ExecutorState.TERMINATED;
        }

        @Override
        public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            if (state == ExecutorState.TERMINATED) {
                return true;
            }

            long startTime = System.nanoTime();
            long timeoutNanos = unit.toNanos(timeout);
            mainLock.lock();
            try {
                while (state != ExecutorState.TERMINATED) {
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
            return true;
        }

        @Override
        public String toString() {
            int currentActiveWorkerCount;
            int currentQueueSize;
            mainLock.lock();
            try {
                currentActiveWorkerCount = activeWorkerCount;
                currentQueueSize = queue.size();
            } finally {
                mainLock.unlock();
            }
            return "SimpleThreadPoolTaskExecutor{"
                    + "poolName=" + poolName
                    + ", state=" + state
                    + ", maxQueueSize=" + maxQueueSize
                    + ", maxThreadCount=" + maxThreadCount
                    + ", activeWorkers=" + getNumberOfExecutingTasks()
                    + ", runningWorkers=" + currentActiveWorkerCount
                    + ", queue=" + currentQueueSize + '}';
        }

        private class Worker implements Runnable {
            private final AtomicBoolean runCalled;
            private Thread ownerThread;

            public Worker() {
                this.runCalled = new AtomicBoolean(false);
            }

            public void setOwnerThread(Thread ownerThread) {
                this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
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


                if (!prepareStartWorker()) {
                    return;
                }

                try {
                    workerLoop();
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE, "Unexpected error in the main thread loop of " + poolName, ex);
                } finally {
                    finishWorker();
                }
            }

            private boolean prepareStartWorker() {
                OWNER_EXECUTOR.set(Impl.this);

                mainLock.lock();
                try {
                    if (state != ExecutorState.RUNNING && queue.isEmpty()) {
                        return false;
                    }
                    activeWorkerCount++;
                    return true;
                } finally {
                    mainLock.unlock();
                }
            }

            private void finishWorker() {
                mainLock.lock();
                try {
                    activeWorkerCount--;
                } finally {
                    mainLock.unlock();
                    tryTerminateAndNotify();
                }
            }

            private void workerLoop() {
                while (true) {
                    QueuedItem itemToProcess = poll();
                    if (itemToProcess == null) {
                        return;
                    }

                    try {
                        execute(itemToProcess);
                    } catch (Throwable ex) {
                        LOGGER.log(Level.SEVERE, "Unexpected error while processing a task of " + poolName, ex);
                    }
                }
            }

            private void execute(QueuedItem itemToProcess) {
                if (state.ordinal() < ExecutorState.TERMINATING.ordinal()) {
                    currentlyExecuting.getAndIncrement();
                    try {
                        itemToProcess.runTask();
                    } finally {
                        currentlyExecuting.getAndDecrement();
                    }
                } else {
                    itemToProcess.cancel();
                }
            }

            private QueuedItem poll() {
                mainLock.lock();
                try {
                    while (true) {
                        if (!queue.isEmpty()) {
                            QueuedItem result = queue.remove(0);
                            checkAddToQueueSignal.signal();
                            return result;
                        }

                        if (state != ExecutorState.RUNNING) {
                            return null;
                        }

                        try {
                            checkQueueSignal.await();
                        } catch (InterruptedException ex) {
                            // We are not using interrupts directly to communicate with the thread.
                        }
                    }
                } finally {
                    mainLock.unlock();
                }
            }

            public void startThread() {
                ownerThread.start();
            }
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

        public void runTask() {
            Thread.interrupted();
            submittedTask.execute(cancelToken);
        }

        public void cancel() {
            submittedTask.cancel();
        }

        public void onCancel(Runnable cancelTask) {
            ListenerRef listenerRef = cancelToken.addCancellationListener(cancelTask);
            submittedTask.getFuture().whenComplete((result, error) -> listenerRef.unregister());
        }
    }

    // The order of the enum constants is relevant
    private enum ExecutorState {
        RUNNING,
        SHUTTING_DOWN,
        TERMINATING, // = tasks are to be canceled
        TERMINATED
    }
}
