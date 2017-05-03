package org.jtrim2.swing.component;

import java.awt.image.BufferedImage;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.async.AsyncReport;

/**
 * Defines a rendering tasks which draws its result to a {@code BufferedImage}.
 * This interface differs from {@link org.jtrim2.swing.concurrent.async.DataRenderer}
 * only in that this interface is less general and must render to a
 * {@code BufferedImage}.
 * <P>
 * Instances of a {@code ImageRenderer} are need to be used in the following
 * ways:
 * <ol>
 *  <li>
 *   Before actually calling any of the methods of {@code ImageRenderer}, the
 *   {@link #startRendering(CancellationToken, BufferedImage) startRendering}
 *   method need to be called. That is, none of the other methods defined by the
 *   {@code ImageRenderer} interface can be called before calling
 *   {@code startRendering}.
 *  </li>
 *  <li>
 *   After {@code startRendering} has been called, the
 *   {@link #render(CancellationToken, Object, BufferedImage) render} method can
 *   be called multiple times (zero or more) passing more and more accurate data
 *   to it with each invocation. The data is intended to be provided by an
 *   {@link org.jtrim2.concurrent.async.AsyncDataLink} but this is not strictly
 *   necessary (although an {@code AsyncDataLink} can model each valid uses).
 *  </li>
 *  <li>
 *   Once no more data is available the
 *   {@link #finishRendering(CancellationToken, AsyncReport, BufferedImage) finishRendering}
 *   method of the {@code ImageRenderer}. must be called. The
 *   {@code finishRendering} method is mandatory to be called if
 *   {@code startRendering} has been called previously.
 *  </li>
 * </ol>
 * Note that the {@code startRendering}, {@code render} and
 * {@code finishRendering} methods are all allowed to do some rendering and
 * each invocation to these methods expected to overwrite the previously done
 * rendering by any of these methods. Multiple invocations to {@code render}
 * need to overwrite each other's result as well.
 * <P>
 * <B>Significant rendering</B>: Each method of this interface which is allowed
 * to do some rendering may do a significant, insignificant rendering or no
 * rendering at all. If a significant rendering was done by a
 * {@code ImageRenderer}, it means that this {@code ImageRenderer} might be
 * canceled if there is another {@code ImageRenderer} which would overwrite its
 * results. This property is required because if {@code ImageRenderer} would be
 * canceled without regard if it has done a significant rendering or not and new
 * {@code ImageRenderer} instances were supplied rapidly overwriting its result:
 * It would be possible that no significant rendering is done for a significant
 * amount of time. When painting a component this would manifest as the
 * component does not get repainted until the {@code ImageRenderer} instances
 * are no longer provided rapidly. <B>Note</B>: Once any of the rendering
 * methods of this interface returns that it has done a significant rendering,
 * subsequent rendering method invocations must also be significant or do no
 * rendering at all.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are not required to be safe to be accessed
 * from multiple threads concurrently. In fact, the {@code startRendering},
 * {@code render} and {@code finishRendering} methods must never be called
 * concurrently because their result must overwrite each other's results
 * and if they were called concurrently it could not be determined which call
 * needs to overwrite the results of which call.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <DataType> the type of the data passed to the {@code render} method
 * @param <ResultType> the type of the object returned by one of the rendering
 *   methods when they did some rendering (even if insignificant)
 *
 * @see AsyncRenderingComponent
 *
 * @author Kelemen Attila
 */
public interface ImageRenderer<DataType, ResultType> {
    /**
     * This method is invoked before calling any of the other methods of this
     * interface. This method may also do some initial rendering if it can.
     * <P>
     * <B>Note</B>: Once this method has been called, it is mandatory to call
     * the {@code finishRendering} method of this {@code ImageRenderer} before
     * it becomes eligible for garbage collection.
     * <P>
     * This method should not throw an exception but if it does, the rendering
     * process should be continued as if this method returned {@code false}.
     *
     * @param cancelToken the {@code CancellationToken} signaling cancellation
     *   a request when the rendering request has been canceled. Upon
     *   cancellation this method may simply return
     *   {@code RenderingResult.noRendering()} (even if it modified the passed
     *   {@code BufferedImage}) or throw an {@link org.jtrim2.cancel.OperationCanceledException}
     *   or even continue as if nothing happened. This argument cannot be
     *   {@code null}.
     * @param drawingSurface the {@code BufferedImage} to which this method
     *   needs to draw to if it renders anything. This argument cannot be
     *   {@code null}.
     *
     * @return the {@code RenderingResult} which determines if this method
     *   did any rendering or not and an attached arbitrary object if this
     *   method did any rendering. This method may never return {@code null},
     *   if it does no rendering return {@link RenderingResult#noRendering()}.
     *   Even if this method returns that it did no rendering, it is allowed
     *   to modify the image but those modifications are to be discarded.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException implementation may
     *   throw this exception if they detect a cancellation request. They are
     *   not required to do so, they may even return
     *   {@code RenderingResult.noRendering()} or even ignore the cancellation
     *   request.
     */
    public RenderingResult<ResultType> startRendering(
            CancellationToken cancelToken, BufferedImage drawingSurface);

    /**
     * This method might be optionally called to determine if a subsequent
     * invocation to the {@code render} method will be significant or not.
     * <P>
     * That is, in case this method returns {@code true} for a particular data,
     * it promises that a subsequent call to the {@code render} method (of the
     * same {@code ImageRenderer}) will do a
     * {@link RenderingResult#isSignificant() significant rendering} if the same
     * data is passed to the {@code render} method. Returning {@code false} if
     * simply the refusal of making such commitment. <B>Note</B>: When
     * cancellation is requested, implementations are allowed to break this
     * promise and the {@code render} method may return as it pleases.
     * <P>
     * Notice that returning {@code false} is always safe, it does not promises
     * that {@code render} method will not do a significant rendering.
     * <P>
     * Unlike other methods of this interface this method must be very quick to
     * return and if this method cannot determine what the {@code render} method
     * will do, it must simply return {@code false} rather than doing some
     * expensive test. For example, this method might check the existence of a
     * property in the passed data but may not analyze the pixels of an image
     * (if the data contains an image).
     * <P>
     * This method is provided for performance reasons only, so that callers
     * may be able to more eagerly cancel the rendering process if required.
     * However, it is always safe for this method to simply return
     * {@code false}.
     *
     * @param data the data object to be checked if passing this data to
     *   the {@code render} method will cause a significant rendering. This
     *   argument can be {@code null} if the provider of the data can provide
     *   {@code null} objects.
     *
     * @return {@code true} if a subsequent invocation to the {@code render}
     *   method with the same data will cause a significant rendering,
     *   {@code false} otherwise
     */
    public boolean willDoSignificantRender(DataType data);

    /**
     * This method may be called multiple times to do the implementation
     * defined rendering. When this method is called multiple times it will be
     * done so with a more and more accurate data and each invocation is
     * expected to overwrite the results of the previous rendering. This method
     * is not called if there is no data available.
     * <P>
     * This method should not throw an exception but if it does, the rendering
     * process should be continued as if this method returned
     * {@link RenderingResult#noRendering()}.
     *
     * @param data the data object which is the input of the rendering process.
     *   This argument can be {@code null} if the provider of the data can
     *   provide {@code null} objects.
     * @param cancelToken the {@code CancellationToken} signaling cancellation
     *   a request when the rendering request has been canceled. Upon
     *   cancellation this method may simply return
     *   {@code RenderingResult.noRendering()} (even if it modified the passed
     *   {@code BufferedImage}) or throw an {@link org.jtrim2.cancel.OperationCanceledException}
     *   or even continue as if nothing happened. This argument cannot be
     *   {@code null}.
     * @param drawingSurface the {@code BufferedImage} to which this method
     *   needs to draw to if it renders anything. This argument cannot be
     *   {@code null}.
     *
     * @return the {@code RenderingResult} which determines if this method
     *   did any rendering or not and an attached arbitrary object if this
     *   method did any rendering. This method may never return {@code null},
     *   if it does no rendering return {@link RenderingResult#noRendering()}.
     *   Even if this method returns that it did no rendering, it is allowed
     *   to modify the image but those modifications are to be discarded.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException implementation may
     *   throw this exception if they detect a cancellation request. They are
     *   not required to do so, they may even return
     *   {@code RenderingResult.noRendering()} or even ignore the cancellation
     *   request.
     */
    public RenderingResult<ResultType> render(
            CancellationToken cancelToken, DataType data, BufferedImage drawingSurface);

    /**
     * This method is called to allow for a final rendering after no more
     * {@code render} call will be done. This method must be called if
     * {@code startRendering} is called on this {@code ImageRenderer}. Also,
     * this method may not be called more than once.
     * <P>
     * Note that in general it is not allowed to restart a {@code ImageRenderer},
     * once {@code finishRendering} has been called, this {@code ImageRenderer}
     * object is no longer useful regarding rendering.
     *
     * @param report the {@code AsyncReport} object defining how the rendering
     *   has been completed (the same as with the
     *   {@link org.jtrim2.concurrent.async.AsyncDataListener AsyncDataListener}).
     *   This argument cannot be {@code null}.
     * @param cancelToken the {@code CancellationToken} signaling cancellation
     *   a request when the rendering request has been canceled. Upon
     *   cancellation this method may simply return
     *   {@code RenderingResult.noRendering()} (even if it modified the passed
     *   {@code BufferedImage}) or throw an {@link org.jtrim2.cancel.OperationCanceledException}
     *   or even continue as if nothing happened. This argument cannot be
     *   {@code null}.
     * @param drawingSurface the {@code BufferedImage} to which this method
     *   needs to draw to if it renders anything. This argument cannot be
     *   {@code null}.
     * @return the {@code RenderingResult} which determines if this method
     *   did any rendering or not and an attached arbitrary object if this
     *   method did any rendering. This method may never return {@code null},
     *   if it does no rendering return {@link RenderingResult#noRendering()}.
     *   Even if this method returns that it did no rendering, it is allowed
     *   to modify the image but those modifications are to be discarded.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException implementation may
     *   throw this exception if they detect a cancellation request. They are
     *   not required to do so, they may even return
     *   {@code RenderingResult.noRendering()} or even ignore the cancellation
     *   request.
     */
    public RenderingResult<ResultType> finishRendering(
            CancellationToken cancelToken, AsyncReport report, BufferedImage drawingSurface);
}
