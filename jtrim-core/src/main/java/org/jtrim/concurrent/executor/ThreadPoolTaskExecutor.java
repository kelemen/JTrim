package org.jtrim.concurrent.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.ObjectFinalizer;

/**
 *
 * @author Kelemen Attila
 */
public final class ThreadPoolTaskExecutor extends AbstractTaskExecutorService {
    private static final Logger LOGGER = Logger.getLogger(ThreadPoolTaskExecutor.class.getName());

    private static final long DEFAULT_THREAD_TIMEOUT_MS = 5000;

    private final ObjectFinalizer finalizer;

    private final String poolName;
    private final ListenerManager<Runnable, Void> listeners;

    private volatile int maxQueueSize;
    private volatile long threadTimeoutNanos;
    private volatile int maxThreadCount;

    private final Lock mainLock;

    // activeWorkers contains workers which may execute tasks and not only
    // cleanup tasks.
    private final RefList<Worker> activeWorkers;
    private final RefList<Worker> runningWorkers;
    private final RefList<QueuedItem> queue; // the oldest task is the head of the queue
    private final Condition notFullQueueSignal;
    private final Condition checkQueueSignal;
    private final Condition terminateSignal;
    private volatile ExecutorState state;

    public ThreadPoolTaskExecutor(String poolName) {
        this(poolName, Runtime.getRuntime().availableProcessors());
    }

    public ThreadPoolTaskExecutor(String poolName, int maxThreadCount) {
        this(poolName, maxThreadCount, Integer.MAX_VALUE);
    }

    public ThreadPoolTaskExecutor(String poolName, int maxThreadCount, int maxQueueSize) {
        this(poolName, maxThreadCount, maxQueueSize,
                DEFAULT_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public ThreadPoolTaskExecutor(
            String poolName,
            int maxThreadCount,
            int maxQueueSize,
            long threadTimeout,
            TimeUnit timeUnit) {

        ExceptionHelper.checkNotNullArgument(poolName, "poolName");
        ExceptionHelper.checkArgumentInRange(maxThreadCount, 1, Integer.MAX_VALUE, "maxThreadCount");
        ExceptionHelper.checkArgumentInRange(maxQueueSize, 1, Integer.MAX_VALUE, "maxQueueSize");
        ExceptionHelper.checkArgumentInRange(threadTimeout, 0, Long.MAX_VALUE, "threadTimeout");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

        this.poolName = poolName;
        this.listeners = new CopyOnTriggerListenerManager<>();
        this.maxThreadCount = maxThreadCount;
        this.maxQueueSize = maxQueueSize;
        this.threadTimeoutNanos = timeUnit.toNanos(threadTimeout);
        this.state = ExecutorState.RUNNING;
        this.activeWorkers = new RefLinkedList<>();
        this.runningWorkers = new RefLinkedList<>();
        this.queue = new RefLinkedList<>();
        this.mainLock = new ReentrantLock();
        this.notFullQueueSignal = mainLock.newCondition();
        this.checkQueueSignal = mainLock.newCondition();
        this.terminateSignal = mainLock.newCondition();

        this.finalizer = new ObjectFinalizer(new Runnable() {
            @Override
            public void run() {
                doShutdown();
            }
        }, poolName + " ThreadPoolTaskExecutor shutdown");
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

    public void setThreadTimeout(long threadTimeout, TimeUnit timeUnit) {
        ExceptionHelper.checkArgumentInRange(threadTimeout, 0, Long.MAX_VALUE, "threadTimeout");
        this.threadTimeoutNanos = timeUnit.toNanos(threadTimeout);
    }

    @Override
    protected void submitTask(
            CancellationToken cancelToken,
            CancellationController cancelController,
            CancelableTask task,
            Runnable cleanupTask,
            boolean hasUserDefinedCleanup) {

        CancellationToken waitQueueCancelToken = hasUserDefinedCleanup
                ? CancellationSource.UNCANCELABLE_TOKEN
                : cancelToken;

        QueuedItem newItem = isShutdown()
                ? new QueuedItem(cleanupTask)
                : new QueuedItem(cancelToken, cancelController, task, cleanupTask);

        try {
            boolean submitted = false;
            do {
                Worker newWorker = new Worker();
                if (newWorker.tryStartWorker(newItem)) {
                    submitted = true;
                }
                else {
                    mainLock.lock();
                    try {
                        if (!runningWorkers.isEmpty()) {
                            if (queue.size() < maxQueueSize) {
                                queue.addLastGetReference(newItem);
                                checkQueueSignal.signal();
                                submitted = true;
                            }
                            else {
                                CancelableWaits.await(waitQueueCancelToken, notFullQueueSignal);
                            }
                        }
                    } finally {
                        mainLock.unlock();
                    }
                }
            } while (!submitted);
        } catch (OperationCanceledException ex) {
            // Notice that this can only be thrown if there
            // is no user defined cleanup task and only when waiting for the
            // queue to allow adding more elements.
            cleanupTask.run();
        }
    }

    private void doShutdown() {
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

    @Override
    public void shutdown() {
        finalizer.doFinalize();
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

    private void cancelTasksOfWorkers() {
        List<Worker> currentWorkers;
        mainLock.lock();
        try {
            currentWorkers = new ArrayList<>(activeWorkers);
        } finally {
            mainLock.unlock();
        }

        Throwable toThrow = null;
        for (Worker worker: currentWorkers) {
            try {
                worker.cancelCurrentTask();
            } catch (Throwable ex) {
                if (toThrow == null) {
                    toThrow = ex;
                }
                else {
                    toThrow.addSuppressed(ex);
                }
            }
        }
        if (toThrow != null) {
            ExceptionHelper.rethrow(toThrow);
        }
    }

    @Override
    public void shutdownAndCancel() {
        // First, stop allowing submitting anymore tasks.
        // Also, the current implementation mandates shutdown() to be called
        // before this ThreadPoolTaskExecutor becomes unreachable.
        shutdown();

        setTerminating();
        cancelTasksOfWorkers();
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

    private void notifyTerminate() {
        listeners.onEvent(RunnableDispatcher.INSTANCE, null);
    }

    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        // A quick check for the already terminate case.
        if (isTerminated()) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        AutoUnregisterListener autoListener = new AutoUnregisterListener(listener);
        ListenerRef result = autoListener.registerWith(listeners);
        if (isTerminated()) {
            autoListener.run();
        }
        return result;
    }

    @Override
    public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        if (!isTerminated()) {
            long startTime = System.nanoTime();
            long timeoutNanos = unit.toNanos(startTime);
            mainLock.lock();
            try {
                while (!isTerminated()) {
                    long elapsed = System.nanoTime() - startTime;
                    long toWaitNanos = timeoutNanos - elapsed;
                    if (toWaitNanos <= 0) {
                        throw new OperationCanceledException();
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
            notifyTerminate();
        }
    }

    private boolean tryTerminate() {
        if (isShutdown() && state != ExecutorState.TERMINATED) {
            mainLock.lock();
            try {
                // This check should be more clever because this way can only
                // terminate if even cleanup methods were finished.
                if (isShutdown() && activeWorkers.isEmpty()) {
                    if (state != ExecutorState.TERMINATED) {
                        state = ExecutorState.TERMINATED;
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

    private class Worker extends Thread {
        private RefCollection.ElementRef<?> activeSelfRef;
        private RefCollection.ElementRef<?> selfRef;
        private QueuedItem firstTask;

        private final AtomicReference<CancellationController> currentControllerRef;

        public Worker() {
            this.firstTask = null;
            this.activeSelfRef = null;
            this.selfRef = null;
            this.currentControllerRef = new AtomicReference<>(null);
        }

        public boolean tryStartWorker(QueuedItem firstTask) {
            mainLock.lock();
            try {
                if (firstTask == null && queue.isEmpty()) {
                    return false;
                }

                if (runningWorkers.size() >= maxThreadCount) {
                    return false;
                }

                if (!isTerminated()) {
                    activeSelfRef = activeWorkers.addLastGetReference(this);
                }
                selfRef = runningWorkers.addLastGetReference(this);
            } finally {
                mainLock.unlock();
            }
            this.firstTask = firstTask;
            start();
            return true;
        }

        public void cancelCurrentTask() {
            CancellationController currentController = currentControllerRef.get();
            if (currentController != null) {
                currentController.cancel();
            }
        }

        private void executeTask(QueuedItem item) {
            assert Thread.currentThread() == this;

            currentControllerRef.set(item.cancelController);
            try {
                if (!isTerminating()) {
                    item.task.execute(item.cancelToken);
                }
                else {
                    if (activeSelfRef != null) {
                        mainLock.lock();
                        try {
                            activeSelfRef.remove();
                        } finally {
                            mainLock.unlock();
                        }
                        activeSelfRef = null;
                    }
                    tryTerminateAndNotify();
                }
            } finally {
                currentControllerRef.set(null);
                item.cleanupTask.run();
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

        private void finishWorking() {
            mainLock.lock();
            try {
                if (activeSelfRef != null) {
                    activeSelfRef.remove();
                }
                selfRef.remove();
            } finally {
                mainLock.unlock();
            }
        }

        private QueuedItem pollFromQueue() {
            long startTime = System.nanoTime();
            long toWaitNanos = threadTimeoutNanos;
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

                    try {
                        toWaitNanos = checkQueueSignal.awaitNanos(toWaitNanos);
                    } catch (InterruptedException ex) {
                        // In this thread, we don't care about interrupts but
                        // we need to recalculate the allowed waiting time.
                        toWaitNanos = threadTimeoutNanos - (System.nanoTime() - startTime);
                    }
                } while (toWaitNanos > 0);
            } finally {
                mainLock.unlock();
            }
            return null;
        }

        private void processQueue() {
            QueuedItem itemToProcess = pollFromQueue();
            while (itemToProcess != null) {
                executeTask(itemToProcess);
                itemToProcess = pollFromQueue();
            }
        }

        @Override
        public void run() {
            try {
                if (firstTask != null) {
                    executeTask(firstTask);
                    firstTask = null;
                }
                processQueue();
            } catch (Throwable ex) {
                writeLog(Level.SEVERE, "Unexpected exception", ex);
            } finally {
                finishWorking();
                try {
                    tryTerminateAndNotify();
                } finally {
                    restartIfNeeded();
                }
            }
        }
    }

    private static class QueuedItem {
        public final CancellationToken cancelToken;
        public final CancellationController cancelController;
        public final CancelableTask task;
        public final Runnable cleanupTask;

        public QueuedItem(
                CancellationToken cancelToken,
                CancellationController cancelController,
                CancelableTask task,
                Runnable cleanupTask) {

            this.cancelToken = cancelToken;
            this.cancelController = cancelController;
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        public QueuedItem(Runnable cleanupTask) {
            this.cancelToken = CancellationSource.CANCELED_TOKEN;
            this.cancelController = DummyCancellationController.INSTANCE;
            this.task = DummyCancelableTask.INSTANCE;
            this.cleanupTask = cleanupTask;
        }
    }

    public enum DummyCancelableTask implements CancelableTask {
        INSTANCE;

        @Override
        public void execute(CancellationToken cancelToken) {
        }
    }

    public enum DummyCancellationController implements CancellationController {
        INSTANCE;

        @Override
        public void cancel() {
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

    private static class AutoUnregisterListener implements Runnable {
        private final AtomicReference<Runnable> listener;
        private volatile ListenerRef listenerRef;

        public AutoUnregisterListener(Runnable listener) {
            this.listener = new AtomicReference<>(listener);
            this.listenerRef = null;
        }

        public ListenerRef registerWith(ListenerManager<Runnable, ?> manager) {
            ListenerRef currentRef = manager.registerListener(this);
            this.listenerRef = currentRef;
            if (listener.get() == null) {
                this.listenerRef = null;
                currentRef.unregister();
            }
            return currentRef;
        }

        @Override
        public void run() {
            Runnable currentListener = listener.getAndSet(null);
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
            }
            if (currentListener != null) {
                currentListener.run();
            }
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
