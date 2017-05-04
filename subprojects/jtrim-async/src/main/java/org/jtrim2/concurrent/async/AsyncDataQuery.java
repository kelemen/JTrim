package org.jtrim2.concurrent.async;

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
 * {@link AsyncQueries} utility class:
 * <ul>
 *  <li>{@link AsyncQueries#cacheResults(AsyncDataQuery)}</li>
 *  <li>{@link AsyncQueries#cacheLinks(AsyncDataQuery, int)}</li>
 *  <li>{@link AsyncQueries#cacheByID(AsyncDataQuery, ReferenceType, ObjectCache, int)}</li>
 * </ul>
 * <P>
 * The intended use of this interface is that implementations should do as
 * little work as possible. That is, an implementation which allow files to be
 * loaded based on the path to the file should not actually also try to prefetch
 * or cache (etc.) it. Such implementation should only return an
 * {@code AsyncDataLink} which actually will load the file when requested.
 * Various other processing should be implemented in a different (preferably
 * generic) implementation and then those implementations should be combined
 * with each other. The {@link AsyncQueries} utility class also provides many
 * methods to link queries and links after each other.
 *
 * <h3>String representation of data links and queries</h3>
 * Since {@code AsyncDataLink} and {@code AsyncDataQuery} instances can be
 * attached in a convoluted way, it can be very helpful if the
 * {@link Object#toString() toString()} method returns a human readable string
 * describing what the {@code AsyncDataLink} will do. The string representation
 * is not intended to be parsed or even be parsable it is only intended to
 * contain helpful information when debugging an application. To be consistent
 * with the string representation provided by implementations in <EM>JTrim</EM>,
 * the following guidelines should be used:
 * <P>
 * <ul>
 *  <li>
 *   The representation should be multi-lined each line describing a single
 *   action.
 *  </li>
 *  <li>
 *   The representation should be readable from top to bottom describing the
 *   consecutive actions.
 *  </li>
 *  <li>
 *   When an {@code AsyncDataLink} or {@code AsyncDataQuery} wraps another
 *   query, the string representation of the subquery or sublink should be
 *   indented. The indentations should be done using the {@code appendIndented}
 *   methods of the {@link AsyncFormatHelper} class.
 *  </li>
 *  <li>
 *   When working with arrays or collections it is recommended to add the
 *   content as an indented multi-line string with each element in a separate
 *   line. The {@code AsyncFormatHelper} contains methods to format them so.
 *  </li>
 *  <li>
 *   The efficiency is not an issue because the string representation is
 *   intended to be used for debugging only.
 *  </li>
 *  <li>
 *   The methods in {@link AsyncFormatHelper} should be used whenever possible
 *   for better consistency.
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>. Note however that the
 * {@link #createDataLink(Object) createDataLink} method must return
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
 * @see AsyncQueries
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
