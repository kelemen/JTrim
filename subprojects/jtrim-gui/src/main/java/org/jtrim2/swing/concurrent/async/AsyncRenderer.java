package org.jtrim2.swing.concurrent.async;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.async.AsyncDataLink;

/**
 * Defines an object which is capable to render a component asynchronously.
 * <P>
 * To render the component associated with the {@code AsyncRenderer} call
 * its {@link #render(CancellationToken, AsyncDataLink, DataRenderer) render}
 * method with the arguments used to do the rendering. Multiple calls to the
 * {@code render} method may overwrite each other. That is, only the last
 * {@code render} method call is guaranteed to have any effect previous calls
 * might be discarded at the discretion of the implementation. Therefore a
 * single instance of {@code AsyncRenderer} should not be used to render
 * multiple components.
 * <P>
 * A rendering request is defined by an {@link AsyncDataLink} and a
 * {@link DataRenderer} where the data link provides the data for the
 * {@code DataRenderer}. In what context the {@code AsyncRenderer} calls the
 * rendering methods is completely implementation dependent but usually it
 * should execute it in a context of a {@link org.jtrim2.concurrent.TaskExecutor}.
 * Note however that implementations are also allowed to execute rendering
 * synchronously (despite the name of this class). For example, executing
 * synchronously might be useful for debugging.
 * <P>
 * <B>Note on cancellation</B>: Rendering requests can be canceled but note
 * that canceling a request might also implicitly cause the cancellation of
 * every previous rendering requests. This is because the canceled request
 * might have caused the cancellation of previous requests (since rendering
 * requests may overwrite each other).
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be accessed
 * from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this listener are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see AsyncRendererFactory
 * @see GenericAsyncRendererFactory
 * @see org.jtrim2.swing.component.AsyncRenderingComponent
 *
 * @author Kelemen Attila
 */
public interface AsyncRenderer {
    /**
     * Submits a rendering requests to be done asynchronously.
     * <P>
     * First the {@link DataRenderer#startRendering(CancellationToken) startRendering}
     * method of the renderer will be called, then the data is requested from
     * the passed {@code AsyncDataLink} and the provided data is passed to the
     * {@link DataRenderer#render(CancellationToken, Object) render} method of
     * the renderer. Once each data has been retrieved by the data link or this
     * rendering request has been canceled (either by explicit cancellation or
     * by another request overwriting it)
     * {@link DataRenderer#finishRendering(CancellationToken, AsyncReport) finishRendering}
     * will be called. The {@code finishRendering} method is always called if
     * the {@code startRendering} method has been called.
     * <P>
     * Note that if another {@code render} method call overwrites this request,
     * this request might be completely ignored.
     *
     * @param <DataType> the type of the data retrieved by the passed data link
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   cancel this rendering request. This argument cannot be {@code null}.
     * @param dataLink the {@code AsyncDataLink} providing the data for the
     *   {@code render} method of the passed renderer. This argument can be
     *   {@code null}, in which case a data link is assumed which does not
     *   return any data but completes successfully immediately.
     * @param renderer the {@code DataRenderer} which is used to do the
     *   rendering. How and where this renderer renders depends completely
     *   on the passed {@code DataRenderer}. This argument cannot be
     *   {@code null}.
     * @return an object through which the caller might check the progress of
     *   this rendering request. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} or the {@code DataRenderer} is {@code null}
     */
    public <DataType> RenderingState render(
            CancellationToken cancelToken,
            AsyncDataLink<DataType> dataLink,
            DataRenderer<? super DataType> renderer);
}
