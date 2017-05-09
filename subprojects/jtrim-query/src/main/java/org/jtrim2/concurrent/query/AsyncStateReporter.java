package org.jtrim2.concurrent.query;

/**
 * Defines the listener which will be notified periodically by the
 * {@link AsyncDataLink} created by the
 * {@link AsyncLinks#createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit) AsyncLinks.createStateReporterLink}
 * method.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface do not need to be safe to be called by
 * multiple threads concurrently. The {@code AsyncDataLink} notifying the method
 * of this interface does not call the method concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of the {@link #reportState(AsyncDataLink, AsyncDataListener, AsyncDataController) reportState}
 * method must be as quick as possible and it must not wait for external events
 * or for other threads. Implementations of this interface in general are not
 * required to be <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data which is being retrieved by the
 *   {@link AsyncDataLink} reporting its state.
 *
 * @see AsyncLinks#createStateReporterLink(UpdateTaskExecutor, AsyncDataLink, AsyncStateReporter, long, TimeUnit)
 */
public interface AsyncStateReporter<DataType> {
    /**
     * The method to be called periodically to be notified of the state of the
     * progress of the data retrieving process.
     * <P>
     * This method is invoked until the data retrieving is finished (either due
     * to completion, cancellation or an error). Note however, that this method
     * may be invoked after the data retrieving is finished but should be
     * stopped to be called not much after the data retrieving is finished.
     * <P>
     * To actually query the state of progress of the data retrieving process,
     * use the specified {@code AsyncDataController}.
     *
     * @param dataLink the {@code AsyncDataLink} which is providing the data
     *   whose state is being watched. This argument cannot be {@code null}.
     * @param dataListener the {@code AsyncDataListener} to be notified of the
     *   retrieved data. This argument cannot be {@code null}.
     * @param controller the {@code AsyncDataController} which can be used to
     *   retrieve the state of the data retrieving process. This argument
     *   cannot be {@code null}.
     */
    public void reportState(
            AsyncDataLink<DataType> dataLink,
            AsyncDataListener<? super DataType> dataListener,
            AsyncDataController controller);
}
