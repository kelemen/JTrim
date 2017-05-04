package org.jtrim2.concurrent.async;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.RepeatingTask;
import org.jtrim2.executor.ExecutorsEx;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AsyncLinks#createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit)
 *
 * @author Kelemen Attila
 */
final class PeriodicStateReporterLink<DataType>
        implements AsyncDataLink<DataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private static final ScheduledExecutorService REPORT_TIMER;

    static {
        REPORT_TIMER = Executors.newSingleThreadScheduledExecutor(
                new ExecutorsEx.NamedThreadFactory(true, "Async State Report Timer"));
    }

    private final UpdateTaskExecutor reportExecutor;
    private final AsyncDataLink<DataType> wrappedLink;
    private final AsyncStateReporter<DataType> reporter;

    private final TimeUnit reportPeriodUnit;
    private final long reportPeriod;

    public PeriodicStateReporterLink(
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {
        this(null, wrappedLink, reporter, period, periodUnit);
    }

    public PeriodicStateReporterLink(
            UpdateTaskExecutor reportExecutor,
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        ExceptionHelper.checkNotNullArgument(wrappedLink, "wrappedLink");
        ExceptionHelper.checkNotNullArgument(reporter, "reporter");
        ExceptionHelper.checkNotNullArgument(periodUnit, "periodUnit");
        ExceptionHelper.checkArgumentInRange(period, 0, Long.MAX_VALUE, "period");

        this.reportExecutor = reportExecutor;
        this.wrappedLink = wrappedLink;
        this.reporter = reporter;
        this.reportPeriodUnit = periodUnit;
        this.reportPeriod = period;
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super DataType> dataListener) {

        DataStateListener<DataType> listenerWrapper;
        listenerWrapper = new DataStateListener<>(dataListener);
        AsyncDataController result = wrappedLink.getData(cancelToken, listenerWrapper);

        ReportTask task = new ReportTask(listenerWrapper, dataListener, result);
        task.execute();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Report state of (");
        AsyncFormatHelper.appendIndented(wrappedLink, result);
        result.append(")");


        return result.toString();
    }

    private class ReportTask extends RepeatingTask {
        private final DataStateListener<DataType> listenerWrapper;

        private final AsyncDataListener<? super DataType> listener;
        private final AsyncDataController controller;
        private final Runnable doReportTask;

        public ReportTask(
                DataStateListener<DataType> listenerWrapper,
                AsyncDataListener<? super DataType> listener,
                AsyncDataController controller) {

            super(REPORT_TIMER, reportPeriod, reportPeriodUnit);

            this.listenerWrapper = listenerWrapper;
            this.listener = listener;
            this.controller = controller;

            this.doReportTask = () -> {
                reporter.reportState(wrappedLink,
                        ReportTask.this.listener,
                        ReportTask.this.controller);
            };
        }

        @Override
        public boolean runAndTest() {
            if (reportExecutor != null) {
                reportExecutor.execute(doReportTask);
            }
            else {
                doReportTask.run();
            }

            return !listenerWrapper.isFinished();
        }
    }

    private static class DataStateListener<DataType>
    implements
            AsyncDataListener<DataType>,
            PossiblySafeListener {

        private final AsyncDataListener<? super DataType> wrappedListener;
        private volatile boolean finished;

        public DataStateListener(AsyncDataListener<? super DataType> wrappedListener) {
            this.wrappedListener = wrappedListener;
            this.finished = false;
        }

        @Override
        public boolean isSafeListener() {
            // I cannot think of a reasonable test for this but it is obviously
            // good.
            return AsyncHelper.isSafeListener(wrappedListener);
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void onDataArrive(DataType newData) {
            wrappedListener.onDataArrive(newData);
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            finished = true;
            wrappedListener.onDoneReceive(report);
        }
    }
}
