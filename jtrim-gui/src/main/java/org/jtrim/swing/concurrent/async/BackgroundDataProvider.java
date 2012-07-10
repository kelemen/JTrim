package org.jtrim.swing.concurrent.async;

import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.ChildCancellationSource;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
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
 *
 * @author Kelemen Attila
 */
public final class BackgroundDataProvider<IDType, RightType> {
    private final AccessManager<IDType, RightType> accessManager;

    public BackgroundDataProvider(AccessManager<IDType, RightType> accessManager) {
        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");

        this.accessManager = accessManager;
    }

    public <QueryArgType, DataType> AsyncDataQuery<QueryArgType, DataType> createQuery(
            AccessRequest<? extends IDType, ? extends RightType> request,
            AsyncDataQuery<QueryArgType, DataType> wrappedQuery) {
        return new SwingDataQuery<>(request, wrappedQuery);
    }

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
            private final AccessToken<?> accessToken;
            private final Runnable cleanupTask;

            private final TaskExecutor swingExecutor;
            private final UpdateTaskExecutor dataExecutor;

            public SwingDataListener(
                    AsyncDataListener<? super DataType> dataListener,
                    AccessToken<?> accessToken,
                    Runnable cleanupTask) {
                this.dataListener = dataListener;
                this.swingExecutor = SwingTaskExecutor.getStrictExecutor(true);
                TaskExecutor tokenExecutor = accessToken.createExecutor(swingExecutor);
                this.dataExecutor = new GenericUpdateTaskExecutor(tokenExecutor);
                this.accessToken = accessToken;
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
            public void onDoneReceive(AsyncReport report) {
                boolean wasCanceled = false;

                try {
                    wasCanceled = accessToken.isReleased();
                    cleanupTask.run();
                } finally {
                    final AsyncReport forwardedReport = wasCanceled
                            ? AsyncReport.getReport(report.getException(), true)
                            : report;
                    swingExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                        @Override
                        public void execute(CancellationToken cancelToken) {
                            dataListener.onDoneReceive(forwardedReport);
                        }
                    }, null);
                }
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
