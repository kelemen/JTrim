package org.jtrim.concurrent.async;

import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncDataLinkConverter#getData(org.jtrim.concurrent.async.AsyncDataListener)
 *
 * @author Kelemen Attila
 */
final class AsyncDataListenerConverter<OldDataType, NewDataType>
implements
        AsyncDataListener<OldDataType> {

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
    public boolean requireData() {
        return wrappedListener.requireData();
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
        StringBuilder result = new StringBuilder(256);
        result.append("Convert using (");
        AsyncFormatHelper.appendIndented(converter, result);
        result.append(")\nto (");
        AsyncFormatHelper.appendIndented(wrappedListener, result);
        result.append(")");

        return result.toString();
    }

}
