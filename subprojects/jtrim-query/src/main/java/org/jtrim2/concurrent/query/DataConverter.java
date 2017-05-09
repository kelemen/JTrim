package org.jtrim2.concurrent.query;

/**
 * Defines a conversion from one type to another. The conversion does not need
 * to be reversible and, of course, there can be multiple conversion defined
 * from one type to another.
 * <P>
 * Note that in case this conversion is slow (e.g.: waits for external events,
 * like reading a file), consider using {@link AsyncDataConverter} or simply an
 * {@link AsyncDataQuery} for conversion.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be used by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <OldDataType> the type of the object to be converted
 * @param <NewDataType> the type of the object returned by the conversion
 *
 * @see AsyncLinks#convertResult(AsyncDataLink, DataConverter)
 * @see AsyncQueries#convertResults(AsyncDataQuery, DataConverter)
 * @see DataTransformer
 */
public interface DataConverter<OldDataType, NewDataType> {
    /**
     * Converts the data from one type to another as defined by the
     * implementation. Note that the conversion might not be reversible.
     *
     * @param data the object to be converted. This argument can be
     *   {@code null} if the conversion supports converting {@code null} values.
     * @return the converted data as defined by the implementation. This method
     *   may or may not returns {@code null}, depending on the implementation.
     */
    public NewDataType convertData(OldDataType data);
}
