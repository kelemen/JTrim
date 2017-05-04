package org.jtrim2.concurrent.async;

import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AsyncQueries#convertResults(AsyncDataQuery, DataConverter)
 *
 * @author Kelemen Attila
 */
final class AsyncDataQueryConverter<NewDataType, QueryArgType, OldDataType>
implements
        AsyncDataQuery<QueryArgType, NewDataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery;
    private final DataConverter<? super OldDataType, ? extends NewDataType> converter;

    public AsyncDataQueryConverter(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        ExceptionHelper.checkNotNullArgument(wrappedQuery, "wrappedQuery");
        ExceptionHelper.checkNotNullArgument(converter, "converter");

        this.wrappedQuery = wrappedQuery;
        this.converter = converter;
    }

    @Override
    public AsyncDataLink<NewDataType> createDataLink(QueryArgType arg) {
        AsyncDataLink<? extends OldDataType> converterLink
                = wrappedQuery.createDataLink(arg);

        return AsyncLinks.convertResultSync(converterLink, converter);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }
}