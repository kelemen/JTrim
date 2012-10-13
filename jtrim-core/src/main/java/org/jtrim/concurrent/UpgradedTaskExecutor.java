package org.jtrim.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.utils.ExceptionHelper;

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
    private final RefCollection<CancellationController> currentTasks;
    private final AtomicLong submittedTaskCount;
    private final WaitableSignal terminatedSignal;
    private volatile boolean shuttedDown;

    public UpgradedTaskExecutor(TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
        this.mainLock = new ReentrantLock();
        this.currentTasks = new RefLinkedList<>();
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
            CancellationController cancelController,
            final CancelableTask task,
            final Runnable cleanupTask,
            boolean hasUserDefinedCleanup) {

        RefCollection.ElementRef<?> controllerRef = null;
        boolean tryExecute = !shuttedDown;
        if (tryExecute) {
            mainLock.lock();
            try {
                // This double check is required and see the comment in the
                // shutdownAndCancel() method for explanation.
                tryExecute = !shuttedDown;
                if (tryExecute) {
                    controllerRef = currentTasks.addGetReference(cancelController);
                }
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
                    if (shuttedDown) {
                        finishExecuteTask();
                        return;
                    }
                    try {
                        task.execute(cancelToken);
                    } finally {
                        finishExecuteTask();
                    }
                }
            };
        }
        else {
            taskToExecute = Tasks.noOpCancelableTask();
        }

        final RefCollection.ElementRef<?> taskCancelControllerRef;
        taskCancelControllerRef = controllerRef;
        executor.execute(cancelToken, taskToExecute, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                try {
                    if (taskCancelControllerRef != null) {
                        mainLock.lock();
                        try {
                            taskCancelControllerRef.remove();
                        } finally {
                            mainLock.unlock();
                        }
                    }
                } finally {
                    cleanupTask.run();
                }
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

        List<CancellationController> toCancel;
        mainLock.lock();
        try {
            toCancel = new ArrayList<>(currentTasks);
            currentTasks.clear();
        } finally {
            mainLock.unlock();
        }

        // Note that at this point it is impossible that a new
        // CancellationController is added to this list because:
        //
        // 1. shuttedDown was true prior the above copy
        // 2. Adding CancellationController may only happen before or after
        //    the above copy and never concurrently (due to the exclusive lock).
        // 3. If adding happened before, then there is no problem as the
        //    above copy will retrieve that CancellationController.
        // 4. If it happened after, then shuttedDown must be true when trying to
        //    add and so the CancellationController will not be added.

        for (CancellationController controller: toCancel) {
            controller.cancel();
        }

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
    public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return terminatedSignal.waitSignal(cancelToken, timeout, unit);
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
