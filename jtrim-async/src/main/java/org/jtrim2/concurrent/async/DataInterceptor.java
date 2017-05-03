package org.jtrim2.concurrent.async;

/**
 * Defines an interface for intercepting data provided by an
 * {@link AsyncDataLink}. That is, before actually notifying the
 * {@link AsyncDataListener}, the appropriate method of this interface is
 * notified first. It is also possible to filter out some data, so it won't be
 * forwarded to the listener.
 * <P>
 * This interface is used by the {@code AsyncDataLink} create by the
 * {@link AsyncLinks#interceptData(AsyncDataLink, DataInterceptor)} method.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be used by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>. Note however, that they must be quick
 * non-blocking methods.
 *
 * @param <DataType> the type of the data being intercepted
 *
 * @see AsyncLinks#interceptData(AsyncDataLink, DataInterceptor)
 *
 * @author Kelemen Attila
 */
public interface DataInterceptor<DataType> {
    /**
     * Invoked before data is forwarded to the {@link AsyncDataListener}.
     * It is possible to filter out this data (i.e.: not forward it to the
     * listener) by returning {@code false}.
     * <P>
     * Since this method is called in a listener, it should be a quick
     * non-blocking method. Also if this method throws an exception, the data
     * will not be forwarded (as if returning {@code false}). Note however, that
     * like {@code AsyncDataListener}, this method should throw an exception.
     *
     * @param newData the data received and to be forwarded to the
     *   {@code AsyncDataListener}. This argument can be {@code null} if it is
     *   possible for the providing {@link AsyncDataLink} to provide
     *   {@code null} data objects.
     * @return {@code true} if the specified data is to be forwarded to the
     *   {@code AsyncDataListener}, {@code false} if this data should be
     *   filtered out.
     */
    public boolean onDataArrive(DataType newData);

    /**
     * Invoked before the {@link AsyncDataListener}
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} was
     * called.
     * <P>
     * The {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} of
     * the listener will be called after this method returns even if it throws
     * an exception. Note however, that this method should not throw exceptions,
     *
     * @param report the {@code AsyncReport} which is to be passed to the
     *   {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive}
     *   method of the {@code AsyncDataListener}. This argument cannot be
     *   {@code null}.
     */
    public void onDoneReceive(AsyncReport report);
}

