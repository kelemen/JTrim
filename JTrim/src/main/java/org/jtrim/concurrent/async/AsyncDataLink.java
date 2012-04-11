package org.jtrim.concurrent.async;

/**
 * Defines a link to asynchronously access a specific data.
 * <P>
 * Instances of {@code AsyncDataLink} usually only need to store the input
 * needed to find the actual data. For example: A {@code AsyncDataLink} intended
 * to load a particular file, needs to store the path to the file only. And
 * later load the content of the file when requested to do so.
 * <P>
 * The link to the data is intended to be permanent, so users should not hope
 * that the underlying data will change. Note however, that sometimes it is
 * impossible to the implementation to guarantee that it will provide the same
 * data always when requested.
 * <P>
 * To combine{@code AsyncDataLink} instances see the useful utility methods in
 * {@link AsyncDatas}.
 *
 * <h3>Providing the data</h3>
 * When the user of this interface needs the data referenced by the
 * {@code AsyncDataLink}, it needs to invoke the
 * {@link #getData(org.jtrim.concurrent.async.AsyncDataListener) getData}
 * method and provide a listener to which the data will be forwarded when ready.
 * The data need to forwarded by calling the
 * {@link AsyncDataListener#onDataArrive(Object) onDataArrive} method of the
 * listener passing the data as an argument.
 * <P>
 * The data is intended to be provided iteratively with each progress providing
 * a more accurate or simply a super set of the previously provided data.
 * Providing possibly incomplete data however is optional and implementations
 * may only provide the final complete data. However providing the final data
 * is mandatory (unless when providing the data is canceled). The intention of
 * this definition is that when a new data is forwarded to the listener,
 * previously provided datas can be safely ignored and be discarded.
 * <P>
 * It is important to note that the datas must be provided one after another and
 * must never be forwarded concurrently to the same listener. This is because
 * listeners are not required to be thread-safe.
 * <P>
 * As an example when a {@code AsyncDataLink} implementation loads an image
 * based on a file path it can iteratively forward an incomplete image until
 * all the pixels of the image was loaded. Once the image has been loaded: the
 * final complete image must be forwarded to the listener.
 *
 * <h4>Finish providing data</h4>
 * Once the data has been completely loaded (or failed to be loaded due to some
 * unexpected error), the listener must be notified by calling the
 * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
 * of the listener. The report can contain an exception as well to describe the
 * failure which may have occurred. Note that once the client requested the
 * data to be loaded, it is mandatory for every {@code AsyncDataListener}
 * implementation to sooner or later call the {@code onDoneReceive} method.
 *
 * <h4>Controlling the data currently being loaded</h4>
 * The clients can control how the data is to be loaded by the returned
 * {@link AsyncDataController} object and also query the current progress of
 * the loading process. Controlling the loading process is done by sending an
 * object through the {@link AsyncDataController#controlData(Object) controlData}
 * method of the returned {@code AsyncDataController}. How and what can be
 * controlled is completely implementation dependant but it most not affect the
 * final data to be forwarded to the listener. It may affect the time needed to
 * provide the final data but not the actual result. Note that due to some
 * sources being unreliable, this may not be achievable but a best effort must
 * be done to adhere to this contract. Controlling the data is merely intended
 * to be used to affect intermediate datas.
 * <P>
 * Besides controlling the way the data is being loaded, loading the data can
 * be canceled when no longer needed. Note that implementation may ignore
 * cancellation attempts but good implementations should at least make a best
 * attempt to cancel loading the requested data.
 * <P>
 * The final generic feature of the {@code AsyncDataController} is that it can
 * be used to retrieve the current status of the loading process. The most
 * important property of this status is the estimated progress of loading
 * process. Implementations however are recommended to provide other valuable
 * information about the progress.
 *
 * <h4>Optimization possibility for implementations</h4>
 * The {@code AsyncDataListener} provides a
 * {@link AsyncDataListener#requireData() requireData} method which can be
 * polled to determine if the client can actually do something useful with the
 * provided data at the moment. If the listener reports that it does not
 * currently needs the data, the implementation of {@code AsyncDataLink} may
 * omit forwarding intermediate results. Note however that the final data must
 * still be forwarded regardless what this method returns. Also implementations
 * of {@code AsyncDataLink} may completely ignore this method.
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
 * {@link #getData(AsyncDataListener) getData} method must return reasonably
 * fast, must never do expensive tasks synchronously and especially not depend
 * on some external resources.
 *
 * @param <DataType> the type of the data to be accessed. This type is strongly
 *   recommended to be immutable or effectively immutable.
 *
 * @see AsyncDataController
 * @see AsyncDataListener
 * @see AsyncDataQuery
 * @see AsyncDatas
 * @see AsyncFormatHelper
 * @author Kelemen Attila
 */
public interface AsyncDataLink<DataType> {
    /**
     * Starts retrieving the data which is linked to this {@code AsyncDataLink}.
     * The data will be forwarded to the specified listener usually
     * asynchronously on a separate thread. Note however that this method may
     * also forward the data synchronously on the current thread to the listener
     * if it is readily available and does not need some expensive operation to
     * load.
     * <P>
     * Once this method has been called successfully, the
     * {@link AsyncDataListener#onDoneReceive(AsyncReport) onDoneReceive} method
     * of the listener must be called eventually regardless what happens.
     * Failing to call this method is a serious failure of the
     * {@code AsyncDataLink} implementation.
     *
     * @param dataListener the listener to which the data is to be forwarded.
     *   This argument cannot be {@code null}.
     * @return the {@code AsyncDataController} which can be used to
     *   cancel loading the data, control the way it is being loaded and request
     *   the progress of the loading process. This method must never return
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public AsyncDataController getData(AsyncDataListener<? super DataType> dataListener);
}
