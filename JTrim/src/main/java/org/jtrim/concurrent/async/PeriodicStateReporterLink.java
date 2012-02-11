package org.jtrim.concurrent.async;

import java.util.concurrent.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDatas#createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit)
 *
 * @author Kelemen Attila
 */
final class PeriodicStateReporterLink<DataType>
        implements AsyncDataLink<DataType> {

    private static final ScheduledExecutorService reportTimer;

    static {
        reportTimer = Executors.newSingleThreadScheduledExecutor(
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

        this.reportExecutor = reportExecutor;
        this.wrappedLink = wrappedLink;
        this.reporter = reporter;
        this.reportPeriodUnit = periodUnit;
        this.reportPeriod = period;
    }

    @Override
    public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener) {
        DataStateListener<DataType> listenerWrapper;
        listenerWrapper = new DataStateListener<>(dataListener);
        AsyncDataController result = wrappedLink.getData(listenerWrapper);

        ReportTask task = new ReportTask(listenerWrapper, dataListener, result);
        task.execute();

        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
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

            super(reportTimer, reportPeriod, reportPeriodUnit);

            this.listenerWrapper = listenerWrapper;
            this.listener = listener;
            this.controller = controller;

            this.doReportTask = new Runnable() {
                @Override
                public void run() {
                    reporter.reportState(wrappedLink,
                            ReportTask.this.listener,
                            ReportTask.this.controller);
                }
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
            AsyncDataListener<DataType> {

        private final AsyncDataListener<? super DataType> wrappedListener;
        private volatile boolean finished;

        public DataStateListener(AsyncDataListener<? super DataType> wrappedListener) {
            this.wrappedListener = wrappedListener;
            this.finished = false;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean requireData() {
            return wrappedListener.requireData();
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
