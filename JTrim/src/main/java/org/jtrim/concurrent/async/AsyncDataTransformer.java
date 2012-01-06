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
public final class AsyncDataTransformer<DataType> {
    private final DataTransformer<DataType> transformer;
    private final ExecutorService executor;

    public AsyncDataTransformer(
            DataTransformer<DataType> transformer,
            ExecutorService executor) {

        ExceptionHelper.checkNotNullArgument(transformer, "transformer");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.transformer = transformer;
        this.executor = executor;
    }

    public Future<DataType> submit(DataType input) {
        return executor.submit(new CallableWithArgument<>(input, transformer));
    }

    public DataTransformer<DataType> getTransformer() {
        return transformer;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Transformation: ");
        AsyncFormatHelper.appendIndented(transformer, result);
        result.append("\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
    }

    private static class CallableWithArgument<DataType> implements
            Callable<DataType> {

        private final DataType input;
        private final DataTransformer<DataType> transformer;

        public CallableWithArgument(
                DataType input,
                DataTransformer<DataType> transformer) {

            this.input = input;
            this.transformer = transformer;
        }

        @Override
        public DataType call() throws Exception {
            return transformer.transform(input);
        }
    }
}
