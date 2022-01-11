package org.jtrim2.concurrent.query;

/**
 * Listens for data provided by an {@link AsyncDataLink}.
 * <P>
 * Data is actually submitted to this listener by calling the
 * {@link #onDataArrive(Object) onDataArrive(DataType)} method. This method may
 * be called multiple times subsequently to provide more and more accurate
 * datas. Once there will be no more datas provided the
 * {@link #onDoneReceive(AsyncReport) onDoneReceive(AsyncReport)} method must
 * be called with the appropriate status.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are not required to be safe to use by
 * multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data provided by the {@link AsyncDataLink}.
 *   This type is strongly recommended to be immutable or effectively immutable.
 *
 * @see AsyncDataLink
 * @see AsyncHelper
 */
public interface AsyncDataListener<DataType> {
    /**
     * Invoked when data is available or more detailed information is available.
     * <P>
     * This method may invoked repeatedly with more and more detailed data until
     * the final and complete data is available. The general rule for the data
     * to be provided to this method is that any previously reported data may be
     * discarded safely because the new data is at least as accurate as the
     * previous one.
     * <P>
     * Once the final data was reported to this method the
     * {@link #onDoneReceive(AsyncReport) onDoneReceive} method must be called
     * to notify this listener that no more data will be provided.
     * <P>
     * Note that this method must not be called concurrently twice for the same
     * listener as listener implementations are not expected to be thread-safe.
     * Also this method may only be invoked before the {@code onDoneReceive}
     * has been called. That is every invocation of this method
     * <I>happen-before</I>, the invocation of the {@code onDoneReceive} method
     * of the same data request.
     *
     * @param data the data provided by the {@link AsyncDataLink}. This
     *   argument can only be {@code null} if the underlying
     *   {@code AsyncDataLink} implementations allows sending {@code null} data
     *   objects. However, allowing sending {@code null} data objects is not
     *   recommended.
     */
    public void onDataArrive(DataType data);

    /**
     * Invoked after the last data was forwarded to the
     * {@link #onDataArrive(Object) onDataArrive} method. This method must be
     * called exactly once per data request.
     * <P>
     * This method must be called under every circumstances, even if the
     * requested data could not have been provided completely
     * (e.g.: it was canceled).
     * <P>
     * Note that this method must only be called after every
     * {@code onDataArrive} method invocation has returned and must not be
     * called concurrently with such method invocation.
     *
     * @param report the {@code AsyncReport} object describing the state
     *   querying the data finished. That is, the exception if it could not be
     *   completed successfully due to an error and if querying the data was
     *   canceled before completion. This argument cannot be {@code null}.
     */
    public void onDoneReceive(AsyncReport report);
}
