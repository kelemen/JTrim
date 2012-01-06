/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.concurrent.locks.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class SafeDataListener<DataType>
implements
        AsyncDataListener<OrderedData<DataType>> {

    private final AsyncDataListener<? super DataType> wrappedListener;
    private final InOrderScheduledSyncExecutor eventScheduler;

    private final Runnable dataForwardTask;

    private final Lock dataLock;
    private OrderedData<DataType> lastUnsent;

    // The following fields are confined to the eventScheduler
    private volatile boolean done; // also read by requireData()
    private boolean forwardedData; // true if lastSentIndex is meaningful
    private long lastSentIndex;

    public SafeDataListener(AsyncDataListener<? super DataType> wrappedListener) {
        ExceptionHelper.checkNotNullArgument(wrappedListener, "wrappedListener");

        this.wrappedListener = wrappedListener;
        this.eventScheduler = new InOrderScheduledSyncExecutor();
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

    private void storeData(OrderedData<DataType> data) {
        ExceptionHelper.checkNotNullArgument(data, "data");

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

    @Override
    public boolean requireData() {
        return !done && wrappedListener.requireData();
    }

    @Override
    public void onDataArrive(OrderedData<DataType> data) {
        storeData(data);
        eventScheduler.execute(dataForwardTask);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        eventScheduler.execute(new DoneForwardTask(report));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("MakeSafe (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }

    private class DataForwardTask implements Runnable {
        @Override
        public void run() {
            assert eventScheduler.isCurrentThreadExecuting();

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

            wrappedListener.onDataArrive(data.getRawData());
        }
    }

    private class DoneForwardTask implements Runnable {
        private final AsyncReport report;

        public DoneForwardTask(AsyncReport report) {
            this.report = report;
        }

        @Override
        public void run() {
            assert eventScheduler.isCurrentThreadExecuting();

            if (done) {
                // Data sending was already terminated.
                return;
            }

            done = true;
            wrappedListener.onDoneReceive(report);
        }
    }
}
