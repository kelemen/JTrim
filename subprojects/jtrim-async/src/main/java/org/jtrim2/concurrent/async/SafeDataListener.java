package org.jtrim2.concurrent.async;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutors;

/**
 * @see AsyncHelper#makeSafeOrderedListener(AsyncDataListener)
 */
final class SafeDataListener<DataType>
implements
        AsyncDataListener<OrderedData<DataType>>,
        PossiblySafeListener {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataListener<? super DataType> wrappedListener;
    private final ContextAwareTaskExecutor eventScheduler;

    private final CancelableTask dataForwardTask;

    private final Lock dataLock;
    private OrderedData<DataType> lastUnsent;

    // The following fields are confined to the eventScheduler
    private boolean done;
    private boolean forwardedData; // true if lastSentIndex is meaningful
    private long lastSentIndex;

    public SafeDataListener(AsyncDataListener<? super DataType> wrappedListener) {
        Objects.requireNonNull(wrappedListener, "wrappedListener");

        this.wrappedListener = wrappedListener;
        this.eventScheduler = TaskExecutors.inOrderSyncExecutor();
        this.dataLock = new ReentrantLock();
        this.lastUnsent = null;

        // this value does not matter but setting it to the maximum
        // will always cause failure to forward datas if we ignore
        // forwardedData.
        this.lastSentIndex = Long.MAX_VALUE;
        this.forwardedData = false;
        this.done = false;

        this.dataForwardTask = new DataForwardTask();
    }

    @Override
    public boolean isSafeListener() {
        return true;
    }

    private void storeData(OrderedData<DataType> data) {
        Objects.requireNonNull(data, "data");

        dataLock.lock();
        try {
            if (lastUnsent == null || lastUnsent.getIndex() < data.getIndex()) {
                lastUnsent = data;
            }
        } finally {
            dataLock.unlock();
        }
    }

    private OrderedData<DataType> pollData() {
        OrderedData<DataType> result;

        dataLock.lock();
        try {
            result = lastUnsent;
            lastUnsent = null;
        } finally {
            dataLock.unlock();
        }

        return result;
    }

    private void submitEventTask(CancelableTask task) {
        eventScheduler.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
    }

    @Override
    public void onDataArrive(OrderedData<DataType> data) {
        storeData(data);
        submitEventTask(dataForwardTask);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        submitEventTask(new DoneForwardTask(report));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("MakeSafe (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }

    private class DataForwardTask implements CancelableTask {
        @Override
        public void execute(CancellationToken cancelToken) {
            assert eventScheduler.isExecutingInThis();

            OrderedData<DataType> data = pollData();
            if (data == null) {
                // A data was overwritten by a newever one.
                return;
            }

            if (forwardedData && data.getIndex() <= lastSentIndex) {
                // We have already sent a newer data, so this can be safely
                // ignored.
                return;
            }

            if (done) {
                // Data sending was terminated.
                return;
            }

            lastSentIndex = data.getIndex();
            forwardedData = true;
            wrappedListener.onDataArrive(data.getRawData());
        }
    }

    private class DoneForwardTask implements CancelableTask {
        private final AsyncReport report;

        public DoneForwardTask(AsyncReport report) {
            this.report = report;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            assert eventScheduler.isExecutingInThis();

            if (done) {
                // Data sending was already terminated.
                return;
            }

            done = true;
            wrappedListener.onDoneReceive(report);
        }
    }
}
