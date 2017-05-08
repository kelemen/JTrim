package org.jtrim2.concurrent.async;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see AsyncLinks#convertResult(AsyncDataLink, DataConverter)
 *
 * @author Kelemen Attila
 */
final class AsyncDataLinkConverter<OldDataType, NewDataType>
        implements AsyncDataLink<NewDataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataLink<? extends OldDataType> wrappedDataLink;
    private final DataConverter<? super OldDataType, ? extends NewDataType> converter;

    public AsyncDataLinkConverter(
            AsyncDataLink<? extends OldDataType> wrappedDataLink,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        Objects.requireNonNull(wrappedDataLink, "wrappedDataLink");
        Objects.requireNonNull(converter, "converter");

        this.wrappedDataLink = wrappedDataLink;
        this.converter = converter;
    }

    @Override
    public AsyncDataController getData(
            CancellationToken cancelToken,
            AsyncDataListener<? super NewDataType> dataListener) {
        AsyncDataListener<OldDataType> converterListener;

        converterListener = new AsyncDataListenerConverter<>(
                dataListener, converter);

        return wrappedDataLink.getData(cancelToken, converterListener);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(wrappedDataLink, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }

}
