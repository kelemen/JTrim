package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.EnumSet;
import java.util.Set;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link ImageTransformer} scaling an image to fit the display.
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
public final class ZoomToFitTransformer implements ImageTransformer {
    /**
     * Returns the image transformations required to be applied to an image to
     * fit a display with the particular size. The transformation assumes that
     * (0, 0) offset means, that the center of the image is displayed at the
     * center of the display.
     * <P>
     * Note that {@code ZoomToFitTransformer} will use the transformation
     * returned by this method.
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
        ExceptionHelper.checkArgumentInRange(destWidth, 0, Integer.MAX_VALUE, "destWidth");
        ExceptionHelper.checkArgumentInRange(destHeight, 0, Integer.MAX_VALUE, "destHeight");
        ExceptionHelper.checkNotNullArgument(options, "options");
        ExceptionHelper.checkNotNullArgument(transBase, "transBase");

        if (srcWidth <= 0 || srcHeight <= 0) {
            return BasicImageTransformations.identityTransformation();
        }

        boolean magnify = options.contains(ZoomToFitOption.MAY_MAGNIFY);
        boolean keepAspectRatio = options.contains(ZoomToFitOption.KEEP_ASPECT_RATIO);
        boolean fitWidth = options.contains(ZoomToFitOption.FIT_WIDTH);
        boolean fitHeight = options.contains(ZoomToFitOption.FIT_HEIGHT);

        AffineTransform transf = new AffineTransform();
        transf.rotate(transBase.getRotateInRadians());

        double minX;
        double maxX;

        double minY;
        double maxY;

        Point2D srcPoint = new Point2D.Double(0.0, 0.0);
        Point2D destPoint = new Point2D.Double();

        // upper left corner
        destPoint = transf.transform(srcPoint, destPoint);
        minX = destPoint.getX();
        maxX = destPoint.getX();

        minY = destPoint.getY();
        maxY = destPoint.getY();

        // upper right corner
        srcPoint.setLocation((double)srcWidth, 0.0);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = minX > destPoint.getX() ? destPoint.getX() : minX;
        maxX = maxX < destPoint.getX() ? destPoint.getX() : maxX;

        minY = minY > destPoint.getY() ? destPoint.getY() : minY;
        maxY = maxY < destPoint.getY() ? destPoint.getY() : maxY;

        // lower left corner
        srcPoint.setLocation(0.0, (double)srcHeight);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = minX > destPoint.getX() ? destPoint.getX() : minX;
        maxX = maxX < destPoint.getX() ? destPoint.getX() : maxX;

        minY = minY > destPoint.getY() ? destPoint.getY() : minY;
        maxY = maxY < destPoint.getY() ? destPoint.getY() : maxY;

        // lower left corner
        srcPoint.setLocation((double)srcWidth, (double)srcHeight);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = minX > destPoint.getX() ? destPoint.getX() : minX;
        maxX = maxX < destPoint.getX() ? destPoint.getX() : maxX;

        minY = minY > destPoint.getY() ? destPoint.getY() : minY;
        maxY = maxY < destPoint.getY() ? destPoint.getY() : maxY;

        double dx = maxX - minX;
        double dy = maxY - minY;

        double zoomX;
        double zoomY;

        if (keepAspectRatio) {
            double zoom;
            zoomX = (double)destWidth / dx;
            zoomY = (double)destHeight / dy;

            if (fitWidth && fitHeight) {
                zoom = Math.min(zoomX, zoomY);
            }
            else if (fitWidth) {
                zoom = zoomX;
            }
            else if (fitHeight) {
                zoom = zoomY;
            }
            else {
                zoom = 1.0;
            }

            if (!magnify && zoom > 1.0) {
                zoom = 1.0;
            }

            zoomX = zoom;
            zoomY = zoom;
        }
        else {

            boolean normalRotate = true;
            boolean rotate90 = false;

            double baseRotate = transBase.getRotateInRadians();

            if (baseRotate == Math.PI / 2.0 || baseRotate == 3 * Math.PI / 2) {
                rotate90 = true;
            }
            else if (baseRotate != 0.0 && baseRotate != Math.PI) {
                normalRotate = false;
            }

            zoomX = (double)destWidth / dx;
            zoomY = (double)destHeight / dy;

            boolean scaleX = (!normalRotate || fitWidth);
            boolean scaleY = (!normalRotate || fitHeight);

            if (!scaleX && (zoomX < 1.0 || !magnify)) {
                zoomX = 1.0;
            }

            if (!scaleY && (zoomY < 1.0 || !magnify)) {
                zoomY = 1.0;
            }

            if (rotate90) {
                double tmpZoom = zoomX;
                zoomX = zoomY;
                zoomY = tmpZoom;
            }

            if (!normalRotate) {
                double zoom = Math.min(zoomX, zoomY);
                zoomX = zoom;
                zoomY = zoom;
            }

            if (!magnify && zoomX >= 1.0 && zoomY >= 1.0) {
                zoomX = 1.0;
                zoomY = 1.0;
            }
        }

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder(transBase);
        result.setOffset(0.0, 0.0);
        result.setZoomX(zoomX);
        result.setZoomY(zoomY);

        return result.create();
    }

    private final BasicImageTransformations transBase;
    private final Set<ZoomToFitOption> options;
    private final Color bckgColor;
    private final InterpolationType interpolationType;

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
        ExceptionHelper.checkNotNullArgument(transBase, "transBase");
        ExceptionHelper.checkNotNullArgument(options, "options");
        ExceptionHelper.checkNotNullArgument(bckgColor, "bckgColor");
        ExceptionHelper.checkNotNullArgument(interpolationType, "interpolationType");

        this.transBase = transBase;
        this.options = EnumSet.copyOf(options);
        this.bckgColor = bckgColor;
        this.interpolationType = interpolationType;

        ExceptionHelper.checkNotNullElements(this.options, "options");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TransformedImage convertData(ImageTransformerData data) {
        BasicImageTransformations transformations;
        transformations = ZoomToFitTransformer.getBasicTransformations(data.getSrcWidth(),
                data.getSrcHeight(),
                data.getDestWidth(),
                data.getDestHeight(),
                options,
                transBase);

        ImageTransformer transformer;
        if (AffineImageTransformer.isSimpleTransformation(transformations)) {
            transformer = new AffineImageTransformer(transformations,
                    bckgColor, InterpolationType.NEAREST_NEIGHBOR);
        }
        else {
            transformer = new AffineImageTransformer(transformations,
                    bckgColor, interpolationType);
        }
        return transformer.convertData(data);
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
        return "ZoomToFit " + options
                + " use interpolation " + interpolationType;
    }
}
