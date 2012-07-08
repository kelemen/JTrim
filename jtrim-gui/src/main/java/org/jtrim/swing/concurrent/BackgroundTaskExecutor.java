package org.jtrim.swing.concurrent;

import java.util.Collection;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class BackgroundTaskExecutor<IDType, RightType> {
    private final AccessManager<IDType, RightType> accessManager;
    private final TaskExecutor executor;

    public BackgroundTaskExecutor(
            AccessManager<IDType, RightType> accessManager,
            TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.accessManager = accessManager;
        this.executor = executor;
    }

    public Collection<AccessToken<IDType>> scheduleToExecute(
            CancellationToken cancelToken,
            AccessRequest<? extends IDType, ? extends RightType> request,
            final BackgroundTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(request, "request");
        ExceptionHelper.checkNotNullArgument(task, "task");

        AccessResult<IDType> accessResult = accessManager.getScheduledAccess(request);
        tryExecute(cancelToken, accessResult, task);
        return accessResult.getBlockingTokens();
    }

    public Collection<AccessToken<IDType>> tryExecute(
            CancellationToken cancelToken,
            AccessRequest<? extends IDType, ? extends RightType> request,
            final BackgroundTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(request, "request");
        ExceptionHelper.checkNotNullArgument(task, "task");

        AccessResult<IDType> accessResult = accessManager.tryGetAccess(request);
        return tryExecute(cancelToken, accessResult, task);
    }

    private Collection<AccessToken<IDType>> tryExecute(
            CancellationToken cancelToken,
            final AccessResult<IDType> accessResult,
            final BackgroundTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        if (accessResult.isAvailable()) {
            boolean submitted = false;
            try {
                CancelableTask executorTask = new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        task.execute(cancelToken, new SwingReporterImpl());
                    }
                };
                CleanupTask releaseTask = new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        accessResult.release();
                    }
                };

                TaskExecutor taskExecutor = accessResult.getAccessToken().createExecutor(executor);
                taskExecutor.execute(cancelToken, executorTask, releaseTask);
                submitted = true;
            } finally {
                if (!submitted) {
                    accessResult.release();
                }
            }
            return null;
        }
        else {
            return accessResult.getBlockingTokens();
        }
    }

    private static class SwingReporterImpl implements SwingReporter {
        private final TaskExecutor swingExecutor;
        private final UpdateTaskExecutor progressExecutor;

        public SwingReporterImpl() {
            this.swingExecutor = SwingTaskExecutor.getStrictExecutor(true);
            this.progressExecutor = new GenericUpdateTaskExecutor(swingExecutor);
        }

        @Override
        public void updateProgress(Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            progressExecutor.execute(task);
        }

        @Override
        public void writeData(final Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            swingExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    task.run();
                }
            }, null);
        }
    }
}
