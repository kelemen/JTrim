package org.jtrim2.concurrent.async;

import java.util.Objects;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Contains useful helper methods for asynchronous data transferring using an
 * {@link AsyncDataLink}.
 * <P>
 * This class cannot be inherited or instantiated.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 *
 * @see AsyncLinks
 * @see AsyncQueries
 *
 * @author Kelemen Attila
 */
public final class AsyncHelper {
    static final int DEFAULT_CACHE_TIMEOUT = 1000; // ms
    static final int DEFAULT_CACHE_SIZE = 128;

    private static DataTransferException toTransferException(Throwable exception) {
        Objects.requireNonNull(exception, "exception");

        if (exception instanceof DataTransferException) {
            return (DataTransferException)exception;
        }
        else {
            return new DataTransferException(exception);
        }
    }

    /**
     * Creates a single {@link DataTransferException} from the specified
     * exceptions.
     * <P>
     * The exception will be created as follows:
     * <ul>
     *  <li>
     *   If there are no exceptions specified {@code null} is returned.
     *  </li>
     *  <li>
     *   If there is a single exception is specified: The specified exception is
     *   returned if it is a {@code DataTransferException}, if it is not then
     *   a new {@code DataTransferException} with the specified exception as its
     *   {@link Throwable#getCause() cause}.
     *  </li>
     *  <li>
     *   If there are more than one exception specified: A new
     *   {@code DataTransferException} is created and the specified exceptions
     *   will be added to it as {@link Throwable#addSuppressed(Throwable) suppressed}
     *   exceptions.
     *  </li>
     * </ul>
     *
     * @param exceptions the array of exceptions to be represented by the
     *   returned {@code DataTransferException} exception. This array cannot be
     *   {@code null} and cannot contain {@code null} elements.
     * @return the {@code DataTransferException} representing the specified
     *   exceptions or {@code null} if there was no exception specified
     *   (empty array was passed)
     *
     * @throws NullPointerException thrown if the passed array of exceptions is
     *   {@code null} or contains {@code null} elements
     */
    public static DataTransferException getTransferException(Throwable... exceptions) {
        DataTransferException result;

        switch (exceptions.length) {
            case 0:
                result = null;
                break;
            case 1:
                result = toTransferException(exceptions[0]);
                break;
            default:
                result = new DataTransferException();
                ExceptionHelper.checkNotNullElements(exceptions, "exceptions");
                for (Throwable exception: exceptions) {
                    result.addSuppressed(exception);
                }
                break;
        }

        return result;
    }

    // Listener builder methods

    static <DataType> boolean isSafeListener(AsyncDataListener<DataType> listener) {
        if (listener instanceof PossiblySafeListener) {
            return ((PossiblySafeListener)listener).isSafeListener();
        }
        else {
            return false;
        }
    }

    /**
     * Creates an {@code AsyncDataListener} which forwards data (with an
     * additional index) to a specified listener in a thread-safe manner. That
     * is, the {@link AsyncDataListener#onDataArrive(Object) onDataArrive} and
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
     * of the returned {@code AsyncDataListener} can be called concurrently from
     * multiple threads and these methods of the specified listener will still
     * not be called concurrently. Also data will be forwarded to the specified
     * listener only if there were no data forwarded with a greater or equal
     * {@link OrderedData#getIndex() index} and the {@code onDoneReceive} method
     * has not yet been called.
     * <P>
     * As an example see the following single threaded code:
     * <pre>
     * AsyncDataListener&lt;OrderedData&lt;String&gt;&gt; listener;
     * listener = makeSafeOrderedListener(...);
     *
     * listener.onDataArrive(new OrderedData&lt;&gt;(1, "data1"));
     * listener.onDataArrive(new OrderedData&lt;&gt;(3, "data3"));
     * listener.onDataArrive(new OrderedData&lt;&gt;(2, "data2"));
     * listener.onDoneReceive(AsyncReport.SUCCESS);
     * listener.onDataArrive(new OrderedData&lt;&gt;(4, "data4"));
     * listener.onDoneReceive(AsyncReport.CANCELED);
     * </pre>
     * The above code will forward {@code "data1"} and {@code "data3"} to the
     * wrapped listener (which should be written in the place of "...") only
     * and in this order. Also only the {@code AsyncReport.SUCCESS} will be
     * reported in the {@code onDoneReceive} method. That is, the
     * {@code "data3"} line and the two lines below the first
     * {@code onDoneReceive} call will be effectively ignored.
     *
     * @param <DataType> the type of the data to be forwarded to the specified
     *   listener
     * @param outputListener the {@code AsyncDataListener} to which the data
     *   reported to the returned listener will be eventually forwarded. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataListener} which forwards data (with an
     *   additional index) to a specified listener in a thread-safe manner. This
     *   method never returns {@code null}.
     *
     * @see #makeSafeListener(AsyncDataListener)
     */
    public static <DataType>
            AsyncDataListener<OrderedData<DataType>> makeSafeOrderedListener(
            AsyncDataListener<? super DataType> outputListener) {

        return new SafeDataListener<>(outputListener);
    }

    /**
     * Creates an {@code AsyncDataListener} which forwards data to a specified
     * listener in a thread-safe manner.
     * <P>
     * This method is similar to the
     * {@link #makeSafeOrderedListener(AsyncDataListener) makeSafeOrderedListener}
     * method, the only difference is that the listener returned by this method
     * implicitly assumes the order of the datas from the order of the method
     * calls.
     * <P>
     * More precisely, the
     * {@link AsyncDataListener#onDataArrive(Object) onDataArrive} and
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
     * of the returned {@code AsyncDataListener} can be called concurrently from
     * multiple threads and these methods of the specified listener will still
     * not be called concurrently. Also data will be forwarded to the specified
     * listener only if the {@code onDoneReceive} method has not yet been
     * called.
     * <P>
     * As an example see the following single threaded code:
     * <pre>
     * AsyncDataListener&lt;String&gt; listener;
     * listener = makeSafeOrderedListener(...);
     *
     * listener.onDataArrive("data1");
     * listener.onDataArrive("data2");
     * listener.onDoneReceive(AsyncReport.SUCCESS);
     * listener.onDataArrive("data3");
     * listener.onDoneReceive(AsyncReport.CANCELED);
     * </pre>
     * The above code will forward {@code "data1"} and {@code "data2"} to the
     * wrapped listener (which should be written in the place of "...") only
     * and in this order. Also only the {@code AsyncReport.SUCCESS} will be
     * reported in the {@code onDoneReceive} method. That is, the two lines
     * below the first {@code onDoneReceive} call will be effectively ignored.
     * <P>
     * Note that although invoking two {@code onDataArrive} methods concurrently
     * is safe regarding consistency, in practice this must be avoided. This
     * must be avoided because there is no telling in what order will these
     * datas be forwarded and the contract of {@code AsyncDataListener} requires
     * that a data forwarded be at least as accurate as the previous ones. This
     * implies that if the {@code onDataArrive} method is called concurrently
     * the datas forwarded to them must be equivalent. In this case it was a
     * waste of effort to forward all the data instead of just one of them.
     *
     * @param <DataType> the type of the data to be forwarded to the specified
     *   listener
     * @param outputListener the {@code AsyncDataListener} to which the data
     *   reported to the returned listener will be eventually forwarded. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataListener} which forwards data to a specified
     *   listener in a thread-safe manner. This method never returns
     *   {@code null}. This method may return the same listener passed in the
     *   argument if the specified listener already has the properties defined
     *   for the return value.
     *
     * @see #makeSafeOrderedListener(AsyncDataListener)
     */
    @SuppressWarnings("unchecked")
    public static <DataType>
            AsyncDataListener<DataType> makeSafeListener(
            AsyncDataListener<? super DataType> outputListener) {
        if (isSafeListener(outputListener)) {
            // This is a safe cast due to erasure. That is, DataType is only
            // used as an argument and if the passed argument implements
            // DataType then it will implement any of its subclass (obviously).
            return (AsyncDataListener<DataType>)outputListener;
        }

        return new DataOrdererListener<>(
                makeSafeOrderedListener(outputListener));
    }

    private AsyncHelper() {
        throw new AssertionError();
    }
}
