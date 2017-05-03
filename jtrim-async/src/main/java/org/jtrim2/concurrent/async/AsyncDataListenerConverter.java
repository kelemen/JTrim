package org.jtrim2.concurrent.async;

import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AsyncDataLinkConverter#getData(AsyncDataListener)
 *
 * @author Kelemen Attila
 */
final class AsyncDataListenerConverter<OldDataType, NewDataType>
implements
        AsyncDataListener<OldDataType>,
        PossiblySafeListener {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataListener<? super NewDataType> wrappedListener;
    private final DataConverter<? super OldDataType, ? extends NewDataType> converter;

    public AsyncDataListenerConverter(
            AsyncDataListener<? super NewDataType> wrappedListener,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        ExceptionHelper.checkNotNullArgument(converter, "converter");
        ExceptionHelper.checkNotNullArgument(wrappedListener, "wrappedListener");

        this.wrappedListener = wrappedListener;
        this.converter = converter;
    }

    @Override
    public boolean isSafeListener() {
        return converter.getClass().isAnnotationPresent(StatelessClass.class)
                && AsyncHelper.isSafeListener(wrappedListener);
    }

    @Override
    public void onDataArrive(OldDataType data) {
        NewDataType newData = converter.convertData(data);
        wrappedListener.onDataArrive(newData);
    }

    @Override
    public void onDoneReceive(AsyncReport report) {
        wrappedListener.onDoneReceive(report);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert using (");
        AsyncFormatHelper.appendIndented(converter, result);
        result.append(")\nto (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }

}
