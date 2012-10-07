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
 * transformation. Despite the name of this class, this implementation only
 * supports transformations specified by {@link BasicImageTransformations}.
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
    private static final double RAD_0;
    private static final double RAD_90;
    private static final double RAD_180;
    private static final double RAD_270;

    static {
        BasicImageTransformations.Builder radConvTest;
        radConvTest = new BasicImageTransformations.Builder();

        radConvTest.setRotateInDegrees(0);
        RAD_0 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(90);
        RAD_90 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(180);
        RAD_180 = radConvTest.getRotateInRadians();

        radConvTest.setRotateInDegrees(270);
        RAD_270 = radConvTest.getRotateInRadians();
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
     *
     * @throws NullPointerException thrown if the specified
     *   {@code BasicImageTransformations} is {@code null}
     */
    public static AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight) {

        double srcAnchorX = (srcWidth - 1.0) * 0.5;
        double srcAnchorY = (srcHeight - 1.0) * 0.5;

        double destAnchorX = (destWidth - 1.0) * 0.5;
        double destAnchorY = (destHeight - 1.0) * 0.5;

        AffineTransform affineTransf = new AffineTransform();
        affineTransf.translate(transformations.getOffsetX(), transformations.getOffsetY());
        affineTransf.translate(destAnchorX, destAnchorY);
        affineTransf.rotate(transformations.getRotateInRadians());
        if (transformations.isFlipHorizontal()) affineTransf.scale(-1.0, 1.0);
        if (transformations.isFlipVertical()) affineTransf.scale(1.0, -1.0);
        affineTransf.scale(transformations.getZoomX(), transformations.getZoomY());
        affineTransf.translate(-srcAnchorX, -srcAnchorY);

        return affineTransf;
    }

    /**
     * Creates an affine transformation from the given
     * {@link BasicImageTransformations} object assuming the source and
     * destination image sizes specified in the given
     * {@link ImageTransformerData}.
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

        BufferedImage srcImage = input.getSourceImage();
        if (srcImage == null) {
            return null;
        }

        return getTransformationMatrix(transformations,
                srcImage.getWidth(), srcImage.getHeight(),
                input.getDestWidth(), input.getDestHeight());
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

        return (transformation.getZoomX() == 1.0 &&
                transformation.getZoomY() == 1.0 &&
                (radRotate == RAD_0 ||
                radRotate == RAD_90 ||
                radRotate == RAD_180 ||
                radRotate == RAD_270));
    }

    private final BasicImageTransformations transformations;
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
        ExceptionHelper.checkNotNullArgument(transformations, "transformations");
        ExceptionHelper.checkNotNullArgument(bckgColor, "bckgColor");
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        this.transformations = transformations;
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
                try {
                    g.drawImage(srcImage, new AffineTransformOp(affineTransf, interpolationType), 0, 0);
                } catch (ImagingOpException ex) {
                    throw new ImageProcessingException(ex);
                }
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
