package org.jtrim.access.query;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jtrim.access.AccessToken;
import org.jtrim.access.RewBase;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.jtrim.concurrent.async.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * An executor which can execute REW (read, evaluate, write) queries.
 * This executor will periodically call the
 * {@link RewQuery#writeState(org.jtrim.concurrent.async.AsyncDataState) RewQuery.writeState(AsyncDataState)}
 * method of the submitted REW query to allow the client to display the
 * current state of the query part of the executed REW query. This
 * implementation will use the same thread for all instances to submit such
 * state reports to the specified write token, so if submitting a task to a
 * write token can wait for an extended period of time that may adversely affect
 * the performance of other REW queries in this JVM. Note that this thread model
 * may change in later implementations.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are completely thread-safe and the methods can be
 * called from any thread.
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I> and
 * do not wait for asynchronous or external tasks to complete.
 *
 * @see RewQuery
 * @see RewQueryExecutor
 * @author Kelemen Attila
 */
public final class AutoReportRewQueryExecutor implements RewQueryExecutor {
    private final long reportPeriodNanos;

    /**
     * Initializes a {@code AutoReportRewQueryExecutor} to report the state
     * of the REW query periodically. Note that specified period is just
     * an approximate value and the implementation may not be able to report
     * in the requested time.
     *
     * @param reportPeriod the period between the calls to report the state
     *   of the executing REW query. The time is measured in the specified time
     *   unit ({@code reportPeriodUnit}). This argument must be non-negative.
     * @param reportPeriodUnit the time unit of {@code reportPeriod}. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code reportPeriod} is
     *   a negative integer
     * @throws NullPointerException thrown if {@code reportPeriodUnit} is
     *   {@code null}
     */
    public AutoReportRewQueryExecutor(
            long reportPeriod, TimeUnit reportPeriodUnit) {
        this.reportPeriodNanos = reportPeriodUnit.toNanos(reportPeriod);
        ExceptionHelper.checkArgumentInRange(this.reportPeriodNanos, 0, Long.MAX_VALUE, "reportPeriodNanos");
    }

    private <I, O> Future<?> executeGeneric(
            RewQuery<I, O> query,
            AccessToken<?> readToken, AccessToken<?> writeToken,
            boolean releaseOnTerminate, boolean readNow) {

        UpdateTaskExecutor updateExecutor
                = new GenericUpdateTaskExecutor(SyncTaskExecutor.getSimpleExecutor());

        AutoReportRewQuery<I, O> futureTask;
        futureTask = new AutoReportRewQuery<>(updateExecutor, query,
                readToken, writeToken, releaseOnTerminate);

        return futureTask.start(readNow);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> execute(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(query, readToken, writeToken, false, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeNow(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(query, readToken, writeToken, false, true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeAndRelease(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(query, readToken, writeToken, true, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> executeNowAndRelease(RewQuery<?, ?> query,
            AccessToken<?> readToken, AccessToken<?> writeToken) {
        return executeGeneric(query, readToken, writeToken, true, true);
    }

    /**
     * Returns the string representation of this REW query executor in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "AutoReportRewQuery{"
                + TimeUnit.NANOSECONDS.toMillis(reportPeriodNanos) + " ms}";
    }

    private static class InitLaterCancelController implements CancellationController {
        private volatile CancellationController cancelController;
        private volatile boolean canceled;

        public InitLaterCancelController() {
            this.cancelController = null;
            this.canceled = true;
        }

        public void setController(CancellationController cancelController) {
            this.cancelController = cancelController;
            if (canceled) {
                cancel();
            }
        }

        @Override
        public void cancel() {
            canceled = true;
            CancellationController currentController = cancelController;
            cancelController = null;
            if (currentController != null) {
                currentController.cancel();
            }
        }
    }

    private class AutoReportRewQuery<InputType, OutputType>
    extends
            RewBase<Void> {

        private final InitLaterCancelController cancelController;

        private final UpdateTaskExecutor queryStateExecutor;

        // sending recieved datas and finishing receiving is done
        // through this executor.
        private final TaskExecutor orderedWriteToken;
        private final UpdateTaskExecutor outputWriter;
        private final RewQuery<InputType, OutputType> query;
        private final TaskExecutor readExecutor;
        private final TaskExecutor writeExecutor;

        public AutoReportRewQuery(
                UpdateTaskExecutor queryStateExecutor,
                RewQuery<InputType, OutputType> query,
                AccessToken<?> readToken,
                AccessToken<?> writeToken,
                boolean releaseOnTerminate) {
            super(readToken, writeToken, releaseOnTerminate);

            this.queryStateExecutor = queryStateExecutor;
            this.cancelController = new InitLaterCancelController();
            // FIXME: Somehow specify the backing executors for read and write
            this.readExecutor = readToken.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            this.writeExecutor = writeToken.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            this.orderedWriteToken = TaskExecutors.inOrderExecutor(writeExecutor);
            this.outputWriter = new GenericUpdateTaskExecutor(orderedWriteToken);
            this.query = query;
        }

        private void setConroller(CancellationController controller) {
            cancelController.setController(controller);
        }

        @Override
        protected Runnable createReadTask() {
            return new Runnable() {
                @Override
                public void run() {
                    InputType input = task.executeSubTask(new InputReader());
                    if (!task.isDone()) {
                        CancellationSource cancelSource = Cancellation.createCancellationSource();
                        AsyncDataController controller;
                        task.executeSubTask(
                                cancelSource.getToken(),
                                new OutputQuery(input),
                                null);

                        setConroller(cancelSource.getController());
                    }
                }
            };
        }

        @Override
        public void onTerminate(Void result, Throwable exception,
                boolean canceled) {
            if (canceled) {
                cancelController.cancel();
            }
        }

        private class InputReader implements Callable<InputType> {
            @Override
            public InputType call() {
                return query.readInput();
            }
        }

        private class StateReporter implements AsyncStateReporter<OutputType> {
            private final UpdateTaskExecutor stateReporter;

            public StateReporter() {
                this.stateReporter = new GenericUpdateTaskExecutor(writeExecutor);
            }

            @Override
            public void reportState(AsyncDataLink<OutputType> dataLink,
                    AsyncDataListener<? super OutputType> dataListener,
                    AsyncDataController controller) {

                AsyncDataState state = controller.getDataState();
                stateReporter.execute(new StateWriter(state));
            }
        }

        private class StateWriter implements Runnable {
            private final AsyncDataState state;

            public StateWriter(AsyncDataState state) {
                this.state = state;
            }

            @Override
            public void run() {
                query.writeState(state);
            }
        }

        private class OutputQuery implements CancelableFunction<AsyncDataController> {
            private final InputType input;

            public OutputQuery(InputType input) {
                this.input = input;
            }

            @Override
            public AsyncDataController execute(CancellationToken cancelToken) {
                AsyncDataLink<OutputType> dataLink;
                dataLink = query.getOutputQuery().createDataLink(input);
                dataLink = AsyncLinks.createStateReporterLink(queryStateExecutor,
                        dataLink, new StateReporter(),
                        reportPeriodNanos, TimeUnit.NANOSECONDS);

                return dataLink.getData(cancelToken, new OutputListener());
            }
        }

        private class OutputListener implements AsyncDataListener<OutputType> {
            @Override
            public boolean requireData() {
                return true;
            }

            @Override
            public void onDataArrive(OutputType data) {
                task.executeSubTask(outputWriter, new OutputForwarder(data));
            }

            @Override
            public void onDoneReceive(final AsyncReport report) {
                orderedWriteToken.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        Throwable error = null;
                        try {
                            task.executeSubTask(new Runnable() {
                                @Override
                                public void run() {
                                    query.doneReceiving(report);
                                }
                            });
                        } catch (Throwable ex) {
                            error = ex;
                        } finally {
                            task.finishTask(null, error, false);
                        }
                    }
                }, null);
            }
        }

        private class OutputForwarder implements Runnable {
            private final OutputType data;

            public OutputForwarder(OutputType data) {
                this.data = data;
            }

            @Override
            public void run() {
                query.writeOutput(data);
            }
        }
    }
}