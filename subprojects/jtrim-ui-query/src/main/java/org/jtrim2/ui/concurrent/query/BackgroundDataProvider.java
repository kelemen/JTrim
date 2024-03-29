package org.jtrim2.ui.concurrent.query;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jtrim2.access.AccessManager;
import org.jtrim2.access.AccessRequest;
import org.jtrim2.access.AccessResult;
import org.jtrim2.access.AccessToken;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.query.AsyncDataController;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncDataQuery;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataState;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.ui.concurrent.UiExecutorProvider;

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
 * of the listeners will be called in the context of the access token. Both the
 * {@code onDataArrive} and the {@code onDoneReceive} method is called from the
 * UI Thread.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I> but they are
 * non-blocking quick methods allowed to be called from the UI thread.
 *
 * @param <IDType> the type of the request ID of the underlying access manager
 * @param <RightType> the type of the rights handled by the underlying access
 *   manager
 */
public final class BackgroundDataProvider<IDType, RightType> {
    private final AccessManager<IDType, RightType> accessManager;
    private final UiExecutorProvider uiExecutorProvider;

    /**
     * Creates a new {@code BackgroundDataProvider} with the given
     * {@code AccessManager}.
     *
     * @param accessManager the {@code AccessManager} from which access tokens
     *   are requested to transfer data in their context. This argument cannot
     *   be {@code null}.
     * @param uiExecutorProvider a factory to create executors running tasks on the
     *   UI thread of the associated UI framework. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified access manager is
     *   {@code null}
     */
    public BackgroundDataProvider(
            AccessManager<IDType, RightType> accessManager,
            UiExecutorProvider uiExecutorProvider) {
        this.accessManager = Objects.requireNonNull(accessManager, "accessManager");
        this.uiExecutorProvider = Objects.requireNonNull(uiExecutorProvider, "uiExecutorProvider");
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
        return new UiDataQuery<>(request, wrappedQuery);
    }

    /**
     * Creates an {@code AsyncDataLink} which forwards every requested to the
     * specified {@code AsyncDataLink} but delivers the retrieved data in the
     * context of an access token of the access manager of this
     * {@code BackgroundDataProvider} and also on the UI thread.
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
     * Both of the above mentioned methods are always called from the UI thread.
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
        return new UiDataLink<>(request, wrappedLink);
    }

    private class UiDataQuery<QueryArgType, DataType>
    implements
            AsyncDataQuery<QueryArgType, DataType> {

        private final AccessRequest<? extends IDType, ? extends RightType> request;
        private final AsyncDataQuery<QueryArgType, DataType> wrappedQuery;

        public UiDataQuery(
                AccessRequest<? extends IDType, ? extends RightType> request,
                AsyncDataQuery<QueryArgType, DataType> wrappedQuery) {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(wrappedQuery, "wrappedQuery");

            this.request = request;
            this.wrappedQuery = wrappedQuery;
        }

        @Override
        public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
            return createLink(request, wrappedQuery.createDataLink(arg));
        }
    }

    private class UiDataLink<DataType> implements AsyncDataLink<DataType> {
        private final AccessRequest<? extends IDType, ? extends RightType> request;
        private final AsyncDataLink<DataType> wrappedLink;

        public UiDataLink(
                AccessRequest<? extends IDType, ? extends RightType> request,
                AsyncDataLink<DataType> wrappedLink) {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(wrappedLink, "wrappedLink");

            this.request = request;
            this.wrappedLink = wrappedLink;
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                final AsyncDataListener<? super DataType> dataListener) {
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(dataListener, "dataListener");

            AccessResult<IDType> accessResult = accessManager.tryGetAccess(request);
            if (!accessResult.isAvailable()) {
                TaskExecutor executor = uiExecutorProvider.getSimpleExecutor(false);
                executor.execute(() -> {
                    dataListener.onDoneReceive(AsyncReport.CANCELED);
                });
                return PredefinedState.ACCESS_DENIED;
            }

            final AccessToken<?> accessToken = accessResult.getAccessToken();
            final CancellationSource cancelSource = Cancellation.createCancellationSource();
            CancellationToken childToken = Cancellation.anyToken(cancelSource.getToken(), cancelToken);
            Runnable cleanupTask = accessToken::release;

            childToken.addCancellationListener(accessToken::release);
            accessToken.addReleaseListener(cancelSource.getController()::cancel);

            try {
                UiDataListener listener = new UiDataListener(dataListener, accessToken, cleanupTask);
                return wrappedLink.getData(childToken, listener);
            } catch (Throwable ex) {
                cleanupTask.run();
                throw ex;
            }
        }

        private class UiDataListener implements AsyncDataListener<DataType> {
            private final AsyncDataListener<? super DataType> dataListener;
            private final Runnable cleanupTask;

            private final TaskExecutor uiExecutor;
            private final TaskExecutor tokenExecutor;
            private final UpdateTaskExecutor dataExecutor;

            public UiDataListener(
                    AsyncDataListener<? super DataType> dataListener,
                    AccessToken<?> accessToken,
                    Runnable cleanupTask) {
                this.dataListener = dataListener;
                this.uiExecutor = uiExecutorProvider.getStrictExecutor(true);
                this.tokenExecutor = accessToken.createExecutor(uiExecutor);
                this.dataExecutor = new GenericUpdateTaskExecutor(tokenExecutor);
                this.cleanupTask = cleanupTask;
            }

            @Override
            public void onDataArrive(final DataType data) {
                dataExecutor.execute(() -> {
                    dataListener.onDataArrive(data);
                });
            }

            @Override
            public void onDoneReceive(final AsyncReport report) {
                AtomicBoolean forwarded = new AtomicBoolean(false);
                Consumer<AsyncReport> doneForwarder = idempotentConsumer(dataListener::onDoneReceive);
                tokenExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> {
                    doneForwarder.accept(report);
                    forwarded.set(true);
                }).whenComplete((result, error) -> {
                    try {
                        if (!forwarded.get()) {
                            uiExecutorProvider.getSimpleExecutor(false).execute(() -> {
                                doneForwarder.accept(AsyncReport.getReport(report.getException()));
                            });
                        }
                    } finally {
                        cleanupTask.run();
                    }
                });
            }
        }
    }

    private static <T> Consumer<T> idempotentConsumer(Consumer<T> wrapped) {
        AtomicBoolean called = new AtomicBoolean(false);
        return (arg) -> {
            if (called.compareAndSet(false, true)) {
                wrapped.accept(arg);
            }
        };
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
