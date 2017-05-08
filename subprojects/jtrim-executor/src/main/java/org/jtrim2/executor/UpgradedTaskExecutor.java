package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;

/**
 * @see TaskExecutors#upgradeExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class UpgradedTaskExecutor
extends
        AbstractTerminateNotifierTaskExecutorService {

    private final TaskExecutor executor;
    private final Lock mainLock;
    // Canceled when shutdownAndCancel is called.
    private final CancellationSource executorCancelSource;
    private final AtomicLong submittedTaskCount;
    private final WaitableSignal terminatedSignal;
    private volatile boolean shuttedDown;

    public UpgradedTaskExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.mainLock = new ReentrantLock();
        this.executorCancelSource = Cancellation.createCancellationSource();
        this.shuttedDown = false;
        this.submittedTaskCount = new AtomicLong(0);
        this.terminatedSignal = new WaitableSignal();
    }

    private void signalTerminateIfInactive() {
        if (submittedTaskCount.get() <= 0) {
            signalTerminate();
        }
    }

    private void signalTerminate() {
        terminatedSignal.signal();
        notifyTerminateListeners();
    }

    @Override
    protected void submitTask(
            CancellationToken cancelToken,
            final CancelableTask task,
            final Runnable cleanupTask,
            boolean hasUserDefinedCleanup) {

        boolean tryExecute = !shuttedDown;
        if (tryExecute) {
            mainLock.lock();
            try {
                // This double check is required and see the comment in the
                // shutdownAndCancel() method for explanation.
                tryExecute = !shuttedDown;
            } finally {
                mainLock.unlock();
            }
        }

        CancelableTask taskToExecute;
        if (tryExecute) {
            taskToExecute = new CancelableTask() {
                private void finishExecuteTask() {
                    if (submittedTaskCount.decrementAndGet() <= 0) {
                        if (shuttedDown) {
                            signalTerminateIfInactive();
                        }
                    }
                }

                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    submittedTaskCount.incrementAndGet();
                    try {
                        task.execute(cancelToken);
                    } finally {
                        finishExecuteTask();
                    }
                }
            };
        }
        else {
            taskToExecute = CancelableTasks.noOpCancelableTask();
        }

        CancellationToken combinedToken = Cancellation.anyToken(cancelToken, executorCancelSource.getToken());
        executor.execute(combinedToken, taskToExecute, (boolean canceled, Throwable error) -> {
            cleanupTask.run();
        });
    }

    @Override
    public void shutdown() {
        shuttedDown = true;
        signalTerminateIfInactive();
    }

    @Override
    public void shutdownAndCancel() {
        shutdown();
        executorCancelSource.getController().cancel();

        signalTerminateIfInactive();
    }

    @Override
    public boolean isShutdown() {
        return shuttedDown;
    }

    @Override
    public boolean isTerminated() {
        return terminatedSignal.isSignaled();
    }

    @Override
    public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return terminatedSignal.tryWaitSignal(cancelToken, timeout, unit);
    }

    @Override
    public String toString() {
        String strState = isTerminated()
                ? "TERMINATED"
                : (isShutdown() ? "SHUTTING DOWN" : "ACTIVE");

        return "UpgradedTaskExecutor{"
                + "executor=" + executor
                + ", currently running tasks=" + submittedTaskCount.get()
                + ", " + strState + '}';
    }
}
