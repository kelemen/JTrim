package org.jtrim.concurrent.async;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
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
 *
 * @author Kelemen Attila
 */
public final class AsyncDatas {
    // TODO: Refactor the methods to separate classes:
    //       AsyncLinks, AsyncQueries, AsyncHelper

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
     * Creates an {@link AsyncDataLink} which will provide the same data as
     * the specified {@link AsyncDataLink} but has a chance to intercept every
     * the data being retrieved. It is also possible for the interceptor to
     * filter out some of the data objects being retrieved.
     * <P>
     * The methods of the {@link DataInterceptor interceptor} object are called
     * in the listener of the data retrieval request, so they must be quick,
     * non-blocking methods.
     *
     * @param <DataType> the type of the data being provided by the returned
     *   {@code AsyncDataLink} (and also the specified one)
     * @param wrappedLink the {@code AsyncDataLink} which actually provides the
     *   data and whose data objects are being intercepted. This argument cannot
     *   be {@code null}.
     * @param interceptor the {@code DataInterceptor} to be notified before data
     *   was received by the listener listening for the requested data. This
     *   argument cannot be {@code null}.
     * @return the {@link AsyncDataLink} providing the same data as the
     *   specified {@link AsyncDataLink} but intercepts every returned data and
     *   the data notification of the data retrieval completion. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <DataType> AsyncDataLink<DataType> interceptData(
            AsyncDataLink<? extends DataType> wrappedLink,
            DataInterceptor<? super DataType> interceptor) {
        return new DataInterceptorLink<>(wrappedLink, interceptor);
    }

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
     * <P>
     * When an {@link LinkedDataControl} object is passed to
     * {@link AsyncDataController#controlData(Object) control} the data
     * retrieval process, the
     * {@link LinkedDataControl#getMainControlData() main control data} is sent
     * to the specified {@code AsyncDataLink} and the
     * {@link LinkedDataControl#getSecondaryControlData() secondary control data}
     * is sent to the {@code AsyncDataLink} created by the specified query
     * defining the conversion.
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
     * @see LinkedDataControl
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

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results. The actual
     * caching mechanism is defined by the specified {@link ObjectCache} and
     * therefore the cached data is stored in a {@link VolatileReference}.
     *
     * <h5>Requesting data</h5>
     * The first time the data is requested from the returned
     * {@code AsyncDataLink}, it will fallback to requesting the data from
     * the specified {@code AsyncDataLink} and provide exactly the same data as
     * provided by this underlying {@code AsyncDataLink}. Once the final data is
     * available, it will be cached using the given cache. Subsequent data
     * requests will simply return this final cached data.
     * <P>
     * In case the cached data expires (no longer available in the cache), the
     * data will be returned as if it has never been requested. Note that the
     * returned {@code AsyncDataLink} implementation will never query the data
     * from the specified {@code AsyncDataLink} if not required. That is, if it
     * is currently in the process of requesting the data, it will not start
     * a concurrent request but will return the data from the ongoing request.
     *
     * <h5>Canceling requests</h5>
     * Attempting to cancel a data request will not cancel the requesting of the
     * data from the specified {@code AsyncDataLink} but will only do so if
     * there are no active requests (only canceled or completed) and even in
     * this case only after one second has elapsed since the last active
     * request.
     *
     * <h5>Controlling requests</h5>
     * In case the data is currently being requested from the specified
     * {@code AsyncDataLink}, the control objects will be forwarded to this
     * underlying request. Note however, that {@link AsyncDataController}
     * objects created by the returned {@code AsyncDataLink} may forward the
     * control requests to the same underlying request.
     *
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param wrappedDataLink the {@code AsyncDataLink} from which the data is
     *   to be requested when the data is not currently cached. This argument
     *   cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @return the {@code AsyncDataLink} which will provide the same data as the
     *   specified {@code AsyncDataLink} but will cache its result. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} except for the {@code ObjectCache} which is allowed to be
     *   {@code null}
     *
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator) {

        return cacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results. The actual
     * caching mechanism is defined by the specified {@link ObjectCache} and
     * therefore the cached data is stored in a {@link VolatileReference}.
     *
     * <h5>Requesting data</h5>
     * The first time the data is requested from the returned
     * {@code AsyncDataLink}, it will fallback to requesting the data from
     * the specified {@code AsyncDataLink} and provide exactly the same data as
     * provided by this underlying {@code AsyncDataLink}. Once the final data is
     * available, it will be cached using the given cache. Subsequent data
     * requests will simply return this final cached data.
     * <P>
     * In case the cached data expires (no longer available in the cache), the
     * data will be returned as if it has never been requested. Note that the
     * returned {@code AsyncDataLink} implementation will never query the data
     * from the specified {@code AsyncDataLink} if not required. That is, if it
     * is currently in the process of requesting the data, it will not start
     * a concurrent request but will return the data from the ongoing request.
     *
     * <h5>Canceling requests</h5>
     * Attempting to cancel a data request will not cancel the requesting of the
     * data from the specified {@code AsyncDataLink} but will only do so if
     * there are no active requests (only canceled or completed) and even in
     * this case only after the given timeout has elapsed since the last active
     * request.
     *
     * <h5>Controlling requests</h5>
     * In case the data is currently being requested from the specified
     * {@code AsyncDataLink}, the control objects will be forwarded to this
     * underlying request. Note however, that {@link AsyncDataController}
     * objects created by the returned {@code AsyncDataLink} may forward the
     * control requests to the same underlying request.
     *
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param wrappedDataLink the {@code AsyncDataLink} from which the data is
     *   to be requested when the data is not currently cached. This argument
     *   cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @param dataCancelTimeout the time in the given unit to wait before
     *   actually canceling abandoned requests. Before this time elapses, it is
     *   possible to start requesting the data and continuing where the request
     *   was left off. This argument must be greater than or equal to zero.
     *   In case this argument is zero, the data requesting will be canceled as
     *   soon as the data is detected to be not required.
     * @param timeUnit the time unit of the {@code dataCancelTimeout} argument.
     *   This argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} which will provide the same data as the
     *   specified {@code AsyncDataLink} but will cache its result. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} except for the {@code ObjectCache} which is allowed to be
     *   {@code null}
     *
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<DataType> cacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator,
            long dataCancelTimeout, TimeUnit timeUnit
            ) {

        return extractCachedResult(
                refCacheResult(wrappedDataLink, refType, refCreator,
                    dataCancelTimeout, timeUnit));
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results and also
     * return the cached reference. The actual caching mechanism is defined by
     * the specified {@link ObjectCache} and therefore the cached data is stored
     * in a {@link VolatileReference}.
     *
     * <h5>Requesting data</h5>
     * The first time the data is requested from the returned
     * {@code AsyncDataLink}, it will fallback to requesting the data from
     * the specified {@code AsyncDataLink} and provide exactly the same data as
     * provided by this underlying {@code AsyncDataLink}. Once the final data is
     * available, it will be cached using the given cache. Subsequent data
     * requests will simply return this final cached data.
     * <P>
     * In case the cached data expires (no longer available in the cache), the
     * data will be returned as if it has never been requested. Note that the
     * returned {@code AsyncDataLink} implementation will never query the data
     * from the specified {@code AsyncDataLink} if not required. That is, if it
     * is currently in the process of requesting the data, it will not start
     * a concurrent request but will return the data from the ongoing request.
     *
     * <h5>Canceling requests</h5>
     * Attempting to cancel a data request will not cancel the requesting of the
     * data from the specified {@code AsyncDataLink} but will only do so if
     * there are no active requests (only canceled or completed) and even in
     * this case only after one second has elapsed since the last active
     * request.
     *
     * <h5>Controlling requests</h5>
     * In case the data is currently being requested from the specified
     * {@code AsyncDataLink}, the control objects will be forwarded to this
     * underlying request. Note however, that {@link AsyncDataController}
     * objects created by the returned {@code AsyncDataLink} may forward the
     * control requests to the same underlying request.
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param wrappedDataLink the {@code AsyncDataLink} from which the data is
     *   to be requested when the data is not currently cached. This argument
     *   cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @return the {@code AsyncDataLink} which will provide the same data as the
     *   specified {@code AsyncDataLink} but will cache its result. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} except for the {@code ObjectCache} which is allowed to be
     *   {@code null}
     *
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<RefCachedData<DataType>> refCacheResult(
            AsyncDataLink<? extends DataType> wrappedDataLink,
            ReferenceType refType, ObjectCache refCreator
            ) {

        return refCacheResult(
                wrappedDataLink,
                refType, refCreator,
                DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results and also
     * return the cached reference. The actual caching mechanism is defined by
     * the specified {@link ObjectCache} and therefore the cached data is stored
     * in a {@link VolatileReference}.
     *
     * <h5>Requesting data</h5>
     * The first time the data is requested from the returned
     * {@code AsyncDataLink}, it will fallback to requesting the data from
     * the specified {@code AsyncDataLink} and provide exactly the same data as
     * provided by this underlying {@code AsyncDataLink}. Once the final data is
     * available, it will be cached using the given cache. Subsequent data
     * requests will simply return this final cached data.
     * <P>
     * In case the cached data expires (no longer available in the cache), the
     * data will be returned as if it has never been requested. Note that the
     * returned {@code AsyncDataLink} implementation will never query the data
     * from the specified {@code AsyncDataLink} if not required. That is, if it
     * is currently in the process of requesting the data, it will not start
     * a concurrent request but will return the data from the ongoing request.
     *
     * <h5>Canceling requests</h5>
     * Attempting to cancel a data request will not cancel the requesting of the
     * data from the specified {@code AsyncDataLink} but will only do so if
     * there are no active requests (only canceled or completed) and even in
     * this case only after the given timeout has elapsed since the last active
     * request.
     *
     * <h5>Controlling requests</h5>
     * In case the data is currently being requested from the specified
     * {@code AsyncDataLink}, the control objects will be forwarded to this
     * underlying request. Note however, that {@link AsyncDataController}
     * objects created by the returned {@code AsyncDataLink} may forward the
     * control requests to the same underlying request.
     *
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param wrappedDataLink the {@code AsyncDataLink} from which the data is
     *   to be requested when the data is not currently cached. This argument
     *   cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @param dataCancelTimeout the time in the given unit to wait before
     *   actually canceling abandoned requests. Before this time elapses, it is
     *   possible to start requesting the data and continuing where the request
     *   was left off. This argument must be greater than or equal to zero.
     *   In case this argument is zero, the data requesting will be canceled as
     *   soon as the data is detected to be not required.
     * @param timeUnit the time unit of the {@code dataCancelTimeout} argument.
     *   This argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} which will provide the same data as the
     *   specified {@code AsyncDataLink} but will cache its result. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} except for the {@code ObjectCache} which is allowed to be
     *   {@code null}
     *
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     */
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

    /**
     * Creates a new {@code AsyncDataLink} which will return the given input
     * data transformed by the specified {@code AsyncDataConverter}, assuming
     * that subsequent converters in the list provide more and more accurate
     * conversion. That is, the returned {@code AsyncDataLink} will first return
     * the input transformed by the first {@code AsyncDataConverter}, the
     * transformed by the second {@code AsyncDataConverter} and so on.
     * <P>
     * Some of the conversions may be omitted on the discretion of the returned
     * {@code AsyncDataLink} if the {@link AsyncDataListener} of a request does
     * not currently {@link AsyncDataListener#requireData() requires} data. Note
     * however, that the final transformation is always applied.
     *
     * @param <InputType> the type of the input to be transformed
     * @param <ResultType> the type of the result of the transformation
     * @param input the input to be transformed and returned. This argument can
     *   only be {@code null} if the {@code AsyncDataConverter} implementations
     *   accept {@code null} inputs.
     * @param transformers the list of {@link AsyncDataConverter} instances to
     *   transform the input data. Every element of the list defines a more
     *   accurate conversion than previous elements of the list. So every
     *   element of this list defines the same conversion except with different
     *   accuracy. This argument cannot be {@code null}, cannot contain
     *   {@code null} elements and also must contain at least a single element.
     * @return the {@code AsyncDataLink} which will transform the given input
     *   using the specified conversions. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the convert list or any of the
     *   converters is {@code null}
     * @throws IllegalArgumentException thrown if there are no transformations
     *   specified
     */
    public static <InputType, ResultType> AsyncDataLink<ResultType> convertGradually(
            InputType input,
            List<? extends AsyncDataConverter<InputType, ResultType>> transformers) {

        return new ImproverTasksLink<>(input, transformers);
    }

    /**
     * Creates a new {@code AsyncDataLink} which will provide the exact same
     * data as the specified {@code AsyncDataLink} but will periodically report
     * the state of the data retrieving process.
     * <P>
     * The state will be reported through the provided
     * {@link AsyncStateReporter#reportState(AsyncDataLink, AsyncDataListener, AsyncDataController)}
     * method. This method will be invoked on a separate thread which may be
     * shared by other state reports. That is, multiple {@code AsyncDataLink}
     * instances created by this method may share the same thread to handle
     * state reports. Therefore it is important for the
     * {@code AsyncStateReporter} instances not to depend on each other.
     * <P>
     * The reporting of the state of progress will end once the data providing
     * is done (reporting is done separately for every data providing process).
     * Note however, that it does not mean that once the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive}
     * notification was received no more state report will be done but state
     * reporting will end eventually after the {@code onDoneReceive}
     * notification.
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param wrappedLink the {@code AsyncDataLink} which is to provide the
     *   data for the returned {@code AsyncDataLink} and whose state of progress
     *   is to be reported. This argument cannot be {@code null}.
     * @param reporter the {@code AsyncStateReporter} to which the state of
     *   data providing progress will be reported periodically. This argument
     *   cannot be {@code null}.
     * @param period the period of time in the specified time unit in which
     *   the state of data providing progress is to be reported. This argument
     *   must be greater than or equal to zero.
     * @param periodUnit the time unit of the {@code period} argument. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} which will provide the exact same data
     *   as the specified {@code AsyncDataLink} but will periodically report the
     *   state of the data retrieving process. This method never returns
     *   {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified time period is
     *   lesser than zero
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        return new PeriodicStateReporterLink<>(null, wrappedLink,
                reporter, period, periodUnit);
    }

    /**
     * Creates a new {@code AsyncDataLink} which will provide the exact same
     * data as the specified {@code AsyncDataLink} but will periodically report
     * the state of the data retrieving process.
     * <P>
     * This method works exactly the same way as its cousin
     * {@link #createStateReporterLink(AsyncDataLink, AsyncStateReporter, long, TimeUnit) createStateReporterLink}
     * method except that it will forward state of progress notifications to
     * the specified {@link UpdateTaskExecutor UpdateTaskExecutor} instead of
     * directly invoking the
     * {@link AsyncStateReporter#reportState(AsyncDataLink, AsyncDataListener, AsyncDataController)}
     * method.
     * <P>
     * The state will be reported through the provided
     * {@link AsyncStateReporter#reportState(AsyncDataLink, AsyncDataListener, AsyncDataController)}
     * method. This method will be invoked in the context of the specified
     * {@code UpdateTaskExecutor}.
     * <P>
     * The reporting of the state of progress will end once the data providing
     * is done (reporting is done separately for every data providing process).
     * Note however, that it does not mean that once the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive}
     * notification was received no more state report will be done but state
     * reporting will end eventually after the {@code onDoneReceive}
     * notification.
     *
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataLink} which is the same type as the returned
     *   {@code AsyncDataLink}
     * @param reportExecutor the {@code UpdateTaskExecutor} to use to report
     *   the state of data loading progress. This argument cannot be
     *   {@code null}.
     * @param wrappedLink the {@code AsyncDataLink} which is to provide the
     *   data for the returned {@code AsyncDataLink} and whose state of progress
     *   is to be reported. This argument cannot be {@code null}.
     * @param reporter the {@code AsyncStateReporter} to which the state of
     *   data providing progress will be reported periodically. This argument
     *   cannot be {@code null}.
     * @param period the period of time in the specified time unit in which
     *   the state of data providing progress is to be reported. This argument
     *   must be greater than or equal to zero.
     * @param periodUnit the time unit of the {@code period} argument. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} which will provide the exact same data
     *   as the specified {@code AsyncDataLink} but will periodically report the
     *   state of the data retrieving process. This method never returns
     *   {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified time period is
     *   lesser than zero
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit)
     */
    public static <DataType> AsyncDataLink<DataType> createStateReporterLink(
            UpdateTaskExecutor reportExecutor,
            AsyncDataLink<DataType> wrappedLink,
            AsyncStateReporter<DataType> reporter,
            long period, TimeUnit periodUnit) {

        ExceptionHelper.checkNotNullArgument(reportExecutor, "reportExecutor");

        return new PeriodicStateReporterLink<>(reportExecutor, wrappedLink,
                reporter, period, periodUnit);
    }

    /**
     * Creates and returns an {@code AsyncDataLink} which will provide the given
     * data.
     * <P>
     * This method acts as a gateway between statically available data and
     * {@code AsyncDataLink}. The returned {@code AsyncDataLink} will always
     * provide the specified data immediately in the
     * {@link AsyncDataLink#getData(AsyncDataListener)} method and not on a
     * separate thread. Therefore once the {@code getData} method returns it
     * is guaranteed that the data providing has completed (i.e.: the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} has
     * been called).
     *
     * @param <DataType> the type of the data to be provided
     * @param data the data to be provided by the returned
     *   {@code AsyncDataLink}. That is, exactly this object (and only this),
     *   will be provided. This argument can be {@code null} but in this case
     *   the returned {@code AsyncDataLink} will provided {@code null} as a
     *   data.
     * @param state the state of progress to be returned by the
     *   returned {@code AsyncDataLink} when data is requested. Since after
     *   the data has been requested, the providing is immediately completed
     *   this state must always report the
     *   {@link AsyncDataState#getProgress() current progress} as {@code 1.0}.
     *   This argument can be {@code null} but in this case the returned state
     *   will always be {@code null} which is not recommended.
     * @return {@code AsyncDataLink} which will provide the specified data.
     *   This method never returns {@code null}.
     */
    public static <DataType> AsyncDataLink<DataType> createPreparedLink(
            DataType data, AsyncDataState state) {

        return new PreparedDataLink<>(data, state);
    }

    // Query builder methods

    /**
     * Creates and returns an {@code AsyncDataQuery} which creates
     * {@code AsyncDataLink} instances caching their results. The returned
     * {@code AsyncDataQuery} is based on a user specified one.
     * <P>
     * Note that the {@code AsyncDataLink} instances themselves will not be
     * cached. That is, requesting an {@code AsyncDataLink} from the returned
     * {@code AsyncDataQuery} will return a new, independent
     * {@code AsyncDataLink} instance. Only these {@code AsyncDataLink}
     * instances will cache their results. The caching mechanism done by these
     * {@code AsyncDataLink} instances is the same as those created by the
     * {@link #cacheResult(AsyncDataLink, ReferenceType,ObjectCache)} method.
     * <P>
     * The {@link ObjectCache ObjectCache} and other parameters defining the
     * exact caching mechanism must be specified when querying new
     * {@code AsyncDataLink} from the returned {@code AsyncDataQuery}.
     * <P>
     * As an example: Assume that the specified {@code AsyncDataQuery} loads a
     * file by its filename as a byte array. The returned {@code AsyncDataQuery}
     * will then load the files as a byte array as well, however individual
     * {@code AsyncDataLink} instances will cache their results and will not
     * reread the file. However whenever a new {@code AsyncDataLink} is returned
     * by the returned {@code AsyncDataQuery} it will still need to reread the
     * file at the first attempt of loading regardless that a file with the same
     * name has been requested from the returned {@code AsyncDataQuery}.
     *
     * @param <QueryArgType> the type of the input of the specified
     *   {@code AsyncDataQuery}
     * @param <DataType> the type of the data returned by the specified
     *   (and the returned)  {@code AsyncDataQuery}
     * @param wrappedQuery the {@code AsyncDataQuery} whose
     *   {@code AsyncDataLink} instances are to cache their results. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataQuery} which will provide the same data as
     *   the specified {@code AsyncDataQuery} but {@code AsyncDataLink}
     *   instances created by it will cache their results. Notice that this
     *   returned query requires additional inputs to specify how data is to
     *   be cached. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataQuery} is {@code null}
     *
     * @see #cacheByID(AsyncDataQuery, ReferenceType, ObjectCache, int)
     * @see #cacheLinks(AsyncDataQuery, int)
     * @see #cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<CachedDataRequest<QueryArgType>, DataType> cacheResults(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {

        return new AsyncCachedLinkQuery<>(wrappedQuery);
    }

    /**
     * Creates and returns an {@code AsyncDataQuery} which caches
     * {@code AsyncDataLink} instances based on the input of the query. That is,
     * the returned {@code AsyncDataQuery} may return the same
     * {@code AsyncDataLink} for the same input (based on {@code equals}). The
     * data provided by the {@code AsyncDataLink} instances are not cached by
     * the returned query. Those can be cached by using the
     * {@link #cacheResults(AsyncDataQuery) cacheResults} method.
     * <P>
     * The {@code AsyncDataQuery} created by this method caches only at most
     * 128 {@code AsyncDataLink} instances and when this limit is exceeded, the
     * one used least recently is discarded from the cache. Also a lease time
     * can be specified when requesting an {@code AsyncDataLink} when it must be
     * removed from the cache and the {@code AsyncDataLink} needs to be
     * recreated if requested again.
     * <P>
     * Note that most often you want to cache both the {@code AsyncDataLink} and
     * the data provided by them. Assume that there is a query which loads the
     * content of a file as a byte array by file name:
     * <code><pre>
     * AsyncDataQuery&lt;Path, byte[]&gt; baseQuery = ...;
     * AsyncDataQuery&lt;CachedLinkRequest&lt;CachedDataRequest&lt;Path&gt;&gt;, byte[]&gt; cachedQuery;
     * cachedQuery = AsyncDatas.cacheLinks(AsyncDatas.cacheResults(baseQuery));
     * </pre></code>
     * In the above example code {@code cachedQuery} will cache both the
     * {@code AsyncDataLink} instances and the data provided by them. Note that
     * order of {@code cacheLinks} and {@code cacheResults} method calls is
     * important and applying the {@code cacheLinks} method first would not
     * result in a desirable behaviour because the returned links would not
     * share the cached data of the {@code AsyncDataLink} instances.
     * <P>
     * Note that it is possible to manually remove cached items from the
     * returned {@code AsyncDataQuery} using its
     * {@link CachedAsyncDataQuery#clearCache() clearCache} and
     * {@link CachedAsyncDataQuery#removeFromCache(Object) removeFromCache}
     * methods.
     *
     * @param <QueryArgType> the type of the input argument of the specified
     *   {@code AsyncDataQuery}
     * @param <DataType> the type of the data provided by the specified
     *   (and returned) {@code AsyncDataQuery}
     * @param wrappedQuery the {@code AsyncDataQuery} which actually provides
     *   the data. The returned {@code AsyncDataQuery} will fall back to use
     *   this {@code AsyncDataQuery} for uncached {@code AsyncDataLink}
     *   instances. This argument cannot be {@code null}.
     * @return the {@code AsyncDataQuery} which caches {@code AsyncDataLink}
     *   instances based on the input of the query. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataQuery} is {!code null}
     *
     * @see #cacheLinks(AsyncDataQuery, int)
     * @see #cacheResults(AsyncDataQuery)
     * @see #cacheByID(AsyncDataQuery,ReferenceType, ObjectCache, int)
     */
    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery) {

        return cacheLinks(wrappedQuery, DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates and returns an {@code AsyncDataQuery} which caches
     * {@code AsyncDataLink} instances based on the input of the query. That is,
     * the returned {@code AsyncDataQuery} may return the same
     * {@code AsyncDataLink} for the same input (based on {@code equals}). The
     * data provided by the {@code AsyncDataLink} instances are not cached by
     * the returned query. Those can be cached by using the
     * {@link #cacheResults(AsyncDataQuery) cacheResults} method.
     * <P>
     * The {@code AsyncDataQuery} created by this method caches only at most
     * the specified number of {@code AsyncDataLink} instances and when this
     * limit is exceeded, the one used least recently is discarded from the
     * cache. Also a lease time can be specified when requesting an
     * {@code AsyncDataLink} when it must be removed from the cache and the
     * {@code AsyncDataLink} needs to be recreated if requested again.
     * <P>
     * Note that most often you want to cache both the {@code AsyncDataLink} and
     * the data provided by them. Assume that there is a query which loads the
     * content of a file as a byte array by file name:
     * <code><pre>
     * AsyncDataQuery&lt;Path, byte[]&gt; baseQuery = ...;
     * AsyncDataQuery&lt;CachedLinkRequest&lt;CachedDataRequest&lt;Path&gt;&gt;, byte[]&gt; cachedQuery;
     * cachedQuery = AsyncDatas.cacheLinks(AsyncDatas.cacheResults(baseQuery));
     * </pre></code>
     * In the above example code {@code cachedQuery} will cache both the
     * {@code AsyncDataLink} instances and the data provided by them. Note that
     * order of {@code cacheLinks} and {@code cacheResults} method calls is
     * important and applying the {@code cacheLinks} method first would not
     * result in a desirable behaviour because the returned links would not
     * share the cached data of the {@code AsyncDataLink} instances.
     * <P>
     * Note that it is possible to manually remove cached items from the
     * returned {@code AsyncDataQuery} using its
     * {@link CachedAsyncDataQuery#clearCache() clearCache} and
     * {@link CachedAsyncDataQuery#removeFromCache(Object) removeFromCache}
     * methods.
     *
     * @param <QueryArgType> the type of the input argument of the specified
     *   {@code AsyncDataQuery}
     * @param <DataType> the type of the data provided by the specified
     *   (and returned) {@code AsyncDataQuery}
     * @param wrappedQuery the {@code AsyncDataQuery} which actually provides
     *   the data. The returned {@code AsyncDataQuery} will fall back to use
     *   this {@code AsyncDataQuery} for uncached {@code AsyncDataLink}
     *   instances. This argument cannot be {@code null}.
     * @param maxCacheSize the maximum number of {@code AsyncDataLink} to store
     *   in the cache. When this argument is 0, the returned
     *   {@code AsyncDataQuery} will not actually cache {@code AsyncDataLink}
     *   instances. This argument must greater than or equal to zero.
     * @return the {@code AsyncDataQuery} which caches {@code AsyncDataLink}
     *   instances based on the input of the query. This method never returns
     *   {@code null}.
     *
     * @throws IllegalArgumentException thrown if the {@code maxCacheSize} is
     *   lesser than zero
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataQuery} is {!code null}
     *
     * @see #cacheLinks(AsyncDataQuery)
     * @see #cacheResults(AsyncDataQuery)
     * @see #cacheByID(AsyncDataQuery,ReferenceType, ObjectCache, int)
     */
    public static <QueryArgType, DataType>
            CachedAsyncDataQuery<QueryArgType, DataType> cacheLinks(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {

        return new CachedAsyncDataQuery<>(wrappedQuery, maxCacheSize);
    }

    /**
     * Creates and returns an {@code AsyncDataQuery} which creates
     * {@code AsyncDataLink} instances caching their results and caches
     * {@code AsyncDataLink} instances based on a unique ID provided to the
     * query with the input argument. The returned query will cache at most 128
     * {@code AsyncDataLink} instances concurrently.
     * <P>
     * This method call is similar to calling
     * {@code cacheLinks(cacheResults(wrappedQuery))}, except that the query
     * returned by this method caches {@code AsyncDataLink} instances using a
     * supplied ID instead of the input itself. Caching by ID is preferable when
     * the input is large (i.e. retains considerable memory) because
     * {@code cacheLinks(cacheResults(wrappedQuery))} will actually store the
     * input for the cached {@code AsyncDataLink} instances to be able to get
     * data when it disappears from the cache.
     *
     * @param <QueryArgType> the type of the input of the query
     * @param <DataType> the type of the data provided by the query
     * @param wrappedQuery the query which actually provides the data based on
     *   an input. This argument cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @return the {@code AsyncDataQuery} which creates
     *   {@code AsyncDataLink} instances caching their results and caches
     *   {@code AsyncDataLink} instances based on a unique ID provided to the
     *   query with the input argument. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if {@code wrappedQuery} or
     *   {@code refType} is {@code null}
     *
     * @see #cacheLinks(AsyncDataQuery, int)
     * @see #cacheResults(AsyncDataQuery)
     * @see #cacheByID(AsyncDataQuery,ReferenceType, ObjectCache, int)
     */
    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator) {

        return cacheByID(wrappedQuery, refType, refCreator, DEFAULT_CACHE_SIZE);
    }

    /**
     * Creates and returns an {@code AsyncDataQuery} which creates
     * {@code AsyncDataLink} instances caching their results and caches
     * {@code AsyncDataLink} instances based on a unique ID provided to the
     * query with the input argument. The returned query will cache at most the
     * specified number of {@code AsyncDataLink} instances concurrently.
     * <P>
     * This method call is similar to calling
     * {@code cacheLinks(cacheResults(wrappedQuery))}, except that the query
     * returned by this method caches {@code AsyncDataLink} instances using a
     * supplied ID instead of the input itself. Caching by ID is preferable when
     * the input is large (i.e. retains considerable memory) because
     * {@code cacheLinks(cacheResults(wrappedQuery))} will actually store the
     * input for the cached {@code AsyncDataLink} instances to be able to get
     * data when it disappears from the cache.
     *
     * @param <QueryArgType> the type of the input of the query
     * @param <DataType> the type of the data provided by the query
     * @param wrappedQuery the query which actually provides the data based on
     *   an input. This argument cannot be {@code null}.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param refCreator the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @param maxCacheSize the maximum number of {@code AsyncDataLink} instances
     *   to be cached concurrently by the returned query. This argument must be
     *   greater than or equal to zero.
     * @return the {@code AsyncDataQuery} which creates
     *   {@code AsyncDataLink} instances caching their results and caches
     *   {@code AsyncDataLink} instances based on a unique ID provided to the
     *   query with the input argument. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code maxCacheSize < 0}
     * @throws NullPointerException thrown if {@code wrappedQuery} or
     *   {@code refType} is {@code null}
     *
     * @see #cacheLinks(AsyncDataQuery, int)
     * @see #cacheResults(AsyncDataQuery)
     * @see #cacheByID(AsyncDataQuery,ReferenceType, ObjectCache)
     */
    public static <QueryArgType, DataType>
            CachedByIDAsyncDataQuery<QueryArgType, DataType> cacheByID(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator,
            int maxCacheSize) {

        return new CachedByIDAsyncDataQuery<>(wrappedQuery,
                refType, refCreator, maxCacheSize);
    }

    /**
     * Creates a new {@code AsyncDataQuery} which will provide the same data as
     * the specified {@code AsyncDataQuery} but will apply the user defined
     * conversion on the data. That is, the returned query will transform the
     * {@code AsyncDataLink} instances created by the specified query as done
     * by the {@link #convertResult(AsyncDataLink, DataConverter)} method.
     * <P>
     * Note that the conversion is applied in an {@link AsyncDataListener} and
     * therefore needs to be a fast, non-blocking conversion.
     *
     * @param <QueryArgType> the type of the input of both the specified and the
     *   returned query
     * @param <OldDataType> the type of the data provided by the specified query
     *   which is to be converted
     * @param <NewDataType> the type of the data provided by the returned query
     * @param wrappedQuery the query whose provided data  is to be converted.
     *   This argument cannot be {@code null}.
     * @param converter the {@code DataConverter} defining the conversion of the
     *   data. The {@link DataConverter#convertData(Object) DataConverter#convertData(OldDataType)}
     *   method will be applied to every data provided by the specified query
     *   and the result will actually be provided by the resulting query. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataQuery} which will provide the same data as
     *   the specified {@code AsyncDataQuery} but will apply the user defined
     *   conversion on the data. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #convertResult(AsyncDataLink, DataConverter)
     * @see #convertResults(AsyncDataQuery, AsyncDataQuery)
     */
    public static <QueryArgType, OldDataType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {

        return new AsyncDataQueryConverter<>(wrappedQuery, converter);
    }

    /**
     * Creates an {@link AsyncDataQuery} which will provide the same data
     * as the specified {@code AsyncDataQuery} but applies a conversion on the
     * provided data defined by an {@link AsyncDataQuery}. That is, every
     * data provided by the specified query ({@code wrappedQuery}) will be used
     * as an input for the converter query ({@code converter}) and the data
     * provided by this converter query will be provided by the returned query.
     * <P>
     * This method is best used when the conversion need to applied on the data
     * cannot be executed in the {@link AsyncDataListener#onDataArrive(OldDataType)}
     * method (probably because the conversion takes too much time to complete).
     * <P>
     * The returned query works by converting every {@code AsyncDataLink}
     * created by the specified query using the
     * {@link #convertResult(AsyncDataLink, AsyncDataQuery)} method. For further
     * details how exactly the conversion is done refer to the documentation of
     * the {@link #convertResult(AsyncDataLink, AsyncDataQuery) convertResult}
     * method.
     *
     * @param <QueryArgType> the type of the input of both the specified and the
     *   returned query
     * @param <OldDataType> the type of the data provided by the specified query
     *   which is to be converted
     * @param <NewDataType> the type of the data provided by the returned query
     * @param wrappedQuery the query whose provided data  is to be converted.
     *   This argument cannot be {@code null}.
     * @param converter the {@code AsyncDataQuery} defining the conversion of
     *   the data. The {@link DataConverter#convertData(Object) DataConverter#convertData(OldDataType)}
     *   method will be applied to every data provided by the specified query
     *   and the result will actually be provided by the resulting query. This
     *   argument cannot be {@code null}.
     * @return the {@link AsyncDataQuery} which will provide the same data
     *   as the specified {@code AsyncDataQuery} but applies a conversion on the
     *   provided data defined by an {@link AsyncDataQuery}. This method never
     *   returns {@code null}.
     *
     * @see #convertResult(AsyncDataLink, AsyncDataQuery)
     * @see #convertResults(AsyncDataQuery, DataConverter)
     *
     * @see LinkedDataControl
     */
    public static <QueryArgType, OldDataType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResults(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            AsyncDataQuery<? super OldDataType, ? extends NewDataType> converter) {

        return new LinkedAsyncDataQuery<>(wrappedQuery, converter);
    }

    /**
     * Creates an {@link AsyncDataQuery} which will provide the
     * {@link RefCachedData#getData() data part} of the specified query without
     * the {@link VolatileReference} to it.
     * <P>
     * This method was designed to remove no longer necessary
     * {@code VolatileReference} from the data provided by {@code AsyncDataLink}
     * created by the {@link #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)}
     * method.
     *
     * @param <QueryArgType> the type of the input of both the specified and the
     *   returned query
     * @param <DataType> the type of the data provided by the returned query
     * @param query the {@code AsyncDataQuery} whose data part is to be
     *   extracted and provided by the returned query. This argument cannot be
     *   {@code null}.
     * @return the {@link AsyncDataQuery} which will provide the
     *   {@link RefCachedData#getData() data part} of the specified query. This
     *   method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified query is
     *   {@code null}
     *
     * @see #refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> extractCachedResults(
            AsyncDataQuery<? super QueryArgType, RefCachedData<DataType>> query) {

        return convertResults(query, new CachedDataExtractor<DataType>());
    }

    /**
     * Creates an {@link AsyncDataQuery} which will provide the
     * {@link DataWithUid#getData() data part} of the specified query without
     * the {@link DataWithUid#getID() UID}.
     * <P>
     * This method was designed to remove no longer necessary
     * UID from the data provided by {@code AsyncDataLink} created by the
     * {@link #markResultsWithUid(AsyncDataQuery)} method.
     *
     * @param <QueryArgType> the type of the input of both the specified and the
     *   returned query
     * @param <DataType> the type of the data provided by the returned query
     * @param query the {@code AsyncDataQuery} whose data part is to be
     *   extracted and provided by the returned query. This argument cannot be
     *   {@code null}.
     * @return the {@link AsyncDataQuery} which will provide the
     *   {@link DataWithUid#getData() data part} of the specified query. This
     *   method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified query is
     *   {@code null}
     *
     * @see #markResultsWithUid(AsyncDataQuery)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> removeUidFromResults(
            AsyncDataQuery<? super QueryArgType, DataWithUid<DataType>> query) {

        return convertResults(query, new DataIDRemover<DataType>());
    }

    /**
     * Returns an {@code AsyncDataQuery} which returns the same results as the
     * specified {@code AsyncDataLink} but creates a {@link DataWithUid}
     * object with a new unique {@link DataWithUid#getID() ID} from the provided
     * data of the specified {@code AsyncDataQuery}.
     * <P>
     * The returned {@code AsyncDataQuery} works by applying the
     * {@link #markResultWithUid(AsyncDataLink)} method on every
     * {@code AsyncDataLink} created by the specified query.
     * <P>
     * The {@link DataWithUid#getID() ID} of the provided datas will be unique,
     * so that no other object equals to them. That is, the ID will be different
     * even for same data if requested multiple times.
     *
     * @param <QueryArgType> the type of the input of both the specified and the
     *   returned query
     * @param <DataType> the type of the data provided by the specified
     *   {@code AsyncDataQuery}
     * @param query the {@code AsyncDataQuery} from which the
     *   {@link DataWithUid#getData() data part} is to be derived for the
     *   results of the returned {@code AsyncDataQuery}. This argument cannot be
     *   {@code null}.
     * @return the {@code AsyncDataQuery} providing the {@link DataWithUid}
     *   objects with the data parts provided by the specified
     *   {@code AsyncDataQuery}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AsyncDataQuery} is {@code null}
     *
     * @see #markResultWithUid(AsyncDataLink)
     * @see #removeUidFromResults(AsyncDataQuery)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataWithUid<DataType>> markResultsWithUid(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> query) {

        return convertResults(query, new MarkWithIDConverter<DataType>());
    }
}
