package org.jtrim2.concurrent.async;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.executor.UpdateTaskExecutor;

/**
 * Contains static factory methods for useful {@link AsyncDataLink}
 * implementations.
 * <P>
 * This class cannot be inherited or instantiated.
 *
 * <h3>Thread safety</h3>
 * Unless otherwise noted, methods of this class are safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see AsyncDataLink
 * @see AsyncQueries
 * @see AsyncHelper
 */
public final class AsyncLinks {
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
    public static <OldType, NewType> AsyncDataLink<NewType> convertResultSync(
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
     * {@code AsyncDataLink} provides a {@link java.nio.file.Path Path} from an
     * external source to a file and the converter loads the image from a given
     * path, these can be combined to create an {@code AsyncDataLink} which
     * loads an image file from this external source.
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
    public static <OldType, NewType> AsyncDataLink<NewType> convertResultAsync(
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
     * as the specified {@code AsyncDataLink}. That is the returned
     * {@code AsyncDataLink} will simply return the data part for
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

        return convertResultSync(link, new CachedDataExtractor<DataType>());
    }

    /**
     * Returns an {@code AsyncDataLink} which retrieves the
     * {@link DataWithUid#getData() data part} from the {@link DataWithUid}
     * results of the given {@code AsyncDataLink}.
     * <P>
     * The returned {@code AsyncDataLink} will return the same number of results
     * as the specified {@code AsyncDataLink}. That is the returned
     * {@code AsyncDataLink} will simply return the data part for each received
     * {@link DataWithUid} in the same order.
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
     * @see AsyncQueries#markResultsWithUid(AsyncDataQuery)
     * @see AsyncQueries#removeUidFromResults(AsyncDataQuery)
     */
    public static <DataType> AsyncDataLink<DataType> removeUidFromResult(
            AsyncDataLink<DataWithUid<DataType>> link) {

        return convertResultSync(link, new DataIDRemover<>());
    }

    /**
     * Returns an {@code AsyncDataLink} which returns the same results as the
     * specified {@code AsyncDataLink} but creates a {@link DataWithUid}
     * object with a new unique {@link DataWithUid#getID() ID} from the
     * results of the specified {@code AsyncDataLink}.
     * <P>
     * The returned {@code AsyncDataLink} will return the same number of results
     * as the specified {@code AsyncDataLink}. That is the returned
     * {@code AsyncDataLink} will simply return a new {@code DataWithUid} with
     * the same data and a unique ID.
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
     * @see AsyncQueries#markResultsWithUid(AsyncDataQuery)
     * @see #removeUidFromResult(AsyncDataLink)
     * @see AsyncQueries#removeUidFromResults(AsyncDataQuery)
     */
    public static <DataType> AsyncDataLink<DataWithUid<DataType>> markResultWithUid(
            AsyncDataLink<? extends DataType> link) {
        return convertResultSync(link, new MarkWithIDConverter<>());
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results. The actual
     * caching mechanism is defined by the specified {@link ObjectCache} and
     * therefore the cached data is stored in a
     * {@link org.jtrim2.cache.VolatileReference VolatileReference}.
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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
                AsyncHelper.DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results. The actual
     * caching mechanism is defined by the specified {@link ObjectCache} and
     * therefore the cached data is stored in a
     * {@link org.jtrim2.cache.VolatileReference VolatileReference}.
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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
     * in a {@link org.jtrim2.cache.VolatileReference VolatileReference}.
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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
                AsyncHelper.DEFAULT_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns an {@code AsyncDataLink} which will return the same datas as the
     * specified {@code AsyncDataLink} but will cache its results and also
     * return the cached reference. The actual caching mechanism is defined by
     * the specified {@link ObjectCache} and therefore the cached data is stored
     * in a {@link org.jtrim2.cache.VolatileReference VolatileReference}.
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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

        Objects.requireNonNull(reportExecutor, "reportExecutor");

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
     * {@link AsyncDataLink#getData(org.jtrim2.cancel.CancellationToken, AsyncDataListener)}
     * method and not on a separate thread. Therefore once the {@code getData}
     * method returns it is guaranteed that the data providing has completed
     * (i.e.: the {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive}
     * has been called).
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

    private AsyncLinks() {
        throw new AssertionError();
    }
}
