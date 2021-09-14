package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class BackgroundWorkerManager {
    private static final Logger LOGGER = Logger.getLogger(BackgroundWorkerManager.class.getName());

    private final TaskExecutor executor;
    private final Consumer<? super Throwable> failureHandler;

    private final OutstandingTaskTracker workerTracker;

    public BackgroundWorkerManager(
            TaskExecutor executor,
            Runnable onCompletionTask,
            Consumer<? super Throwable> failureHandler) {

        this.executor = Objects.requireNonNull(executor, "executor");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");

        this.workerTracker = new OutstandingTaskTracker(onCompletionTask);
    }

    public void startWorkers(CancellationToken cancelToken, int threadCount, CancelableTask workerTask) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        ExceptionHelper.checkArgumentInRange(threadCount, 1, Integer.MAX_VALUE, "threadCount");
        Objects.requireNonNull(workerTask, "workerTask");

        try {
            startWorkersUnsafe(cancelToken, threadCount, workerTask);
        } finally {
            workerTracker.finishAddingTasks();
        }
    }

    private void startWorkersUnsafe(CancellationToken cancelToken, int threadCount, CancelableTask workerTask) {
        for (int i = 0; i < threadCount; i++) {
            OutstandingTaskTracker.TaskRef consumerLifeRef = workerTracker.startTask();
            try {
                executor.execute(cancelToken, workerTask)
                        .whenComplete((result, failure) -> {
                            if (failure != null) {
                                setFailure(failure);
                            }
                            consumerLifeRef.finishedTask();
                        });
            } catch (Throwable ex) {
                consumerLifeRef.finishedTask();
                throw ex;
            }
        }
    }

    private void setFailure(Throwable failure) {
        try {
            failureHandler.accept(failure);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Unexpected exception while handling background worker failure.", ex);
        }
    }

    public void waitForWorkers() {
        workerTracker.waitForAllTasks();
    }

    // For testing purposes only
    boolean isFinishedAll() {
        return workerTracker.isFinishedAll();
    }
}
