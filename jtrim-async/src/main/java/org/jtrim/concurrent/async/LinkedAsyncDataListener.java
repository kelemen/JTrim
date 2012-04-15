package org.jtrim.concurrent.async;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.concurrent.InOrderScheduledSyncExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see LinkedAsyncDataLink#getData(AsyncDataListener)
 *
 * @author Kelemen Attila
 */
final class LinkedAsyncDataListener<DataType>
implements
        AsyncDataListener<DataType>, AsyncDataController {

    private final QueryAndOutput<?, DataType> queryAndOutput;

    public <SourceDataType> LinkedAsyncDataListener(
            AsyncDataState firstState,
            AsyncDataQuery<? super DataType, ? extends SourceDataType> query,
            AsyncDataListener<? super SourceDataType> outputListener) {

        this.queryAndOutput = new QueryAndOutput<>(firstState, query, outputListener);
    }

    @Override
    public void onDataArrive(DataType data) {
        queryAndOutput.onDataArrive(data);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        queryAndOutput.onDoneReceive(report);
    }

    @Override
    public boolean requireData() {
        return queryAndOutput.requireData();
    }

    @Override
    public void cancel() {
        queryAndOutput.cancel();
    }

    @Override
    public void controlData(Object controlArg) {
        queryAndOutput.controlData(controlArg);
    }

    @Override
    public AsyncDataState getDataState() {
        return queryAndOutput.getDataState();
    }

    @Override
    public String toString() {
        return queryAndOutput.toString();
    }

    private static class QueryAndOutput<SourceDataType, DataType> {
        private final AsyncDataQuery<? super DataType, ? extends SourceDataType> query;
        private final AsyncDataListener<? super SourceDataType> outputListener;

        private final Lock mainLock;
        private final InOrderScheduledSyncExecutor eventScheduler;
        private final Runnable dataForwarderTask;

        private boolean initializedController;
        private InitLaterDataController currentController;
        private Object currentSession;
        private boolean canceled;

        private DataRef<SourceDataType> unsentData;

        private AsyncReport sessionReport;
        private AsyncReport endReport;

        // Writing this field is confined to the eventScheduler
        private volatile boolean finished; // read in requireData()

        public QueryAndOutput(
                AsyncDataState firstState,
                AsyncDataQuery<? super DataType, ? extends SourceDataType> query,
                AsyncDataListener<? super SourceDataType> outputListener) {

            ExceptionHelper.checkNotNullArgument(query, "query");
            ExceptionHelper.checkNotNullArgument(outputListener, "outputListener");

            this.query = query;
            this.outputListener = outputListener;

            this.mainLock = new ReentrantLock();
            this.eventScheduler = new InOrderScheduledSyncExecutor();
            this.dataForwarderTask = new DataForwardTask();

            this.currentSession = null;
            this.currentController = new InitLaterDataController(firstState);
            this.initializedController = false;

            this.unsentData = null;

            this.sessionReport = null;
            this.endReport = null;
            this.finished = false;
            this.canceled = false;
        }

        private void storeData(SourceDataType data, Object session) {
            DataRef<SourceDataType> dataRef = new DataRef<>(data);

            mainLock.lock();
            try {
                if (session == currentSession) {
                    unsentData = dataRef;
                }
            } finally {
                mainLock.unlock();
            }
        }

        private DataRef<SourceDataType> pollData() {
            DataRef<SourceDataType> result;

            mainLock.lock();
            try {
                result = unsentData;
                unsentData = null;
            } finally {
                mainLock.unlock();
            }

            return result;
        }

        public boolean requireData() {
            return !finished && outputListener.requireData();
        }

        private static Object newSession() {
            return new Object();
        }

        public void onDataArrive(DataType data) {
            final Object session;
            InitLaterDataController newController;
            newController = new InitLaterDataController(getDataState());

            AsyncDataController lastController;
            mainLock.lock();
            try {
                if (!initializedController) {
                    newController = currentController;
                    currentController = null;
                }

                lastController = currentController;

                currentSession = newSession();
                sessionReport = null;
                session = currentSession;

                if (canceled) {
                    newController = null;
                }
                else {
                    currentController = newController;
                }

                initializedController = true;
            } finally {
                mainLock.unlock();
            }

            if (lastController != null) {
                lastController.cancel();
            }

            if (newController != null) {
                AsyncDataController queryController;
                queryController = query.createDataLink(data)
                        .getData(new QueryListener(session));
                newController.initController(queryController);
            }
        }

        public void onDoneReceive(AsyncReport report) {
            ExceptionHelper.checkNotNullArgument(report, "report");

            eventScheduler.execute(new EndTask(report));
        }

        public void controlData(Object controlArg) {
            AsyncDataController controller;
            mainLock.lock();
            try {
                controller = currentController;
            } finally {
                mainLock.unlock();
            }

            controller.controlData(controlArg);
        }

        public void cancel() {
            AsyncDataController controller;
            mainLock.lock();
            try {
                controller = currentController;
                canceled = true;
            } finally {
                mainLock.unlock();
            }

            controller.cancel();

            eventScheduler.execute(new EndTask(AsyncReport.CANCELED));
        }

        public AsyncDataState getDataState() {
            AsyncDataController controller;
            mainLock.lock();
            try {
                controller = currentController;
            } finally {
                mainLock.unlock();
            }

            return controller.getDataState();
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(256);
            result.append("Convert from ");
            AsyncFormatHelper.appendIndented(outputListener, result);
            result.append("\nusing ");
            AsyncFormatHelper.appendIndented(query, result);

            return result.toString();
        }

        /**
         * Confined to the eventScheduler.
         */
        private void tryEndReceive() {
            assert eventScheduler.isCurrentThreadExecuting();

            AsyncReport report1;
            AsyncReport report2;

            mainLock.lock();
            try {
                if (sessionReport == null || endReport == null || finished) {
                    return;
                }

                finished = true;
                report1 = sessionReport;
                report2 = endReport;
            } finally {
                mainLock.unlock();
            }

            boolean wasCanceled = report1.isCanceled() || report2.isCanceled();
            Throwable ex1 = report1.getException();
            Throwable ex2 = report2.getException();

            Throwable exception;
            if (ex1 == null) {
                exception = ex2;
            }
            else if (ex2 == null) {
                exception = ex1;
            }
            else {
                exception = new DataTransferException();
                exception.addSuppressed(ex1);
                exception.addSuppressed(ex2);
            }

            AsyncReport report = AsyncReport.getReport(exception, wasCanceled);
            outputListener.onDoneReceive(report);
        }

        // The following tasks (Runnable) are confined to the eventScheduler.

        private class DataForwardTask implements Runnable {
            @Override
            public void run() {
                assert eventScheduler.isCurrentThreadExecuting();

                DataRef<SourceDataType> dataRef = pollData();
                if (dataRef == null || finished) {
                    return;
                }

                outputListener.onDataArrive(dataRef.getData());
            }
        }

        private class SessionEndTask implements Runnable {
            private final Object session;
            private final AsyncReport report;

            public SessionEndTask(Object session, AsyncReport report) {
                assert report != null;

                this.session = session;
                this.report = report;
            }

            @Override
            public void run() {
                assert eventScheduler.isCurrentThreadExecuting();

                mainLock.lock();
                try {
                    if (session == currentSession && sessionReport == null) {
                        sessionReport = report;
                    }
                } finally {
                    mainLock.unlock();
                }

                tryEndReceive();
            }
        }

        private class EndTask implements Runnable {
            private final AsyncReport report;

            public EndTask(AsyncReport report) {
                assert report != null;

                this.report = report;
            }

            @Override
            public void run() {
                assert eventScheduler.isCurrentThreadExecuting();

                mainLock.lock();
                try {
                    if (endReport == null) {
                        endReport = report;
                    }
                } finally {
                    mainLock.unlock();
                }

                tryEndReceive();
            }
        }

        private class QueryListener implements AsyncDataListener<SourceDataType> {
            private final Object session;

            public QueryListener(Object session) {
                this.session = session;
            }

            @Override
            public boolean requireData() {
                return QueryAndOutput.this.requireData();
            }

            @Override
            public void onDataArrive(SourceDataType data) {
                storeData(data, session);
                eventScheduler.execute(dataForwarderTask);
            }

            @Override
            public void onDoneReceive(AsyncReport report) {
                ExceptionHelper.checkNotNullArgument(report, "report");

                eventScheduler.execute(new SessionEndTask(session, report));
            }
        }

        private static class DataRef<T> {
            private final T data;

            public DataRef(T data) {
                this.data = data;
            }

            public T getData() {
                return data;
            }
        }
    }
}
