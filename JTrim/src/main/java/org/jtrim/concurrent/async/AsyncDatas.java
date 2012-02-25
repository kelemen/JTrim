package org.jtrim.concurrent.async;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains static factory and helper methods for {@link AsyncDataLink} and
 * {@link AsyncDataQuery} implementations.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 * correctness.
 *
 * @author Kelemen Attila
 */
public final class AsyncDatas {
    private static final int DEFAULT_CACHE_TIMEOUT = 1000; // ms
    private static final int DEFAULT_CACHE_SIZE = 128;

    private AsyncDatas() {
        throw new AssertionError();
    }

    // Exceptions

    private static DataTransferException toTransferException(Throwable exception) {
        ExceptionHelper.checkNotNullArgument(exception, "exception");

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
     * @return the {@code AsyncDataListener} which forwards data (with an
     *   additional index) to a specified listener in a thread-safe manner. This
     *   method never returns {@code null}.
     *
     * @see #makeSafeOrderedListener(AsyncDataListener)
     */
    public static <DataType>
            AsyncDataListener<DataType> makeSafeListener(
            AsyncDataListener<? super DataType> outputListener) {

        return new DataOrdererListener<>(
                makeSafeOrderedListener(outputListener));
    }

    // Link builder methods

    /**
     * Creates an {@link AsyncDataLink} which will provide the same data
     * as the specified {@code AsyncDataLink} but applies a conversion on the
     * provided data. That is, it will call
     * {@link DataConverter#convertData(Object) converter.convertData} on every
     * data object provided by the specified {@code AsyncDataLink}.
     * <P>
     * For example, if the specified {@code AsyncDataLink} provides a string:
     * {@code "dummy"}, and the converter returns the length of the passed
     * string as an {@code Integer}, then the returned {@code AsyncDataLink}
     * will provide the {@code Integer} value 5.
     *
     * @param <OldType> the type of the data objects provided by the specified
     *   {@code AsyncDataLink}, which is also the type of the input of the
     *   conversion
     * @param <NewType> the type of the data objects provided by the returned
     *   {@code AsyncDataLink}, which is also the type of the output of the
     *   conversion
     * @param input the {@code AsyncDataLink} of which provided data is to be
     *   converted. This argument cannot be {@code null}.
     * @param converter the {@code DataConverter} defining the conversion of
     *   the results of {@code input}. This argument cannot be {@code null}.
     * @return the {@link AsyncDataLink} which will provide the same data
     *   as the specified {@code AsyncDataLink} but applies a conversion on the
     *   provided data. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #convertResult(AsyncDataLink, AsyncDataQuery)
     */
    public static <OldType, NewType> AsyncDataLink<NewType> convertResult(
            AsyncDataLink<? extends OldType> input,
            DataConverter<? super OldType, ? extends NewType> converter) {

        return new AsyncDataLinkConverter<>(input, converter);
    }

    /**
     * Creates an {@link AsyncDataLink} which will provide the same data
     * as the specified {@code AsyncDataLink} but applies a conversion on the
     * provided data defined by an {@link AsyncDataQuery AsyncDataQuery}. That
     * is, it will pass every data received by the input {@code AsyncDataLink}
     * and pass it as an input to the {@code AsyncDataQuery} and will return the
     * data provided by the {@link AsyncDataLink} returned by the
     * {@code AsyncDataQuery}.
     * <P>
     * This method is best used when the conversion requires to much time to be
     * executed directly in a listener. For example, if the specified
     * {@code AsyncDataLink} provides a {@link Path Path} from an external
     * source to a file and the converter loads the image from a given path,
     * these can be combined to create an {@code AsyncDataLink} which loads
     * an image file from this external source.
     * <P>
     * Note that in the above example the input {@code AsyncDataLink} is
     * unlikely to provide more and more accurate data. However in general, it
     * is possible that both the input {@code AsyncDataLink} and the
     * {@code AsyncDataLink} instances returned by the converter
     * {@code AsyncDataQuery} can return more data objects with increasing
     * accuracy. The returned {@code AsyncDataLink} assumes that the conversion
     * cannot make a data (provided by the input
     * {@code AsyncDataLink}) more accurate than the subsequent inputs
     * {@code AsyncDataLink} (after the first conversion).
     * <P>
     * As a general and simple example assume that the input
     * {@code AsyncDataLink} returns the string {@code "A"} and {@code "B"},
     * so {@code "A"} is more accurate than {@code "B"}. Also assume that
     * the converter query for every inputs provides two strings: The first
     * provided string is the input string with {@code "C"} appended and second
     * is with {@code "D"} appended. Therefore the returned
     * {@code AsyncDataLink} will provide the following strings:
     * {@code "AC", "AD", "BC", "BD"} and the accuracy is the same as the order
     * of these strings. Note that the returned query can only provide the
     * strings in this order but may omit any subset of them except for the last
     * one ({@code "BD"} which will always be provided).
     *
     * @param <OldType> the type of the data objects provided by the specified
     *   {@code AsyncDataLink}, which is also the type of the input of the
     *   conversion
     * @param <NewType> the type of the data objects provided by the returned
     *   {@code AsyncDataLink}, which is also the type of the output of the
     *   conversion
     * @param input the {@code AsyncDataLink} of which provided data is to be
     *   converted. This argument cannot be {@code null}.
     * @param converter the {@code AsyncDataQuery} defining the conversion of
     *   the results of {@code input}. This argument cannot be {@code null}.
     * @return the {@link AsyncDataLink} which will provide the same data
     *   as the specified {@code AsyncDataLink} but applies a conversion on the
     *   provided data. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #convertResult(AsyncDataLink, AsyncDataQuery)
     */
    public static <OldType, NewType> AsyncDataLink<NewType> convertResult(
            AsyncDataLink<? extends OldType> input,
            AsyncDataQuery<? super OldType, ? extends NewType> converter) {

        return new LinkedAsyncDataLink<>(input, converter);
    }

    /**
     * Returns an {@code AsyncDataLink} which retrieves the
     * {@link RefCachedData#getData() data part} from the {@link RefCachedData}
     * results of the given {@code AsyncDataLink}.
     * <P>
     * The returned {@code AsyncDataLink} will return the same number of results
     * as the specified {@code AsyncDataLink} without regard to the
     * {@link AsyncDataListener#requireData() needs} of the listener. That is
     * the returned {@code AsyncDataLink} will simply return the data part for
     * each received {@link RefCachedData} in the same order.
     *
     * @param <DataType> the type of the data to be provided by the returned
     *   {@code AsyncDataLink}
     * @param link the {@code AsyncDataLink} from which the
     *   {@link RefCachedData#getData() data part} is to be extracted. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} providing the
     *   {@link RefCachedData#getData() data part} of the specified
     *   {@code AsyncDataLink}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataLink} is {@code null}
     *
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<DataType> extractCachedResult(
            AsyncDataLink<RefCachedData<DataType>> link) {

        return convertResult(link, new CachedDataExtractor<DataType>());
    }

    /**
     * Returns an {@code AsyncDataLink} which retrieves the
     * {@link DataWithUid#getData() data part} from the {@link DataWithUid}
     * results of the given {@code AsyncDataLink}.
     * <P>
     * The returned {@code AsyncDataLink} will return the same number of results
     * as the specified {@code AsyncDataLink} without regard to the
     * {@link AsyncDataListener#requireData() needs} of the listener. That is
     * the returned {@code AsyncDataLink} will simply return the data part for
     * each received {@link DataWithUid} in the same order.
     *
     * @param <DataType> the type of the data to be provided by the returned
     *   {@code AsyncDataLink}
     * @param link the {@code AsyncDataLink} from which the
     *   {@link DataWithUid#getData() data part} is to be extracted. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} providing the
     *   {@link DataWithUid#getData() data part} of the specified
     *   {@code AsyncDataLink}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataLink} is {@code null}
     *
     * @see #markResultWithUid(AsyncDataLink)
     * @see #markResultsWithUid(AsyncDataQuery)
     * @see #removeUidFromResults(AsyncDataQuery)
     */
    public static <DataType> AsyncDataLink<DataType> removeUidFromResult(
            AsyncDataLink<DataWithUid<DataType>> link) {

        return convertResult(link, new DataIDRemover<DataType>());
    }

    /**
     * Returns an {@code AsyncDataLink} which returns the same results as the
     * specified {@code AsyncDataLink} but creates a {@link DataWithUid}
     * object with a new unique {@link DataWithUid#getID() ID} from the
     * results of the specified {@code AsyncDataLink}.
     * <P>
     * The returned {@code AsyncDataLink} will return the same number of results
     * as the specified {@code AsyncDataLink} without regard to the
     * {@link AsyncDataListener#requireData() needs} of the listener. That is
     * the returned {@code AsyncDataLink} will simply return a new
     * {@code DataWithUid} with the same data and a unique ID.
     * <P>
     * The {@link DataWithUid#getID() ID} of the provided datas will be unique,
     * so that no other object equals to them.
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink}
     * @param link the {@code AsyncDataLink} from which the
     *   {@link DataWithUid#getData() data part} is to be derived for the
     *   results of the returned {@code AsyncDataLink}. This argument cannot be
     *   {@code null}.
     * @return the {@code AsyncDataLink} providing the {@link DataWithUid}
     *   objects with the data parts provided by the specified
     *   {@code AsyncDataLink}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataLink} is {@code null}
     *
     * @see #markResultsWithUid(AsyncDataQuery)
     * @see #removeUidFromResult(AsyncDataLink)
     * @see #removeUidFromResults(AsyncDataQuery)
     */
    public static <DataType> AsyncDataLink<DataWithUid<DataType>> markResultWithUid(
            AsyncDataLink<? extends DataType> link) {

        return convertResult(link, new MarkWithIDConverter<DataType>());
    }

    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator) {

        return cacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return extractCachedResult(
                refCacheResult(wrappedDataLink, refType, refCreator,
                    dataCancelTimeout, timeUnit));
    }

    public static <DataType> AsyncDataLink<RefCachedData<DataType>> refCacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator
            ) {

        return refCacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <DataType> AsyncDataLink<RefCachedData<DataType>> refCacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return new RefCachedDataLink<>(
                wrappedDataLink,
                refType, refCreator,
                dataCancelTimeout, timeUnit);
    }

    public static <InputType, ResultType> AsyncDataLink<ResultType> convertGradually(
            InputType input,
            List<? extends AsyncDataConverter<InputType, ResultType>> transformers) {

        return new ImproverTasksLink<>(input, transformers);
    }

    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        return createStateReporterLink(null, wrappedLink, reporter,
                period, periodUnit);
    }

    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            UpdateTaskExecutor reportExecutor,
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        return new PeriodicStateReporterLink<>(reportExecutor, wrappedLink,
                reporter, period, periodUnit);
    }

    public static <DataType> AsyncDataLink<DataType> createPreparedLink(
            DataType data, AsyncDataState state) {

        return new PreparedDataLink<>(data, state);
    }

    // Query builder methods

    public static <QueryArgType, DataType>
            AsyncDataQuery<CachedDataRequest<QueryArgType>, DataType> cacheResults(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {

        return new AsyncCachedLinkQuery<>(wrappedQuery);
    }

    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery) {

        return cacheLinks(wrappedQuery, DEFAULT_CACHE_SIZE);
    }

    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {

        return new CachedAsyncDataQuery<>(wrappedQuery, maxCacheSize);
    }

    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator) {

        return cacheByID(wrappedQuery, refType, refCreator, DEFAULT_CACHE_SIZE);
    }

    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator,
            int maxCacheSize) {

        return new CachedByIDAsyncDataQuery<>(wrappedQuery,
                refType, refCreator, maxCacheSize);
    }

    public static <OldDataType, QueryArgType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        return new AsyncDataQueryConverter<>(wrappedQuery, converter);
    }

    public static <QueryArgType, SecArgType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends SecArgType> wrappedQuery,
            AsyncDataQuery<? super SecArgType, ? extends NewDataType> converter) {

        return new LinkedAsyncDataQuery<>(wrappedQuery, converter);
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> extractCachedResults(
            AsyncDataQuery<? super QueryArgType, RefCachedData<DataType>> query) {

        return convertResults(query, new CachedDataExtractor<DataType>());
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> removeUidFromResults(
            AsyncDataQuery<? super QueryArgType, DataWithUid<DataType>> query) {

        return convertResults(query, new DataIDRemover<DataType>());
    }

    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataWithUid<DataType>> markResultsWithUid(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> query) {

        return convertResults(query, new MarkWithIDConverter<DataType>());
    }
}
