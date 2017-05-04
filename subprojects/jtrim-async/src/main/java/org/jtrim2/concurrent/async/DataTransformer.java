package org.jtrim2.concurrent.async;

/**
 * Defines a transformation of objects. The transformation does not need to be
 * reversible.
 * <P>
 * Note that in case this transformation is slow (e.g.: waits for external
 * events, like reading a file), consider using {@link AsyncDataTransformer} or
 * simply an {@link AsyncDataQuery} for transformation.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be used by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the object to be transformed (and also the type of the
 *   resulting object)
 *
 * @see DataConverter
 *
 * @author Kelemen Attila
 */
public interface DataTransformer<DataType> {
    /**
     * Transforms the data as defined by the implementation. Note that the
     * transformation might not be reversible.
     *
     * @param data the object to be transformed. This argument can be
     *   {@code null} if the transformation supports converting {@code null}
     *   values.
     * @return the transformed data as defined by the implementation. This
     *   method may or may not returns {@code null}, depending on the
     *   implementation.
     */
    public DataType transform(DataType data);
}
