/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class AsyncDataLinkConverter<OldDataType, NewDataType>
        implements AsyncDataLink<NewDataType> {

    private final AsyncDataLink<? extends OldDataType> wrappedDataLink;
    private final DataConverter<? super OldDataType, ? extends NewDataType> converter;

    public AsyncDataLinkConverter(
            AsyncDataLink<? extends OldDataType> wrappedDataLink,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        ExceptionHelper.checkNotNullArgument(wrappedDataLink, "wrappedDataLink");
        ExceptionHelper.checkNotNullArgument(converter, "converter");

        this.wrappedDataLink = wrappedDataLink;
        this.converter = converter;
    }

    @Override
    public AsyncDataController getData(AsyncDataListener<? super NewDataType> dataListener) {
        AsyncDataListener<OldDataType> converterListener;

        converterListener = new AsyncDataListenerConverter<>(
                dataListener, converter);

        return wrappedDataLink.getData(converterListener);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(wrappedDataLink, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }

}
