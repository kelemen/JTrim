package org.jtrim.concurrent.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a transformation of objects to the same type and the
 * {@link ExecutorService ExecutorService} which should be used to execute the
 * transformation process. This class is a less general version of
 * {@link AsyncDataConverter} and can be used when a series of conversion needed
 * to be done but keeping the type of the objects to be converted.
 * <P>
 * This class effectively just holds a {@link DataTransformer} and the
 * {@code ExecutorService}.
 * <P>
 * This class was created to use a more fitting name when converting to the
 * same type and also to reduce the number of generic argument.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Aside from the {@link #submit(Object) submit} method, methods of this class
 * are <I>synchronization transparent</I>. This {@code submit} method however
 * needs to submit a task to the {@code ExecutorService} and as such is not
 * <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data which is to be converted and also
 *   the type of the resulting data after conversion
 *
 * @see AsyncDataConverter
 * @author Kelemen Attila
 */
public final class AsyncDataTransformer<DataType> {
    private final DataTransformer<DataType> transformer;
    private final ExecutorService executor;

    /**
     * Creates a new {@code AsyncDataTransformer} with the given transformation
     * routine and {@code ExecutorService}.
     *
     * @param transformer the {@code DataTransformer} which defines the
     *   transformation of input datas. This argument cannot be {@code null}.
     * @param executor the {@code ExecutorService} which is to be used execute
     *   the {@link DataTransformer#transform(Object) transformation} of the
     *   input data. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public AsyncDataTransformer(
            DataTransformer<DataType> transformer,
            ExecutorService executor) {

        ExceptionHelper.checkNotNullArgument(transformer, "transformer");
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.transformer = transformer;
        this.executor = executor;
    }

    /**
     * Submits a task to the wrapped {@link #getExecutor() executor} which will
     * carry out the {@link #getTransformer() data transformation routine}.
     *
     * @param input the data which is to be transformed. This argument will be
     *   passed to the wrapped {@link #getTransformer() data transformer}.
     *   This argument can only be {@code null} if the data transformer accepts
     *   {@code null} objects.
     * @return the {@code Future} representing the transformation routine. The
     *   result of the transformation can be retrieved by one of the {@code get}
     *   methods of the returned {@code Future}. This method never returns
     *   {@code null}.
     */
    public Future<DataType> submit(DataType input) {
        return executor.submit(new CallableWithArgument<>(input, transformer));
    }

    /**
     * Returns the data transformer object which will carry out the conversion
     * of the input data. This is the same object which was specified at
     * construction time.
     *
     * @return the data transformer object which will carry out the conversion
     *   of the input data. This method never returns {@code null}.
     */
    public DataTransformer<DataType> getTransformer() {
        return transformer;
    }

    /**
     * Returns the {@code ExecutorService} on which the data transformation is
     * intended to be executed. This {@code ExecutorService} is the same as the
     * one specified at construction time.
     *
     * @return the {@code ExecutorService} on which the data transformation is
     *   intended to be executed. This method never returns {@code null}.
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns the string representation of this {@code AsyncDataTransformer} in
     * no particular format. The returned string will contain the string
     * representation of both the {@link #getTransformer() data transformer} and
     * the {@link #getExecutor() executor}.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
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