package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a request for a data which should be cached if possible. The request
 * is intended to passed to an {@link AsyncDataQuery} as an input.
 * <P>
 * Apart from the input of the query, the request contains the cache to be used
 * to cache the requested data and a timeout value. The timeout value determines
 * how much time must elapse until an abandoned data retrieval request (i.e.:
 * when every data retrieval request was canceled) should actually stop the data
 * retrieval process. It can be advantageous not to stop the data retrieval
 * process immediately when every request was canceled if there is a chance,
 * that the data can be requested soon.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class are immutable except that its
 * properties might be mutable objects (i.e.: the
 * {@link #getObjectCache() ObjectCache} and possibly the
 * {@link #getQueryArg() QueryArg}).
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <QueryArgType> the type of the actual input of the query for the
 *   data retrieval process. Note that this type is recommended to be immutable
 *   or effectively immutable.
 *
 * @see AsyncDatas#cacheResult(AsyncDataLink, ReferenceType, ObjectCache)
 * @see AsyncDatas#cacheResult(AsyncDataLink, ReferenceType, ObjectCache, long, TimeUnit)
 *
 * @author Kelemen Attila
 */
public final class CachedDataRequest<QueryArgType> {
    private static final int DEFAULT_TIMEOUT = 5 * 1000; // ms

    private final QueryArgType queryArg;
    private final ReferenceType refType;
    private final ObjectCache objectCache;
    private final long dataCancelTimeout;

    /**
     * Creates and initializes the {@code CachedDataRequest} with the given
     * properties.
     * <P>
     * The {@code ObjectCache} used to cache data is
     * {@link JavaRefObjectCache#INSTANCE} and the reference type is
     * {@link ReferenceType#WeakRefType} using this constructor.
     * <P>
     * The time in the given unit to wait before actually canceling abandoned
     * requests is 5 seconds using this constructor.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     *
     * @throws NullPointerException thrown if {@code refType} is {@code null}
     */
    public CachedDataRequest(QueryArgType queryArg) {
        this(queryArg, ReferenceType.WeakRefType);
    }

    /**
     * Creates and initializes the {@code CachedDataRequest} with the given
     * properties.
     * <P>
     * The {@code ObjectCache} used to cache data is
     * {@link JavaRefObjectCache#INSTANCE} using this constructor.
     * <P>
     * The time in the given unit to wait before actually canceling abandoned
     * requests is 5 seconds using this constructor.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the {@code JavaRefObjectCache.INSTANCE} cache. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if {@code refType} is {@code null}
     */
    public CachedDataRequest(QueryArgType queryArg, ReferenceType refType) {
        this(queryArg, refType, JavaRefObjectCache.INSTANCE);
    }

    /**
     * Creates and initializes the {@code CachedDataRequest} with the given
     * properties.
     * <P>
     * The time in the given unit to wait before actually canceling abandoned
     * requests is 5 seconds using this constructor.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param objectCache the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     *
     * @throws NullPointerException thrown if {@code refType} is {@code null}
     */
    public CachedDataRequest(QueryArgType queryArg, ReferenceType refType, ObjectCache objectCache) {
        this(queryArg, refType, objectCache, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates and initializes the {@code CachedDataRequest} with the given
     * properties.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     * @param refType the {@code ReferenceType} to be used to reference the
     *   cached data using the specified {@code ObjectCache}. This argument
     *   cannot be {@code null}.
     * @param objectCache the {@code ObjectCache} to use to cache the data. This
     *   argument can be {@code null} in which case
     *   {@link org.jtrim.cache.JavaRefObjectCache#INSTANCE} is used as the
     *   {@code ObjectCache}.
     * @param dataCancelTimeout the time in the given unit to wait before
     *   actually canceling abandoned requests. Before this time elapses, it is
     *   possible to start requesting the data and continuing where the request
     *   was left off. This argument must be greater than or equal to zero.
     *   In case this argument is zero, the data requesting will be canceled as
     *   soon as the data is detected to be not required.
     * @param timeunit the time unit of the {@code dataCancelTimeout} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code dataCancelTimeout < 0}
     * @throws NullPointerException thrown if {@code refType} or
     *   {@code timeunit} is {@code null}
     */
    public CachedDataRequest(QueryArgType queryArg,
            ReferenceType refType, ObjectCache objectCache,
            long dataCancelTimeout, TimeUnit timeunit) {

        ExceptionHelper.checkNotNullArgument(refType, "refType");
        ExceptionHelper.checkNotNullArgument(timeunit, "timeunit");
        ExceptionHelper.checkArgumentInRange(dataCancelTimeout, 0, Long.MAX_VALUE, "dataCancelTimeout");

        this.queryArg = queryArg;
        this.refType = refType;
        this.objectCache = objectCache != null
                ? objectCache
                : JavaRefObjectCache.INSTANCE;
        this.dataCancelTimeout = timeunit.toNanos(dataCancelTimeout);
    }

    /**
     * Returns the timeout value to wait before actually canceling abandoned
     * data retrieval processes in the given time unit.
     * <P>
     * Before this time elapses, it is possible to start requesting the data and
     * continuing where the request was left off. This is advantageous when
     * there is a chance of an abandoned data may be requested again soon.
     *
     * @param timeunit the time unit in which the timeout value is to be
     *   returned. This argument cannot be {@code null}.
     * @return the timeout value to wait before actually canceling abandoned
     *   data retrieval processes in the given time unit. This method always
     *   returns a value greater than or equal to zero.
     *
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public long getDataCancelTimeout(TimeUnit timeunit) {
        return timeunit.convert(dataCancelTimeout, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the {@code ObjectCache} used to cache the data to be retrieved.
     *
     * @return the {@code ObjectCache} used to cache the data to be retrieved.
     *   This method never returns {@code null}.
     */
    public ObjectCache getObjectCache() {
        return objectCache;
    }

    /**
     * Returns the object used as the input of the query of the data. That is,
     * this is the only property which determines what data is to be retrieved.
     * Other properties only define the behaviour of the cache.
     *
     * @return the object used as the input of the query of the data. This
     *   method may return {@code null} if {@code null} was passed in the
     *   constructor.
     */
    public QueryArgType getQueryArg() {
        return queryArg;
    }

    /**
     * Returns the {@code ReferenceType} to be used to reference the cached data
     * using the {@link #getObjectCache() ObjectCache} property. That is, this
     * value is intended to be passed to the
     * {@link ObjectCache#getReference(Object, ReferenceType) getReference}
     * method of the {@code ObjectCache}.
     *
     * @return the {@code ReferenceType} to be used to reference the cached data
     *   using the {@link #getObjectCache() ObjectCache} property. This method
     *   never returns {@code null}.
     */
    public ReferenceType getRefType() {
        return refType;
    }

    /**
     * Returns the string representation of this {@code CachedDataRequest} in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "CachedDataRequest{"
                + "Arg=" + queryArg
                + ", RefCreator=" + objectCache
                + ", RefType=" + refType
                + ", TimeOut=" + getDataCancelTimeout(TimeUnit.MILLISECONDS)
                + " ms}";
    }
}
