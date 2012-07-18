package org.jtrim.swing.concurrent.async;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.ChildCancellationSource;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a class that allows to create {@code AsyncDataQuery} and
 * {@code AsyncDataLink} instances which only provides data until a requested
 * {@link AccessToken} is available.
 * <P>
 * Every {@code BackgroundDataProvider} has an associated {@link AccessManager}
 * and data requested from a data query or data link created by the
 * {@code BackgroundDataProvider} will be provided in the context of an
 * access token of the associated access manager. That is, the
 * {@link AsyncDataListener#onDataArrive(Object) onDataArrive(DataType)} method
 * of the listeners will be called in the context of the access token.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I> but they are
 * non-blocking quick methods allowed to be called from the
 * <I>AWT Event Dispatch Thread</I>.
 *
 * @param <IDType> the type of the request ID of the underlying access manager
 * @param <RightType> the type of the rights handled by the underlying access
 *   manager
 *
 * @author Kelemen Attila
 */
public final class BackgroundDataProvider<IDType, RightType> {
    private final AccessManager<IDType, RightType> accessManager;

    /**
     * Creates a new {@code BackgroundDataProvider} with the given
     * {@code AccessManager}.
     *
     * @param accessManager the {@code AccessManager} from which access tokens
     *   are requested to transfer data in their context. This argument cannot
     *   be {@code null}.
     *
     * @throws NullPointerException thrown if the specified access manager is
     *   {@code null}
     */
    public BackgroundDataProvider(AccessManager<IDType, RightType> accessManager) {
        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");

        this.accessManager = accessManager;
    }

    /**
     * Creates an {@code AsyncDataQuery} which forwards every requested to the
     * specified {@code AsyncDataQuery} but delivers the retrieved data in the
     * context of an access token of the access manager of this
     * {@code BackgroundDataProvider}.
     * <P>
     * The returned data query will simply wrap the data links of the specified
     * data query using the {@link #createLink(AccessRequest, AsyncDataLink) createLink}
     * method. So everything what holds for the links created by the
     * {@code createLink} holds for the data links of the returned data query.
     *
     * @param <QueryArgType> the type of the input of the query
     * @param <DataType> the type of the data to be retrieved
     * @param request the {@code AccessRequest} used to acquire the required
     *   access token in which context the data will be provided. This argument
     *   cannot be {@code null}.
     * @param wrappedQuery the {@code AsyncDataQuery} actually used to retrieve
     *   the data requested by the returned {@code AsyncDataQuery}. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataQuery} which forwards every requested to the
     *   specified {@code AsyncDataQuery} but delivers the retrieved data in the
     *   context of an access token of the access manager of this
     *   {@code BackgroundDataProvider}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #createLink(AccessRequest, AsyncDataLink)
     */
    public <QueryArgType, DataType> AsyncDataQuery<QueryArgType, DataType> createQuery(
            AccessRequest<? extends IDType, ? extends RightType> request,
            AsyncDataQuery<QueryArgType, DataType> wrappedQuery) {
        return new SwingDataQuery<>(request, wrappedQuery);
    }

    /**
     * Creates an {@code AsyncDataLink} which forwards every requested to the
     * specified {@code AsyncDataLink} but delivers the retrieved data in the
     * context of an access token of the access manager of this
     * {@code BackgroundDataProvider}.
     * <P>
     * The required access token is attempted to be retrieved every time the
     * data is requested (i.e.: {@code getData} is invoked). In case the access
     * token cannot be acquired, the data transferring is automatically
     * canceled. If the access token can be acquired but the token is released
     * while retrieving the data, the data retrieval process is canceled and no
     * more data will be transfered to the listener. Note however that the
     * wrapped data link may not respond to this cancellation request and may
     * actually continue retrieving data but this retrieved data will not be
     * forwarded to the listener of the data retrieval.
     * <P>
     * The listener is used the following way:
     * <P>
     * The {@code onDataArrive} method
     * is always called in the context of the acquired access token (and
     * therefore the access token will remain acquired at least until the
     * {@code onDataArrive} method returns.
     * <P>
     * The {@code onDoneReceive} method is attempted to be called in the context
     * of the access token. If the access token has been released, the
     * {@code onDoneReceive} method is called regardless but not from the
     * context of the access token. In this later case, the {@code AsyncReport}
     * will signal that the data retrieval process has been canceled. Therefore
     * if the {@link AsyncReport#isCanceled() isCanceled()} method of the
     * {@code AsyncReport} returns {@code true}, the {@code onDoneReceive}
     * method has been called in the context of the access token (the opposite
     * may not be true).
     * <P>
     * Both of the above mentioned methods are always called from the
     * <I>AWT Event Dispatch Thread</I>.
     *
     * @param <DataType> the type of the data to be retrieved
     * @param request the {@code AccessRequest} used to acquire the required
     *   access token in which context the data will be provided. This argument
     *   cannot be {@code null}.
     * @param wrappedLink the {@code AsyncDataLink} actually used to retrieve
     *   the data requested by the returned {@code AsyncDataLink}. This argument
     *   cannot be {@code null}.
     * @return the {@code AsyncDataLink} which forwards every requested to the
     *   specified {@code AsyncDataLink} but delivers the retrieved data in the
     *   context of an access token of the access manager of this
     *   {@code BackgroundDataProvider}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #createQuery(AccessRequest, AsyncDataQuery)
     */
    public <DataType> AsyncDataLink<DataType> createLink(
            AccessRequest<? extends IDType, ? extends RightType> request,
            AsyncDataLink<DataType> wrappedLink) {
        return new SwingDataLink<>(request, wrappedLink);
    }

    private class SwingDataQuery<QueryArgType, DataType>
    implements
            AsyncDataQuery<QueryArgType, DataType> {

        private final AccessRequest<? extends IDType, ? extends RightType> request;
        private final AsyncDataQuery<QueryArgType, DataType> wrappedQuery;

        public SwingDataQuery(
                AccessRequest<? extends IDType, ? extends RightType> request,
                AsyncDataQuery<QueryArgType, DataType> wrappedQuery) {
            ExceptionHelper.checkNotNullArgument(request, "request");
            ExceptionHelper.checkNotNullArgument(wrappedQuery, "wrappedQuery");

            this.request = request;
            this.wrappedQuery = wrappedQuery;
        }

        @Override
        public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
            return createLink(request, wrappedQuery.createDataLink(arg));
        }
    }

    private class SwingDataLink<DataType> implements AsyncDataLink<DataType> {
        private final AccessRequest<? extends IDType, ? extends RightType> request;
        private final AsyncDataLink<DataType> wrappedLink;

        public SwingDataLink(
                AccessRequest<? extends IDType, ? extends RightType> request,
                AsyncDataLink<DataType> wrappedLink) {
            ExceptionHelper.checkNotNullArgument(request, "request");
            ExceptionHelper.checkNotNullArgument(wrappedLink, "wrappedLink");

            this.request = request;
            this.wrappedLink = wrappedLink;
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                final AsyncDataListener<? super DataType> dataListener) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            AccessResult<IDType> accessResult = accessManager.tryGetAccess(request);
            if (!accessResult.isAvailable()) {
                dataListener.onDoneReceive(AsyncReport.CANCELED);
                return PredefinedState.ACCESS_DENIED;
            }

            final AccessToken<?> accessToken = accessResult.getAccessToken();
            final ChildCancellationSource cancelSource = Cancellation.createChildCancellationSource(cancelToken);
            CancellationToken childToken = cancelSource.getToken();
            Runnable cleanupTask = new Runnable() {
                @Override
                public void run() {
                    accessToken.release();
                    cancelSource.detachFromParent();
                }
            };

            childToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    accessToken.release();
                }
            });
            accessToken.addReleaseListener(new Runnable() {
                @Override
                public void run() {
                    cancelSource.getController().cancel();
                }
            });

            try {
                SwingDataListener listener = new SwingDataListener(
                        dataListener, accessToken, cleanupTask);
                return wrappedLink.getData(childToken, listener);
            } catch (Throwable ex) {
                cleanupTask.run();
                throw ex;
            }
        }

        private class SwingDataListener implements AsyncDataListener<DataType> {
            private final AsyncDataListener<? super DataType> dataListener;
            private final Runnable cleanupTask;

            private final TaskExecutor swingExecutor;
            private final TaskExecutor tokenExecutor;
            private final UpdateTaskExecutor dataExecutor;

            public SwingDataListener(
                    AsyncDataListener<? super DataType> dataListener,
                    AccessToken<?> accessToken,
                    Runnable cleanupTask) {
                this.dataListener = dataListener;
                this.swingExecutor = SwingTaskExecutor.getStrictExecutor(true);
                this.tokenExecutor = accessToken.createExecutor(swingExecutor);
                this.dataExecutor = new GenericUpdateTaskExecutor(tokenExecutor);
                this.cleanupTask = cleanupTask;
            }

            @Override
            public boolean requireData() {
                return dataListener.requireData();
            }

            @Override
            public void onDataArrive(final DataType data) {
                dataExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        dataListener.onDataArrive(data);
                    }
                });
            }

            @Override
            public void onDoneReceive(final AsyncReport report) {
                final AtomicReference<AsyncReport> reportRef = new AtomicReference<>(report);
                final CancelableTask doneForwarder = Tasks.runOnceCancelableTask(new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        dataListener.onDoneReceive(reportRef.get());
                    }
                }, false);

                tokenExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, doneForwarder, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        try {
                            reportRef.set(AsyncReport.getReport(report.getException(), true));
                            doneForwarder.execute(Cancellation.UNCANCELABLE_TOKEN);
                        } finally {
                            cleanupTask.run();
                        }
                    }
                });
            }
        }
    }

    private enum PredefinedState implements AsyncDataController {
        ACCESS_DENIED("Access Denied", 0.0);

        private final AsyncDataState state;

        private PredefinedState(String stateStr, double progress) {
            this.state = new SimpleDataState(stateStr, progress);
        }

        @Override
        public void controlData(Object controlArg) {
        }

        @Override
        public AsyncDataState getDataState() {
            return state;
        }
    }
}
