package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import org.jtrim.image.ImageData;
import org.jtrim.utils.ExceptionHelper;

/**
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
     *   Rotates the points of the image around (0, 0).
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
        affineTransf.rotate(-transformations.getRotateInRadians());
        if (transformations.isFlipHorizontal()) affineTransf.scale(-1.0, 1.0);
        if (transformations.isFlipVertical()) affineTransf.scale(1.0, -1.0);
        affineTransf.scale(transformations.getZoomX(), transformations.getZoomY());

        return affineTransf;
    }

    private static AffineTransform getTransformationMatrix(
            AffineTransform transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        double srcAnchorX = (srcWidth - 1.0) * 0.5;
        double srcAnchorY = (srcHeight - 1.0) * 0.5;

        double destAnchorX = (destWidth - 1.0) * 0.5;
        double destAnchorY = (destHeight - 1.0) * 0.5;

        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(destAnchorX, destAnchorY);
        affineTransf.concatenate(transformations);
        affineTransf.translate(-srcAnchorX, -srcAnchorY);

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

    private static AffineTransform getTransformationMatrix(
            AffineTransform transformations,
            ImageTransformerData input) {

        BufferedImage srcImage = input.getSourceImage();
        if (srcImage == null) {
            return new AffineTransform();
        }

        return getTransformationMatrix(transformations,
                srcImage.getWidth(), srcImage.getHeight(),
                input.getDestWidth(), input.getDestHeight());
    }


    /**
     * Creates an affine transformation from the given
     * {@link BasicImageTransformations} object assuming the source and
     * destination image sizes specified in the given
     * {@link ImageTransformerData}.
     * <P>
     * In case the specified {@code ImageTransformerData} does not contain a
     * source image, this method returns an identity transformation.
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

        return (isAbsOne(transformation.getZoomX()) &&
                isAbsOne(transformation.getZoomY()) &&
                (radRotate == BasicImageTransformations.RAD_0 ||
                radRotate == BasicImageTransformations.RAD_90 ||
                radRotate == BasicImageTransformations.RAD_180 ||
                radRotate == BasicImageTransformations.RAD_270));
    }

    private final AffineTransform transformations;
    private final Color bckgColor;
    private final int interpolationType;

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
        ExceptionHelper.checkNotNullArgument(transformations, "transformations");
        ExceptionHelper.checkNotNullArgument(bckgColor, "bckgColor");
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        this.transformations = new AffineTransform(transformations);
        this.bckgColor = bckgColor;

        switch (interpolationType) {
            case BILINEAR:
                this.interpolationType = AffineTransformOp.TYPE_BILINEAR;
                break;
            case BICUBIC:
                this.interpolationType = AffineTransformOp.TYPE_BICUBIC;
                break;
            default:
                this.interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
                break;
        }
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
                g.drawImage(srcImage, new AffineTransformOp(affineTransf, interpolationType), 0, 0);
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
    public TransformedImage convertData(ImageTransformerData data) {
        BufferedImage srcImage = data.getSourceImage();
        if (srcImage == null) {
            return new TransformedImage(null, null);
        }

        AffineTransform affineTransf = getTransformationMatrix(transformations, data);

        BufferedImage drawingSurface;

        drawingSurface = ImageData.createCompatibleBuffer(
                srcImage, data.getDestWidth(), data.getDestHeight());

        Graphics2D g = drawingSurface.createGraphics();
        try {
            transformImageTo(srcImage, affineTransf, drawingSurface, g);
        } finally {
            g.dispose();
        }


        return new TransformedImage(drawingSurface, new AffineImagePointTransformer(affineTransf));
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
