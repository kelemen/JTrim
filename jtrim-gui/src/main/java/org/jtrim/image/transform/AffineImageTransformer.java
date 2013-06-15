package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.jtrim.cancel.Cancellation;

/**
 * @deprecated Use {@link AffineTransformationStep} instead.
 *
 * Defines an {@link ImageTransformer} transforming an image based on an affine
 * transformation.
 * <P>
 * This {@code AffineImageTransformer} defines the
 * {@link BasicImageTransformations.Builder#setOffset(double, double) offset}
 * so, that (0, 0) offset means that the center of the source image will be
 * transformed to the center of the destination image.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> and
 * calling them while holding a lock should be avoided.
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class AffineImageTransformer implements ImageTransformer {
    /**
     * Creates an affine transformation from the given
     * {@code BasicImageTransformations}.
     * <P>
     * This method will assume that the top-left corner of the image is at
     * (0, 0) and the bottom-right corner of the image is at the
     * (width - 1, height - 1) coordinates. The transformations will be applied
     * in the following order:
     * <ol>
     *  <li>
     *   Scaling: Multiplies the coordinates with
     *   {@link BasicImageTransformations#getZoomX() ZoomX} and
     *   {@link BasicImageTransformations#getZoomY() ZoomY} appropriately.
     *  </li>
     *  <li>
     *   Flips the image if specified so. Vertical flip is equivalent to
     *   multiplying the Y coordinate with -1, while horizontal flipping is
     *   equivalent to multiplying the X coordinate with -1.
     *  </li>
     *  <li>
     *   Rotates the points of the image around (0, 0). Counterclockwise,
     *   assuming that the x axis oriented from left to right, and the y axis
     *   is oriented from bottom to top. Notice that this is different from the
     *   usual display of images.
     *  </li>
     *  <li>
     *   Adds the specified offsets to the appropriate coordinates.
     *  </li>
     * </ol>
     * Notice that if you translate the image (before applying the returned
     * transformation) so, that the center of the image will be at (0, 0) the
     * center of the image will be at the offset of the specified
     * transformation.
     *
     * @param transformations the {@code BasicImageTransformations} from which
     *   the affine transformations are to be calculated. This argument cannot
     *   be {@code null}.
     * @return the affine transformation created from the given
     *   {@code BasicImageTransformations}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public static AffineTransform getTransformationMatrix(BasicImageTransformations transformations) {
        return AffineTransformationStep.getTransformationMatrix(transformations);
    }

    /**
     * Creates an affine transformation from the given
     * {@link BasicImageTransformations} object assuming the specified source
     * and destination image sizes.
     * <P>
     * The {@link BasicImageTransformations.Builder#setOffset(double, double) offset}
     * is defined so, that (0, 0) offset means that the center of the source
     * image will be transformed to the center of the destination image.
     *
     * @param transformations the {@code BasicImageTransformations} to be
     *   applied to the image. This argument cannot be {@code null}.
     * @param srcWidth the assumed width of the source image in pixels
     * @param srcHeight the assumed height of the source image in pixels
     * @param destWidth the assumed width of the destination image in pixels
     * @param destHeight the assumed height of the destination image in pixels
     * @return the affine transformation from the given
     *   {@link BasicImageTransformations} object assuming the specified source
     *   and destination image sizes. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code BasicImageTransformations} is {@code null}
     */
    public static AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        return AffineTransformationStep.getTransformationMatrix(
                transformations, srcWidth, srcHeight, destWidth, destHeight);
    }

    private static AffineTransform getTransformationMatrix(
            AffineTransform transformations,
            ImageTransformerData input) {
        int srcWidth = input.getImageWidth();
        int srcHeight = input.getImageHeight();
        if (srcWidth < 0 || srcHeight < 0) {
            return new AffineTransform();
        }

        return AffineTransformationStep.getTransformationMatrix(
                transformations,
                srcWidth,
                srcHeight,
                input.getDestWidth(),
                input.getDestHeight());
    }


    /**
     * Creates an affine transformation from the given
     * {@link BasicImageTransformations} object assuming the source and
     * destination image sizes specified in the given
     * {@link ImageTransformerData}.
     * <P>
     * In case the specified {@code ImageTransformerData} does not contain the
     * width or height of the source image (either by having a source image or
     * meta-data), this method returns an identity transformation.
     * <P>
     * The {@link BasicImageTransformations.Builder#setOffset(double, double) offset}
     * is defined so, that (0, 0) offset means that the center of the source
     * image will be transformed to the center of the destination image.
     *
     * @param transformations the {@code BasicImageTransformations} to be
     *   applied to the image. This argument cannot be {@code null}.
     * @param input the {@code ImageTransformerData} from which the assumed
     *   width and height of the source and destination image is to be
     *   extracted. This method cannot be {@code null}.
     * @return the  affine transformation from the given
     *   {@link BasicImageTransformations} object assuming the source and
     *   destination image sizes specified in the given
     *   {@link ImageTransformerData}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            ImageTransformerData input) {

        return getTransformationMatrix(getTransformationMatrix(transformations), input);
    }

    /**
     * Returns {@code true} if for the given transformation, the nearest
     * neighbor interpolation should be considered optimal.
     *
     * @param transformation the {@code BasicImageTransformations} to be
     *   checked. This argument cannot be {@code null}.
     * @return {@code true} if for the given transformation, the nearest
     *   neighbor interpolation should be considered optimal, {@code false} if
     *   other interpolations may produce better results
     */
    public static boolean isSimpleTransformation(BasicImageTransformations transformation) {
        return AffineTransformationStep.isSimpleTransformation(transformation);
    }

    private final AffineTransformationStep implementation;

    /**
     * Creates a new {@code AffineImageTransformer} based on the specified
     * {@code BasicImageTransformations}.
     *
     * @param transformations the {@code BasicImageTransformations} to be
     *   applied to source images. This argument cannot be {@code null}.
     * @param bckgColor the {@code Color} to set the pixels of the destination
     *   image to where no pixels of the source image are transformed. This
     *   argument cannot be {@code null}.
     * @param interpolationType the interpolation algorithm to be used when
     *   transforming the source image. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public AffineImageTransformer(BasicImageTransformations transformations,
            Color bckgColor, InterpolationType interpolationType) {
        this(getTransformationMatrix(transformations), bckgColor, interpolationType);
    }

    /**
     * Creates a new {@code AffineImageTransformer} based on the specified
     * {@code AffineTransform}.
     *
     * @param transformations the {@code BasicImageTransformations} to be
     *   applied to source images. This argument cannot be {@code null}.
     * @param bckgColor the {@code Color} to set the pixels of the destination
     *   image to where no pixels of the source image are transformed. This
     *   argument cannot be {@code null}.
     * @param interpolationType the interpolation algorithm to be used when
     *   transforming the source image. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public AffineImageTransformer(AffineTransform transformations,
            Color bckgColor, InterpolationType interpolationType) {
        this.implementation = new AffineTransformationStep(transformations, bckgColor, interpolationType);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TransformedImage convertData(ImageTransformerData data) {
        BufferedImage sourceImage = data.getSourceImage();
        if (sourceImage == null) {
            AffineTransform affineTransf = getTransformationMatrix(implementation.getTransformations(), data);
            return new TransformedImage(null, new AffineImagePointTransformer(affineTransf));
        }

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
