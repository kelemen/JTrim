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
final class AsyncDataQueryConverter<NewDataType, QueryArgType, OldDataType>
implements
        AsyncDataQuery<QueryArgType, NewDataType> {

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

        return AsyncDatas.convertResult(converterLink, converter);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }
}
