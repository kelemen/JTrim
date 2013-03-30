package org.jtrim.concurrent.async;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @see AsyncHelper#makeSafeListener(AsyncDataListener)
 *
 * @author Kelemen Attila
 */
final class DataOrdererListener<DataType>
implements
        AsyncDataListener<DataType>,
        PossiblySafeListener {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataListener<? super OrderedData<DataType>> wrappedListener;
    private final AtomicLong index;

    public DataOrdererListener(AsyncDataListener<? super OrderedData<DataType>> wrappedListener) {
        this.wrappedListener = wrappedListener;
        // Long.MIN_VALUE would be less likely to overflow but using 0 is
        // better for debugging purposes.
        this.index = new AtomicLong(0);
    }

    @Override
    public boolean isSafeListener() {
        return AsyncHelper.isSafeListener(wrappedListener);
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
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Order datas (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }
}
