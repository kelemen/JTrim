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
final class LinkedAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<QueryArgType, DataType> {

    private final AsyncDataQuery<? super QueryArgType, ?> input;
    private final AsyncDataQuery<Object, ? extends DataType> converter;

    public <SecArgType> LinkedAsyncDataQuery(
            AsyncDataQuery<? super QueryArgType, ? extends SecArgType> input,
            AsyncDataQuery<? super SecArgType, ? extends DataType> converter) {

        ExceptionHelper.checkNotNullArgument(input, "input");
        ExceptionHelper.checkNotNullArgument(converter, "converter");

        // Due to the constraint in the argument list the conversion to the
        // output is always valid.
        @SuppressWarnings("unchecked")
        AsyncDataQuery<Object, ? extends DataType> convertedConverter
                = (AsyncDataQuery<Object, ? extends DataType>)converter;

        this.input = input;
        this.converter = convertedConverter;
    }

    @Override
    public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
        return AsyncDatas.convertResult(input.createDataLink(arg), converter);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(input, result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(converter, result);

        return result.toString();
    }

}
