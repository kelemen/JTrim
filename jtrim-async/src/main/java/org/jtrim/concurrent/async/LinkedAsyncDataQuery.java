package org.jtrim.concurrent.async;

import org.jtrim.utils.ExceptionHelper;

/**
 * @see AsyncQueries#convertResults(AsyncDataQuery, AsyncDataQuery)
 *
 * @author Kelemen Attila
 */
final class LinkedAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<QueryArgType, DataType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final InputAndConverter<QueryArgType, ?, DataType> inputAndConverter;

    public <SecArgType> LinkedAsyncDataQuery(
            AsyncDataQuery<? super QueryArgType, ? extends SecArgType> input,
            AsyncDataQuery<? super SecArgType, ? extends DataType> converter) {
        this.inputAndConverter = new InputAndConverter<>(input, converter);
    }

    @Override
    public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
        return inputAndConverter.createDataLink(arg);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Convert from ");
        AsyncFormatHelper.appendIndented(inputAndConverter.getInput(), result);
        result.append("\nusing ");
        AsyncFormatHelper.appendIndented(inputAndConverter.getConverter(), result);

        return result.toString();
    }

    private static class InputAndConverter<QueryArgType, SecArgType, DataType> {
        private final AsyncDataQuery<? super QueryArgType, ? extends SecArgType> input;
        private final AsyncDataQuery<? super SecArgType, ? extends DataType> converter;

        public InputAndConverter(
                AsyncDataQuery<? super QueryArgType, ? extends SecArgType> input,
                AsyncDataQuery<? super SecArgType, ? extends DataType> converter) {

            ExceptionHelper.checkNotNullArgument(input, "input");
            ExceptionHelper.checkNotNullArgument(converter, "converter");

            this.input = input;
            this.converter = converter;
        }

        public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
            return AsyncLinks.convertResult(input.createDataLink(arg), converter);
        }

        public AsyncDataQuery<?, ?> getConverter() {
            return converter;
        }

        public AsyncDataQuery<?, ?> getInput() {
            return input;
        }
    }
}
