package org.jtrim.concurrent.async;

/**
 * Defines an object which can be used to query data based on a specified input.
 * <P>
 * Instances of this class must be able to create a {@link AsyncDataLink} based
 * on the provided input. The {@code AsyncDataLink} then can be used to actually
 * retrieve the input. So to simply retrieve the data based on the provided
 * input, the following code should be used:
 * <P>
 * {@code createDataLink(input).getData(listener)}
 * <P>
 * In most cases {@code AsyncDataQuery} implementations are expected to be
 * relatively simple. They should only create a new instance of an
 * {@code AsyncDataLink} with the specified input.
 * <P>
 * There are some cases however when more sophisticated implementations of
 * {@code AsyncDataQuery} is needed. A notable example of such case is when
 * caching is needed. Such caching mechanism are available in the
 * {@link AsyncDatas} utility class:
 * <ul>
 *  <li>{@link AsyncDatas#cacheResults(AsyncDataQuery)}</li>
 *  <li>{@link AsyncDatas#cacheLinks(AsyncDataQuery, int)}</li>
 *  <li>{@link AsyncDatas#cacheByID(AsyncDataQuery, ReferenceType, ObjectCache, int)}</li>
 * </ul>
 * <P>
 * The intended use of this interface is that implementations should do as
 * little work as possible. That is, an implementation which allow files to be
 * loaded based on the path to the file should not actually also try to prefetch
 * or cache (etc.) it. Such implementation should only return an
 * {@code AsyncDataLink} which actually will load the file when requested.
 * Various other processing should be implemented in a different (preferably
 * generic) implementation and then those implementations should be combined
 * with each other. The {@link AsyncDatas} utility class also provides many
 * methods to link queries and links after each other.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>. Note however that the
 * {@link #createDataLink(AsyncDataListener) createDataLink} method must return
 * reasonably fast, must never do expensive tasks synchronously and especially
 * not depend on some external resources.
 *
 * @param <QueryArgType> the type of the input of the query. It is recommended
 *   that this type be immutable or effectively immutable.
 * @param <DataType> the type of the data to be retrieved. As with
 *   {@code AsyncDataLink}, this type is strongly recommended to be immutable or
 *   effectively immutable.
 *
 * @see AsyncDataController
 * @see AsyncDataLink
 * @see AsyncDataListener
 * @see AsyncDatas
 * @author Kelemen Attila
 */
public interface AsyncDataQuery<QueryArgType, DataType> {
    /**
     * Creates a {@code AsyncDataLink} which will provide data based on the
     * specified input.
     * <P>
     * This method must return immediately without blocking and should not
     * actually start retrieving the requested data (not in way detectable by
     * the user but for example: prefetching is allowed).
     *
     * @param arg the input argument which is to be used to retrieve the data.
     *   It is implementation dependent if {@code null} is allowed as an
     *   argument.
     * @return the {@code AsyncDataLink} which will provide data based on the
     *   specified input. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the implementation does not
     *   allow {@code null} as an input but {@code null} was passed
     */
    public AsyncDataLink<DataType> createDataLink(QueryArgType arg);
}
