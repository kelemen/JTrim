package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.OneShotListenerManager;

/**
 * @see TaskExecutors#upgradeToStoppable(TaskExecutor)
 */
final class UpgradedTaskExecutor
implements
        TaskExecutorService {

    private final TaskExecutor executor;
    // Canceled when shutdownAndCancel is called.
    private final CancellationSource executorCancelSource;
    private final AtomicLong submittedTaskCount;
    private final WaitableSignal terminatedSignal;
    private volatile boolean shuttedDown;

    private final OneShotListenerManager<Runnable, Void> listeners;

    public UpgradedTaskExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.executorCancelSource = Cancellation.createCancellationSource();
        this.shuttedDown = false;
        this.submittedTaskCount = new AtomicLong(0);
        this.terminatedSignal = new WaitableSignal();

        this.listeners = new OneShotListenerManager<>();
    }

    private void notifyTerminateListeners() {
        if (!isTerminated()) {
            throw new IllegalStateException(
                    "May only be called in the terminated state.");
        }
        EventListeners.dispatchRunnable(listeners);
    }

    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return listeners.registerOrNotifyListener(listener);
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
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(function, "function");

        if (cancelToken.isCanceled()) {
            return CancelableTasks.canceledComplationStage();
        }

        CancellationToken combinedToken = Cancellation.anyToken(cancelToken, executorCancelSource.getToken());

        Runnable decTask = Tasks.runOnceTask(this::finishExecuteOne);

        submittedTaskCount.incrementAndGet();
        try {
            if (isShutdown()) {
                decTask.run();
                return CancelableTasks.canceledComplationStage();
            }

            CompletionStage<V> result = executor.executeFunction(combinedToken, function);
            result.whenComplete((taskResult, error) -> {
                decTask.run();
            });
            return result;
        } catch (Throwable ex) {
            decTask.run();
            throw ex;
        }
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
