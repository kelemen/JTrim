package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;

/**
 * @see TaskExecutors#upgradeExecutor(TaskExecutor)
 */
final class UpgradedTaskExecutor
extends
        AbstractTerminateNotifierTaskExecutorService {

    private final TaskExecutor executor;
    // Canceled when shutdownAndCancel is called.
    private final CancellationSource executorCancelSource;
    private final AtomicLong submittedTaskCount;
    private final WaitableSignal terminatedSignal;
    private volatile boolean shuttedDown;

    public UpgradedTaskExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
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

    private void finishExecuteOne() {
        if (submittedTaskCount.decrementAndGet() <= 0) {
            if (shuttedDown) {
                signalTerminateIfInactive();
            }
        }
    }

    @Override
    protected void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask) {
        CancellationToken combinedToken = Cancellation.anyToken(cancelToken, executorCancelSource.getToken());
        executor.execute(combinedToken, (CancellationToken taskCancelToken) -> {
            submittedTaskCount.incrementAndGet();
            try {
                submittedTask.execute(taskCancelToken);
            } finally {
                finishExecuteOne();
            }
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
