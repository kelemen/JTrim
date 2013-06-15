package org.jtrim.image.transform;

import java.awt.Color;
import java.util.Set;
import org.jtrim.cancel.Cancellation;

/**
 * @deprecated Use {@link ZoomToFitTransformationStep} instead.
 *
 * Defines an {@link ImageTransformer} scaling an image to fit the display.
 * <P>
 * <B>Special cases</B>:
 * <ul>
 *  <li>
 *   If neither {@code FIT_WIDTH}, nor {@code FIT_HEIGHT} is specified, then the
 *   image is not scaled (i.e., the applied {@code zoom} property is 1.0).
 *  </li>
 *  <li>
 *   If exactly one of {@code FIT_WIDTH} and {@code FIT_HEIGHT} is specified,
 *   then the aspect ratio is honored unless the absence of {@code MAY_MAGNIFY}
 *   requires that the applied {@code zoomX} or {@code zoomY} must not exceed
 *   1.0.
 *  </li>
 *  <li>
 *   If {@code MAY_MAGNIFY} is not specified, then neither the applied
 *   {@code zoomX}, nor the {@code zoomY} can be greater than 1.0. They will
 *   be reduced accordingly (if {@code KEEP_ASPECT_RATIO} is not set, then only
 *   the offending one will be reduced to 1.0).
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> and
 * calling them while holding a lock should be avoided.
 *
 * @see #getBasicTransformations(int, int, int, int, Set, BasicImageTransformations) getBasicTransformations
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class ZoomToFitTransformer implements ImageTransformer {
    /**
     * Returns the image transformations required to be applied to an image to
     * fit a display with the particular size. The transformation assumes that
     * (0, 0) offset means, that the center of the image is displayed at the
     * center of the display.
     * <P>
     * <B>Special cases</B>: See the class definition for special cases of the
     * zoom to fit options.
     * <P>
     * Note that {@code ZoomToFitTransformer} will use the transformation
     * returned by this method, so you may use this method to check what
     * transformation would the transformation use.
     *
     * @param srcWidth the width of the image to be scaled to fit the display.
     *   If this argument is less or equal to zero, an identity transformation
     *   is returned.
     * @param srcHeight the height of the image to be scaled to fit the display.
     *   If this argument is less or equal to zero, an identity transformation
     *   is returned.
     * @param destWidth the width of display, the source image needs to fit.
     *   This argument must be greater than or equal to zero.
     * @param destHeight the height of display, the source image needs to fit.
     *   This argument must be greater than or equal to zero.
     * @param options the rules to be applied for scaling the image. This
     *   argument cannot be {@code null}.
     * @param transBase the additional transformations to be applied to the
     *   source image. The scaling and offset property of this
     *   {@code BasicImageTransformations} are ignored. This argument cannot be
     *   {@code null}.
     * @return the image transformations required to be applied to an image to
     *   fit a display with the particular size. This method never returns
     *   {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified destination
     *   width or height is less than zero
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static BasicImageTransformations getBasicTransformations(
            int srcWidth, int srcHeight, int destWidth, int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase) {
        return ZoomToFitTransformationStep.getBasicTransformations(
                srcWidth, srcHeight, destWidth, destHeight, options, transBase);
    }

    private final ZoomToFitTransformationStep implementation;

    /**
     * Creates a new {@code ZoomToFitTransformer} with the specified
     * properties.
     *
     * @param transBase the additional transformations to be applied to the
     *   source image. The scaling and offset property of this
     *   {@code BasicImageTransformations} are ignored. This argument cannot be
     *   {@code null}.
     * @param options the rules to be applied for scaling the image. This
     *   argument and its elements cannot be {@code null}. The content of this
     *   set is copied and no reference to the set will be kept by the newly
     *   created instance.
     * @param bckgColor the {@code Color} to set the pixels of the destination
     *   image to where no pixels of the source image are transformed. This
     *   argument cannot be {@code null}.
     * @param interpolationType the interpolation algorithm to be used when
     *   transforming the source image. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public ZoomToFitTransformer(BasicImageTransformations transBase,
            Set<ZoomToFitOption> options, Color bckgColor,
            InterpolationType interpolationType) {
        this.implementation = new ZoomToFitTransformationStep(
                transBase, options, bckgColor, interpolationType);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TransformedImage convertData(ImageTransformerData data) {
        TransformationStepInput input = new TransformationStepInput(
                null,
                data.getDestWidth(),
                data.getDestHeight(),
                new TransformedImage(data.getSourceImage(), null));
        return implementation.render(Cancellation.UNCANCELABLE_TOKEN, input, null);
    }

    /**
     * Returns the string representation of this transformation in no
     * particular format
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return implementation.toString();
    }
}
