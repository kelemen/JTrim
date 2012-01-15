package org.jtrim.concurrent.async;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @see AsyncDatas#makeSafeListener(org.jtrim.concurrent.async.AsyncDataListener)
 *
 * @author Kelemen Attila
 */
final class DataOrdererListener<DataType>
        implements AsyncDataListener<DataType> {

    private final AsyncDataListener<? super OrderedData<DataType>> wrappedListener;
    private final AtomicLong index;

    public DataOrdererListener(AsyncDataListener<? super OrderedData<DataType>> wrappedListener) {
        this.wrappedListener = wrappedListener;
        // Long.MIN_VALUE would be less likely to overflow but using 0 is
        // better for debugging purposes.
        this.index = new AtomicLong(0);
    }

    @Override
    public boolean requireData() {
        return wrappedListener.requireData();
    }

    @Override
    public void onDataArrive(DataType newData) {
        long currentIndex = index.getAndIncrement();
        wrappedListener.onDataArrive(new OrderedData<>(currentIndex, newData));
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        wrappedListener.onDoneReceive(report);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Order datas (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }
}
