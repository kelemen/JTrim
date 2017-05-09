package org.jtrim2.image.transform;

import java.awt.image.BufferedImage;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an element of a series of transformation applied to an image.
 * <P>
 * Note: If you want to cache the result of this transformation step, then you
 * should use the
 * {@link TransformationSteps#cachedStep(org.jtrim2.cache.ReferenceType, ImageTransformationStep, TransformationStepInput.Cmp) TransformationSteps.cachedStep}
 * method, instead of implementing your own caching mechanism in the actual
 * {@code ImageTransformationStep}.
 *
 * <h3>Thread safety</h3>
 * Methods of this interface does not need to be safe to be accessed by multiple
 * threads concurrently. Therefore, they may cache values between
 * {@link #render(CancellationToken, TransformationStepInput, BufferedImage) render}
 * method calls.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>. However, they must be expected to be
 * called from any thread (not only from the EDT).
 *
 * @see TransformationSteps#cachedStep(org.jtrim2.cache.ReferenceType, ImageTransformationStep, TransformationStepInput.Cmp) TransformationSteps.cachedStep
 */
public interface ImageTransformationStep {
    /**
     * Applies the rendering defined by this {@code ImageTransformationStep}.
     * This method should usually apply the transformation on the image produced
     * by the previous transformation step (i.e.:
     * {@link TransformationStepInput#getInputImage() input.getInputImage()}).
     * <P>
     * If this transformation is the first in the sequence, then the input for
     * this transformation is the same as the source image.
     * <P>
     * Callers of this method may provide a {@code BufferedImage} which this
     * method may use to render its output to. Implementations are free to
     * ignore the offered {@code BufferedImage} but for best performance, they
     * should use it instead of creating a new equivalent buffer. Note however,
     * that they must not retain a reference themselves to this buffer after the
     * {@code render} method returns.
     *
     * @param cancelToken the {@code CancellationToken} which might signal, that
     *   the result of this rendering is no longer needed. If this rendering
     *   process can take for more than a few hundred milliseconds,
     *   implementations are recommended to check for cancellation. This
     *   argument cannot be {@code null}.
     * @param input the input for this transformation. If you want to further
     *   transform the output of the previous transformation, then you should
     *   use {@code input.getInputImage()}. This argument cannot be
     *   {@code null}.
     * @param offeredBuffer the {@code BufferedImage} which implementation might
     *   use to render their output to. This method is free to ignore this
     *   argument. If implementation chooses to render its output to this
     *   {@code BufferedImage}, it should also return {@code offeredBuffer} in
     *   its result. This argument can be {@code null}, in which case
     *   implementations are forced to create a new {@code BufferedImage} as
     *   needed.
     * @return the output of the transformation which will be the input for
     *   the next transformation; or if there is no more transformation, the
     *   actual output of the rendering chain. This method may never return
     *   {@code null}. The {@link TransformedImage#getPointTransformer() pointTransformer}
     *   property of the result must be relative to the result of the previous
     *   transformation step and not to the original source image.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException maybe thrown in
     *   response to a cancellation request
     */
    public TransformedImage render(
            CancellationToken cancelToken,
            TransformationStepInput input,
            BufferedImage offeredBuffer);
}
