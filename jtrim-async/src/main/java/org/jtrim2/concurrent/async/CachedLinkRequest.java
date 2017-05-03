package org.jtrim2.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a request for a data when the {@link AsyncDataLink} providing should
 * be cached. The request is intended to passed to an {@link AsyncDataQuery} as
 * an input.
 * <P>
 * Apart from the input of the query, the request contains an timeout value
 * after the cached {@code AsyncDataLink} becomes invalid and should be
 * recreated when needed again.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed by multiple threads
 * concurrently. Instances of this class are immutable except that the actual
 * {@link #getQueryArg() input} of the query can possibly be mutable. Note
 * however that it is recommended that this input be immutable as well making
 * instances of this class completely immutable.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <QueryArgType> the type of the actual input of the query for the
 *   data retrieval process. Note that this type is recommended to be immutable
 *   or effectively immutable.
 *
 * @see AsyncQueries#cacheByID(AsyncDataQuery, org.jtrim2.cache.ReferenceType, org.jtrim2.cache.ObjectCache, int)
 * @see AsyncQueries#cacheLinks(AsyncDataQuery, int)
 *
 * @author Kelemen Attila
 */
public final class CachedLinkRequest<QueryArgType> {
    private static final int DEFAULT_EXPIRE_TIMEOUT_MINS = 60;

    private final QueryArgType queryArg;
    private final long cacheExpireNanos;

    /**
     * Creates and initializes the {@code CachedLinkRequest} with the given
     * input for the query and one hour as the
     * {@link #getCacheExpire(TimeUnit) cache expire timeout} value.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     */
    public CachedLinkRequest(QueryArgType queryArg) {
        this(queryArg, DEFAULT_EXPIRE_TIMEOUT_MINS, TimeUnit.MINUTES);
    }

    /**
     * Creates and initializes the {@code CachedLinkRequest} with the given
     * input for the query and the cache expire timeout value.
     *
     * @param queryArg the object used as the input of the
     *   {@link AsyncDataQuery} to retrieve the requested data. This argument
     *   can be {@code null} if the query accepts {@code null} values as its
     *   input.
     * @param cacheExpire the time in the given unit after the
     *   {@code AsyncDataLink} instance created using this request becomes
     *   invalid and should be recreated. This argument must be greater than or
     *   equal to zero.
     * @param timeunit the time unit of the {@code cacheExpire} argument.
     *   This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code cacheExpire < 0}
     * @throws NullPointerException thrown if {@code timeunit} is {@code null}
     */
    public CachedLinkRequest(QueryArgType queryArg, long cacheExpire, TimeUnit timeunit) {
        ExceptionHelper.checkArgumentInRange(cacheExpire, 0, Long.MAX_VALUE, "cacheExpire");
        ExceptionHelper.checkNotNullArgument(timeunit, "timeunit");

        this.queryArg = queryArg;
        this.cacheExpireNanos = timeunit.toNanos(cacheExpire);
    }

    /**
     * Returns the timeout value in the given time unit after an
     * {@code AsyncDataLink} requested by this request becomes invalid and must
     * be recreated.
     *
     * @param timeunit the time unit in which the timeout value is to be
     *   returned. This argument cannot be {@code null}.
     * @return the timeout value in the given time unit after an
     *   {@code AsyncDataLink} requested by this request becomes invalid and
     *   must be recreated. This method always returns a value greater than or
     *   equal to zero.
     */
    public long getCacheExpire(TimeUnit timeunit) {
        return timeunit.convert(cacheExpireNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the object used as the input of the query of the data. That is,
     * this is the only property which determines what data is to be retrieved.
     *
     * @return the object used as the input of the query of the data. This
     *   method may return {@code null} if {@code null} was passed in the
     *   constructor.
     */
    public QueryArgType getQueryArg() {
        return queryArg;
    }

    /**
     * Returns the string representation of this {@code CachedLinkRequest} in no
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
                + ", Expire=" + getCacheExpire(TimeUnit.MILLISECONDS)
                + " ms}";
    }
}
