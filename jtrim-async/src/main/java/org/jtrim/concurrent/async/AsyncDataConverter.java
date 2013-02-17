package org.jtrim.concurrent.async;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.TaskFuture;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a conversion of objects and the {@link TaskExecutorService}
 * which should be used to execute the conversion process.
 * <P>
 * This class effectively just holds a {@link DataConverter} and the
 * {@code TaskExecutorService}.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Aside from the {@link #submit(CancellationToken, Object)  submit} method,
 * methods of this class are <I>synchronization transparent</I>. This
 * {@code submit} method however needs to submit a task to the
 * {@code TaskExecutorService} and as such is not
 * <I>synchronization transparent</I>.
 *
 * @param <InputType> the type of the data which is to be converted
 * @param <ResultType> the type of the resulting data after conversion
 *
 * @see AsyncLinks#convertGradually(Object, List) AsyncLinks.convertGradually
 * @see AsyncDataTransformer
 * @author Kelemen Attila
 */
public final class AsyncDataConverter<InputType, ResultType> {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    private final DataConverter<InputType, ResultType> converter;
    private final TaskExecutorService executor;

    /**
     * Creates a new {@code AsyncDataConverter} with the given conversion
     * routine and {@code TaskExecutorService}.
     *
     * @param converter the {@code DataConverter} which defines the conversion
     *   of input datas. This argument cannot be {@code null}.
     * @param executor the {@code TaskExecutorService} which is to be used
     *   execute the {@link DataConverter#convertData(Object) conversion} of the
     *   input data. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public AsyncDataConverter(
            DataConverter<InputType, ResultType> converter,
            TaskExecutorService executor) {

        ExceptionHelper.checkNotNullArgument(converter, "converter");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.converter = converter;
        this.executor = executor;
    }

    /**
     * Submits a task to the wrapped {@link #getExecutor() executor} which will
     * carry out the {@link #getConverter() data conversion routine}.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. This is the
     *   {@code CancellationToken} which will be forwarded to the specified
     *   {@code TaskExecutor}. This argument cannot be {@code null}.
     * @param input the data which is to be converted. This argument will be
     *   passed to the wrapped {@link #getConverter() data converter object}.
     *   This argument can only be {@code null} if the data converter accepts
     *   {@code null} objects.
     * @return the {@code Future} representing the conversion routine. The
     *   result of the conversion can be retrieved by one of the {@code get}
     *   methods of the returned {@code Future}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} is {@code null}
     */
    public TaskFuture<ResultType> submit(
            CancellationToken cancelToken, InputType input) {

        return executor.submit(cancelToken,
                new FunctionWithArgument<>(input, converter), null);
    }

    /**
     * Returns the data converter object which will carry out the conversion of
     * the input data. This is the same object which was specified at
     * construction time.
     *
     * @return the data converter object which will carry out the conversion of
     *   the input data. This method never returns {@code null}.
     */
    public DataConverter<InputType, ResultType> getConverter() {
        return converter;
    }

    /**
     * Returns the {@code TaskExecutorService} on which the data conversion is
     * intended to be executed. This {@code TaskExecutorService} is the same as
     * the one specified at construction time.
     *
     * @return the {@code TaskExecutorService} on which the data conversion is
     *   intended to be executed. This method never returns {@code null}.
     */
    public TaskExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns the string representation of this {@code AsyncDataConverter} in
     * no particular format. The returned string will contain the string
     * representation of both the {@link #getConverter() data converter} and the
     * {@link #getExecutor() executor}.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Use converter: ");
        AsyncFormatHelper.appendIndented(converter, result);
        result.append("\nexecute on ");
        AsyncFormatHelper.appendIndented(executor, result);

        return result.toString();
    }

    private static class FunctionWithArgument<InputType, ResultType> implements
            CancelableFunction<ResultType> {

        private final InputType input;
        private final DataConverter<InputType, ResultType> converter;

        public FunctionWithArgument(
                InputType input,
                DataConverter<InputType, ResultType> converter) {

            this.input = input;
            this.converter = converter;
        }

        @Override
        public ResultType execute(CancellationToken cancelToken) {
            return converter.convertData(input);
        }
    }
}
