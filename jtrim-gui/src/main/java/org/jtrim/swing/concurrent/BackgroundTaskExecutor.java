package org.jtrim.swing.concurrent;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an executor to execute background tasks of <I>Swing</I> applications.
 * Background tasks are executed by a {@code TaskExecutor} specified at
 * construction time. The background tasks will also have a chance to report
 * their progress (or whatever else they want) and update components on the
 * <I>AWT Event Dispatch Thread</I>.
 * <P>
 * Every {@code BackgroundTaskExecutor} has an associated {@link AccessManager}
 * and tasks are executed in the context of an {@link AccessToken} of this
 * access manager. Therefore these tasks may only execute if a given right is
 * available. This allows for a convenient way to deny executing background
 * tasks if, for example, an other conflicting task is currently being executed
 * or simply allow to disable a button while the task is executing.
 * <P>
 * In case a background task throws an exception, the exception will be logged
 * in a {@code SEVERE} log message (unless the exception is an instance of
 * {@link OperationCanceledException}).
 *
 * <h3>Thread safety</h3>
 * All of the methods of this class are allowed to be accessed from multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @param <IDType> the type of the request ID of the underlying access manager
 * @param <RightType> the type of the rights handled by the underlying access
 *   manager
 *
 * @see BackgroundTask
 *
 * @author Kelemen Attila
 */
public final class BackgroundTaskExecutor<IDType, RightType> {
    private static final Logger LOGGER = Logger.getLogger(BackgroundTaskExecutor.class.getName());

    private final AccessManager<IDType, RightType> accessManager;
    private final TaskExecutor executor;

    /**
     * Creates a new {@code BackgroundTaskExecutor} with the given access
     * manager and {@code TaskExecutor}.
     * <P>
     * The specified {@code TaskExecutor} is recommended to execute tasks on a
     * separate thread instead of the calling thread, however for debugging
     * purposes it may be beneficial to use the {@code SyncTaskExecutor}. The
     * executor should execute tasks on a separate thread to allow methods of
     * this class to be called from the <I>AWT Event Dispatch Thread</I> without
     * actually blocking the EDT.
     *
     * @param accessManager the {@code AccessManager} from which access tokens
     *   are requested to execute tasks in their context. This argument cannot
     *   be {@code null}.
     * @param executor the {@code TaskExecutor} which actually executes
     *   submitted tasks. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public BackgroundTaskExecutor(
            AccessManager<IDType, RightType> accessManager,
            TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.accessManager = accessManager;
        this.executor = executor;
    }

    /**
     * Submits the given {@link BackgroundTask} to the {@code TaskExecutor}
     * specified at construction time after all the access tokens currently
     * blocking access has been released. This method differs from the
     * {@code tryExecute} method only by invoking the
     * {@link AccessManager#getScheduledAccess(AccessRequest) getScheduledAccess}
     * method of the underlying access token instead of the
     * {@link AccessManager#tryGetAccess(AccessRequest) tryGetAccess} method.
     * <P>
     * This method will first attempt to acquire an {@code AccessToken} from
     * the underlying access manager using the specified {@code AccessRequest}.
     * If the access manager refuses this request this method will immediately
     * return {@code null}. If access was granted for this method, the specified
     * background task will be submitted to the {@code TaskExecutor} specified
     * at construction time in the context of the access token. Therefore the
     * {@code execute} method of the task will only be executed while the
     * acquired was not withdrawn.
     * <P>
     * The submitted background task can be canceled by releasing the
     * {@code AccessToken} acquired by this method. Releasing the acquired
     * access token will cause the {@code CancellationToken} of the submitted
     * task to signal a cancellation request (or it is possible that the task
     * will not even be executed).
     * not necessarily support it). This argument
     *   cannot be {@code null}.
     * @param request the {@code AccessRequest} used to acquire the
     *   {@code AccessToken} in whose context the task is to be executed. This
     *   argument cannot be {@code null}.
     * @param task the {@code BackgroundTask} to be submitted to the underlying
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @return {@code null} if the access was granted to execute the background
     *   task and it was submitted to the underlying {@code TaskExecutor} or
     *   if access was refused, the access tokens blocking access the specified
     *   request. Note that if this method returns an empty collection, it means
     *   that access was refused but the underlying access manager did not
     *   return the reason of refusal.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Collection<AccessToken<IDType>> scheduleToExecute(
            AccessRequest<? extends IDType, ? extends RightType> request,
            BackgroundTask task) {
        return scheduleToExecute(Cancellation.UNCANCELABLE_TOKEN, request, task);
    }

    /**
     * Submits the given {@link BackgroundTask} to the {@code TaskExecutor}
     * specified at construction time after all the access tokens currently
     * blocking access has been released. This method differs from the
     * {@code tryExecute} method only by invoking the
     * {@link AccessManager#getScheduledAccess(AccessRequest) getScheduledAccess}
     * method of the underlying access token instead of the
     * {@link AccessManager#tryGetAccess(AccessRequest) tryGetAccess} method.
     * <P>
     * This method will first attempt to acquire an {@code AccessToken} from
     * the underlying access manager using the specified {@code AccessRequest}.
     * If the access manager refuses this request this method will immediately
     * return {@code null}. If access was granted for this method, the specified
     * background task will be submitted to the {@code TaskExecutor} specified
     * at construction time in the context of the access token. Therefore the
     * {@code execute} method of the task will only be executed while the
     * acquired was not withdrawn.
     * <P>
     * There are in general two ways to cancel the submitted background task:
     * One way is to request cancellation through the specified
     * {@code CancellationToken} and the other is to release the
     * {@code AccessToken} acquired by this method. Releasing the acquired
     * access token will cause the {@code CancellationToken} of the submitted
     * task to signal a cancellation request (or it is possible that the task
     * will not even be executed).
     *
     * @param cancelToken the {@code CancellationToken} which is used to signal
     *   that the background task should be canceled. If the background task
     *   has not yet started, it may not be executed at all (the underlying
     *   {@code TaskExecutor} may not necessarily support it). This argument
     *   cannot be {@code null}.
     * @param request the {@code AccessRequest} used to acquire the
     *   {@code AccessToken} in whose context the task is to be executed. This
     *   argument cannot be {@code null}.
     * @param task the {@code BackgroundTask} to be submitted to the underlying
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @return {@code null} if the access was granted to execute the background
     *   task and it was submitted to the underlying {@code TaskExecutor} or
     *   if access was refused, the access tokens blocking access the specified
     *   request. Note that if this method returns an empty collection, it means
     *   that access was refused but the underlying access manager did not
     *   return the reason of refusal.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Collection<AccessToken<IDType>> scheduleToExecute(
            CancellationToken cancelToken,
            AccessRequest<? extends IDType, ? extends RightType> request,
            BackgroundTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(request, "request");
        ExceptionHelper.checkNotNullArgument(task, "task");

        AccessResult<IDType> accessResult = accessManager.getScheduledAccess(request);
        tryExecute(cancelToken, accessResult, task);
        return accessResult.getBlockingTokens();
    }

    /**
     * Submits the given {@link BackgroundTask} to the {@code TaskExecutor}
     * specified at construction time if the given access rights can
     * successfully acquired from the underlying access manager.
     * <P>
     * This method will first attempt to acquire an {@code AccessToken} from
     * the underlying access manager using the specified {@code AccessRequest}.
     * If the access manager refuses this request this method will immediately
     * return {@code null}. If access was granted for this method, the specified
     * background task will be submitted to the {@code TaskExecutor} specified
     * at construction time in the context of the access token. Therefore the
     * {@code execute} method of the task will only be executed while the
     * acquired was not withdrawn.
     * <P>
     * The submitted background task can be canceled by releasing the
     * {@code AccessToken} acquired by this method. Releasing the acquired
     * access token will cause the {@code CancellationToken} of the submitted
     * task to signal a cancellation request (or it is possible that the task
     * will not even be executed).
     *
     * @param request the {@code AccessRequest} used to acquire the
     *   {@code AccessToken} in whose context the task is to be executed. This
     *   argument cannot be {@code null}.
     * @param task the {@code BackgroundTask} to be submitted to the underlying
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @return {@code null} if the access was granted to execute the background
     *   task and it was submitted to the underlying {@code TaskExecutor} or
     *   if access was refused, the access tokens blocking access the specified
     *   request. Note that if this method returns an empty collection, it means
     *   that access was refused but the underlying access manager did not
     *   return the reason of refusal.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Collection<AccessToken<IDType>> tryExecute(
            AccessRequest<? extends IDType, ? extends RightType> request,
            BackgroundTask task) {
        return tryExecute(Cancellation.UNCANCELABLE_TOKEN, request, task);
    }

    /**
     * Submits the given {@link BackgroundTask} to the {@code TaskExecutor}
     * specified at construction time if the given access rights can
     * successfully acquired from the underlying access manager.
     * <P>
     * This method will first attempt to acquire an {@code AccessToken} from
     * the underlying access manager using the specified {@code AccessRequest}.
     * If the access manager refuses this request this method will immediately
     * return {@code null}. If access was granted for this method, the specified
     * background task will be submitted to the {@code TaskExecutor} specified
     * at construction time in the context of the access token. Therefore the
     * {@code execute} method of the task will only be executed while the
     * acquired was not withdrawn.
     * <P>
     * There are in general two ways to cancel the submitted background task:
     * One way is to request cancellation through the specified
     * {@code CancellationToken} and the other is to release the
     * {@code AccessToken} acquired by this method. Releasing the acquired
     * access token will cause the {@code CancellationToken} of the submitted
     * task to signal a cancellation request (or it is possible that the task
     * will not even be executed).
     *
     * @param cancelToken the {@code CancellationToken} which is used to signal
     *   that the background task should be canceled. If the background task
     *   has not yet started, it may not be executed at all (the underlying
     *   {@code TaskExecutor} may not necessarily support it). This argument
     *   cannot be {@code null}.
     * @param request the {@code AccessRequest} used to acquire the
     *   {@code AccessToken} in whose context the task is to be executed. This
     *   argument cannot be {@code null}.
     * @param task the {@code BackgroundTask} to be submitted to the underlying
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @return {@code null} if the access was granted to execute the background
     *   task and it was submitted to the underlying {@code TaskExecutor} or
     *   if access was refused, the access tokens blocking access the specified
     *   request. Note that if this method returns an empty collection, it means
     *   that access was refused but the underlying access manager did not
     *   return the reason of refusal.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public Collection<AccessToken<IDType>> tryExecute(
            CancellationToken cancelToken,
            AccessRequest<? extends IDType, ? extends RightType> request,
            BackgroundTask task) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(request, "request");
        ExceptionHelper.checkNotNullArgument(task, "task");

        AccessResult<IDType> accessResult = accessManager.tryGetAccess(request);
        return tryExecute(cancelToken, accessResult, task);
    }

    private Collection<AccessToken<IDType>> tryExecute(
            CancellationToken cancelToken,
            AccessResult<IDType> accessResult,
            BackgroundTask task) {

        if (accessResult.isAvailable()) {
            boolean submitted = false;
            try {
                doExecuteTask(cancelToken, accessResult.getAccessToken(), task);
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

    private void doExecuteTask(
            CancellationToken cancelToken,
            final AccessToken<IDType> accessToken,
            final BackgroundTask task) {

        CancelableTask executorTask = new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                task.execute(cancelToken, new SwingReporterImpl());
            }
        };

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        final CancellationToken combinedToken = Cancellation.anyToken(cancelSource.getToken(), cancelToken);
        CleanupTask cleanupTask = new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                try {
                    accessToken.release();
                } finally {
                    if (error != null && !(error instanceof OperationCanceledException)) {
                        LOGGER.log(Level.SEVERE, "The backround task has thrown an unexpected exception", error);
                    }
                }
            }
        };
        accessToken.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                cancelSource.getController().cancel();
            }
        });

        TaskExecutor taskExecutor = accessToken.createExecutor(executor);
        taskExecutor.execute(combinedToken, executorTask, cleanupTask);
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
