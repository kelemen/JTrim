/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AsyncDataConverter<InputType, ResultType> {
    private final DataConverter<InputType, ResultType> converter;
    private final ExecutorService executor;

    public AsyncDataConverter(
            DataConverter<InputType, ResultType> converter,
            ExecutorService executor) {

        ExceptionHelper.checkNotNullArgument(converter, "converter");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.converter = converter;
        this.executor = executor;
    }

    public Future<ResultType> submit(InputType input) {
        return executor.submit(new CallableWithArgument<>(input, converter));
    }

    public DataConverter<InputType, ResultType> getConverter() {
        return converter;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Use converter: ");
        AsyncFormatHelper.appendIndented(converter, result);
        result.append("\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
    }

    private static class CallableWithArgument<InputType, ResultType> implements
            Callable<ResultType> {

        private final InputType input;
        private final DataConverter<InputType, ResultType> converter;

        public CallableWithArgument(
                InputType input,
                DataConverter<InputType, ResultType> converter) {

            this.input = input;
            this.converter = converter;
        }

        @Override
        public ResultType call() throws Exception {
            return converter.convertData(input);
        }
    }
}
