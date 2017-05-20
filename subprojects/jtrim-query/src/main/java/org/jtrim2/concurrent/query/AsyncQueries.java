package org.jtrim2.concurrent.query;

import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;

/**
 * Contains static factory methods for useful {@link AsyncDataQuery}
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
 * @see AsyncDataQuery
 * @see AsyncLinks
 * @see AsyncHelper
 */
public final class AsyncQueries {

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
     * {@link AsyncLinks#cacheResult(AsyncDataLink, ReferenceType,ObjectCache)}
     * method.
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
     * @see AsyncLinks#cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
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
     * <pre>{@code
     * AsyncDataQuery<Path, byte[]> baseQuery = ...;
     * AsyncDataQuery<CachedLinkRequest<CachedDataRequest<Path>>, byte[]> cachedQuery;
     * cachedQuery = AsyncQueries.cacheLinks(AsyncQueries.cacheResults(baseQuery));
     * }</pre>
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

        return cacheLinks(wrappedQuery, AsyncHelper.DEFAULT_CACHE_SIZE);
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
     * <pre>{@code
     * AsyncDataQuery<Path, byte[]> baseQuery = ...;
     * AsyncDataQuery<CachedLinkRequest<CachedDataRequest<Path>>, byte[]> cachedQuery;
     * cachedQuery = AsyncQueries.cacheLinks(AsyncQueries.cacheResults(baseQuery));
     * }</pre>
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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

        return cacheByID(wrappedQuery, refType, refCreator, AsyncHelper.DEFAULT_CACHE_SIZE);
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
     *   {@link org.jtrim2.cache.JavaRefObjectCache#INSTANCE} is used as the
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
     * by the {@link AsyncLinks#convertResultSync(AsyncDataLink, DataConverter)}
     * method.
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
     * @see AsyncLinks#convertResultSync(AsyncDataLink, DataConverter)
     * @see #convertResultsAsync(AsyncDataQuery, AsyncDataQuery)
     */
    public static <QueryArgType, OldDataType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResultsSync(
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
     * cannot be executed in the
     * {@link AsyncDataListener#onDataArrive(Object) AsyncDataListener.onDataArrive}
     * method (probably because the conversion takes too much time to complete).
     * <P>
     * The returned query works by converting every {@code AsyncDataLink}
     * created by the specified query using the
     * {@link AsyncLinks#convertResultAsync(AsyncDataLink, AsyncDataQuery)} method.
     * For further details how exactly the conversion is done refer to the
     * documentation of the {@link AsyncLinks#convertResultAsync(AsyncDataLink, AsyncDataQuery) convertResult}
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
     * @see AsyncLinks#convertResultAsync(AsyncDataLink, AsyncDataQuery)
     * @see #convertResultsSync(AsyncDataQuery, DataConverter)
     *
     * @see LinkedDataControl
     */
    public static <QueryArgType, OldDataType, NewDataType>
            AsyncDataQuery<QueryArgType, NewDataType> convertResultsAsync(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            AsyncDataQuery<? super OldDataType, ? extends NewDataType> converter) {

        return new LinkedAsyncDataQuery<>(wrappedQuery, converter);
    }

    /**
     * Creates an {@link AsyncDataQuery} which will provide the
     * {@link RefCachedData#getData() data part} of the specified query without
     * the {@link org.jtrim2.cache.VolatileReference VolatileReference} to it.
     * <P>
     * This method was designed to remove no longer necessary
     * {@code VolatileReference} from the data provided by {@code AsyncDataLink}
     * created by the {@link AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)}
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
     * @see AsyncLinks#refCacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataType> extractCachedResults(
            AsyncDataQuery<? super QueryArgType, RefCachedData<DataType>> query) {

        return convertResultsSync(query, new CachedDataExtractor<>());
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

        return convertResultsSync(query, new DataIDRemover<>());
    }

    /**
     * Returns an {@code AsyncDataQuery} which returns the same results as the
     * specified {@code AsyncDataLink} but creates a {@link DataWithUid}
     * object with a new unique {@link DataWithUid#getID() ID} from the provided
     * data of the specified {@code AsyncDataQuery}.
     * <P>
     * The returned {@code AsyncDataQuery} works by applying the
     * {@link AsyncLinks#markResultWithUid(AsyncDataLink)} method on every
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
     * @see AsyncLinks#markResultWithUid(AsyncDataLink)
     * @see #removeUidFromResults(AsyncDataQuery)
     */
    public static <QueryArgType, DataType>
            AsyncDataQuery<QueryArgType, DataWithUid<DataType>> markResultsWithUid(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> query) {

        return convertResultsSync(query, new MarkWithIDConverter<>());
    }

    private AsyncQueries() {
        throw new AssertionError();
    }
}
