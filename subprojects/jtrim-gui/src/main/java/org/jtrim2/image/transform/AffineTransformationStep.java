package org.jtrim2.image.transform;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.image.BufferedImages;

/**
 * Defines an {@link ImageTransformationStep} transforming an image based on an
 * affine transformation.
 * <P>
 * This {@code AffineTransformationStep} defines the
 * {@link BasicImageTransformations.Builder#setOffset(double, double) offset}
 * so, that (0, 0) offset means that the center of the source image will be
 * transformed to the center of the destination image.
 * <P>
 * If you don't already have the image but know its dimension, you may use
 * one of the declared static methods (taking both the size of the input image
 * and the size of the destination) to calculate the coordinate transformation
 * applied by this transformation.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> and
 * calling them while holding a lock should be avoided.
 *
 * @see #getTransformationMatrix(AffineTransform, double, double, double, double)
 * @see #getTransformationMatrix(BasicImageTransformations, double, double, double, double)
 *
 * @author Kelemen Attila
 */
public final class AffineTransformationStep implements ImageTransformationStep {
    private final AffineTransform transformations;
    private final Color bckgColor;
    private final InterpolationType interpolationType;
    private final int rawInterpolationType;

    /**
     * Creates a new {@code AffineTransformationStep} based on the specified
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
    public AffineTransformationStep(
            BasicImageTransformations transformations,
            Color bckgColor,
            InterpolationType interpolationType) {
        this(getTransformationMatrix(transformations), bckgColor, interpolationType);
    }

    /**
     * Creates a new {@code AffineTransformationStep} based on the specified
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
    public AffineTransformationStep(
            AffineTransform transformations,
            Color bckgColor,
            InterpolationType interpolationType) {
        Objects.requireNonNull(transformations, "transformations");
        Objects.requireNonNull(bckgColor, "bckgColor");
        Objects.requireNonNull(interpolationType, "interpolationType");

        this.transformations = new AffineTransform(transformations);
        this.bckgColor = bckgColor;
        this.interpolationType = interpolationType;

        switch (interpolationType) {
            case BILINEAR:
                this.rawInterpolationType = AffineTransformOp.TYPE_BILINEAR;
                break;
            case BICUBIC:
                this.rawInterpolationType = AffineTransformOp.TYPE_BICUBIC;
                break;
            default:
                this.rawInterpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
                break;
        }
    }

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
        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(transformations.getOffsetX(), transformations.getOffsetY());
        affineTransf.rotate(transformations.getRotateInRadians());
        if (transformations.isFlipHorizontal()) affineTransf.scale(-1.0, 1.0);
        if (transformations.isFlipVertical()) affineTransf.scale(1.0, -1.0);
        affineTransf.scale(transformations.getZoomX(), transformations.getZoomY());

        return affineTransf;
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

        return getTransformationMatrix(
                getTransformationMatrix(transformations),
                srcWidth,
                srcHeight,
                destWidth,
                destHeight);
    }

    private static boolean isAbsOne(double value) {
        return value == 1.0 || value == -1.0;
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
    public static boolean isSimpleTransformation(
            BasicImageTransformations transformation) {

        double radRotate = transformation.getRotateInRadians();

        return (isAbsOne(transformation.getZoomX())
                && isAbsOne(transformation.getZoomY())
                &&
                (radRotate == BasicImageTransformations.RAD_0
                    || radRotate == BasicImageTransformations.RAD_90
                    || radRotate == BasicImageTransformations.RAD_180
                    || radRotate == BasicImageTransformations.RAD_270));
    }

    /**
     * Creates an affine transformation from the given
     * {@link AffineTransform} object assuming the specified source and
     * destination image sizes.
     * <P>
     * The {@link AffineTransform#translate(double, double) offset}
     * is defined so, that (0, 0) offset means that the center of the source
     * image will be transformed to the center of the destination image.
     *
     * @param transformations the {@code AffineTransform} to be applied to the
     *   image. This argument cannot be {@code null}.
     * @param srcWidth the assumed width of the source image in pixels
     * @param srcHeight the assumed height of the source image in pixels
     * @param destWidth the assumed width of the destination image in pixels
     * @param destHeight the assumed height of the destination image in pixels
     * @return the affine transformation from the given
     *   {@link BasicImageTransformations} object assuming the specified source
     *   and destination image sizes. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code AffineTransform} is {@code null}
     */
    public static AffineTransform getTransformationMatrix(
            AffineTransform transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        double srcAnchorX = srcWidth / 2.0;
        double srcAnchorY = srcHeight / 2.0;

        double destAnchorX = destWidth / 2.0;
        double destAnchorY = destHeight / 2.0;

        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(destAnchorX, destAnchorY);
        affineTransf.concatenate(transformations);
        affineTransf.translate(-srcAnchorX, -srcAnchorY);

        return affineTransf;
    }

    private static boolean isSourceVisible(BufferedImage src,
            BufferedImage dest, AffineTransform transf) {

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        int destWidth = dest.getWidth();
        int destHeight = dest.getHeight();

        Point2D destPoint = new Point2D.Double();

        transf.transform(new Point2D.Double(0, 0), destPoint);

        Polygon resultArea = new Polygon();

        transf.transform(new Point2D.Double(0, 0), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(0, srcHeight), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(srcWidth, srcHeight), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        transf.transform(new Point2D.Double(srcWidth, 0), destPoint);
        resultArea.addPoint((int)Math.round(destPoint.getX()), (int)Math.round(destPoint.getY()));

        return resultArea.intersects(0, 0, destWidth, destHeight);
    }

    private void transformImageTo(
            BufferedImage srcImage,
            AffineTransform affineTransf,
            BufferedImage drawingSurface, Graphics2D g) {

        int destWidth = drawingSurface.getWidth();
        int destHeight = drawingSurface.getHeight();

        g.setBackground(bckgColor);
        g.clearRect(0, 0, destWidth, destHeight);

        if (affineTransf.getDeterminant() != 0.0) {
            // This check is required because if the offset is too large
            // the drawImage seems to enter into an infinite loop.
            // This is possibly because of a floating point overflow.
            if (isSourceVisible(srcImage, drawingSurface, affineTransf)) {
                g.drawImage(srcImage, new AffineTransformOp(affineTransf, rawInterpolationType), 0, 0);
            }
        }
        else {
            // In case the determinant is zero, the transformation
            // transforms the image to a line or a point which means
            // that the result is not visible.
            // Note however that the image was already cleared.
            // g.clearRect(0, 0, destWidth, destHeight);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TransformedImage render(
            CancellationToken cancelToken,
            TransformationStepInput input,
            BufferedImage offeredBuffer) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(input, "input");

        BufferedImage inputImage = input.getInputImage().getImage();
        if (inputImage == null) {
            return TransformedImage.NULL_IMAGE;
        }

        int destWidth = input.getDestinationWidth();
        int destHeight = input.getDestinationHeight();
        if (destWidth <= 0 || destHeight <= 0) {
            return TransformedImage.NULL_IMAGE;
        }

        BufferedImage drawingSurface;
        if (offeredBuffer != null
                && offeredBuffer.getType() == inputImage.getType()
                && offeredBuffer.getWidth() == destWidth
                && offeredBuffer.getHeight() == destHeight) {
            drawingSurface = offeredBuffer;
        }
        else {
            drawingSurface = BufferedImages.createCompatibleBuffer(inputImage, destWidth, destHeight);
        }

        AffineTransform affineTransf = getTransformationMatrix(
                transformations,
                inputImage.getWidth(),
                inputImage.getHeight(),
                destWidth,
                destHeight);
        Graphics2D g = drawingSurface.createGraphics();
        try {
            transformImageTo(inputImage, affineTransf, drawingSurface, g);
        } finally {
            g.dispose();
        }

        return new TransformedImage(drawingSurface, new AffineImagePointTransformer(affineTransf));
    }

    /**
     * Returns the affine transformations specified at construction time. That
     * is, the affine transformation this {@code AffineTransformationStep}
     * applies to input images.
     *
     * @return the affine transformations specified at construction time. This
     *   method never returns {@code null} and modifying the returned
     *   transformation will have no effect on this
     *   {@code AffineTransformationStep}.
     */
    public AffineTransform getTransformations() {
        return new AffineTransform(transformations);
    }

    /**
     * Returns the background color specified at construction time. That is,
     * the color used to fill the images not covered by the transformed input
     * image.
     *
     * @return the background color specified at construction time. This method
     *   never returns {@code null}.
     */
    public Color getBackgroundColor() {
        return bckgColor;
    }

    /**
     * Returns the interpolation type specified at construction time. That is,
     * the type requested to be used when resampling the image when applying the
     * affine transformation.
     *
     * @return the interpolation type specified at construction time. This
     *   method never returns {@code null}.
     */
    public InterpolationType getInterpolationType() {
        return interpolationType;
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
        return "Affine transformation: " + transformations
                + "\nuse interpolation " + interpolationType;
    }
}
