package org.jtrim.swing.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.concurrent.async.AsyncDataState;

/**
 * Defines the state of an asynchronous rendering request. This result is
 * intended to be associated with a rendering request passed to an
 * {@link AsyncRenderer}.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be accessed
 * from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see AsyncRenderer
 *
 * @author Kelemen Attila
 */
public interface RenderingState {
    /**
     * Returns {@code true} if the asynchronous rendering process has completed.
     * When this object refers to a {@link DataRenderer} this implies that the
     * {@link DataRenderer#finishRendering(CancellationToken, AsyncReport) finishRendering}
     * method of the associated {@code DataRenderer} has already returned.
     *
     * @return {@code true} if the asynchronous rendering process has completed,
     *   {@code false} otherwise
     */
    public boolean isRenderingFinished();

    /**
     * Returns the time elapsed since the rendering started. The return value
     * of this method is independent of the completion of the rendering request.
     *
     * @param unit the time unit in which the result is to be interpreted.
     *   This argument cannot be {@code null}.
     * @return the time in the specified time unit elapsed since the start of
     *   the associated rendering request
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public long getRenderingTime(TimeUnit unit);

    /**
     * Returns the state of asynchronous data retrieval process associated with
     * the rendering. That is, this method returns the state returned by the
     * {@link org.jtrim.concurrent.async.AsyncDataLink} passed to
     * {@link AsyncRenderer#render(CancellationToken, AsyncDataLink, DataRenderer) AsyncRenderer.render}.
     *
     * @return the state of asynchronous data retrieval process associated with
     *   the rendering. This method may return {@code null} if the state is
     *   not available.
     */
    public AsyncDataState getAsyncDataState();
}
