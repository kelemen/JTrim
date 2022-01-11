package org.jtrim2.concurrent.query.io;

import java.io.IOException;
import java.io.InputStream;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an interface for opening a new input stream for reading. This
 * interface must open a new stream for each request.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 */
public interface InputStreamOpener {
    /**
     * Opens a new stream to the source defined by this
     * {@code InputStreamOpener} instance. This method opens a new stream each
     * time it is called an these opened channels must be
     * {@link InputStream#close() closed} separately.
     *
     * @param cancelToken the {@code CancellationToken} through which callers
     *   may notify this method that the channel is no longer need to be
     *   opened. Implementations are free to ignore this request but if they
     *   don't, they must throw an {@link org.jtrim2.cancel.OperationCanceledException}
     *   in response to the cancellation request. This argument cannot be
     *   {@code null}.
     * @return the new stream to the source defined by this
     *   {@code InputStreamOpener} instance. The returned instance must be
     *   {@link InputStream#close() closed} in order to prevent resource
     *   leakage. This method never returns {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if
     *   cancellation was detected by this method
     * @throws IOException thrown if the stream could not be opened for some
     *   reasons
     */
    public InputStream openStream(CancellationToken cancelToken) throws IOException;
}
