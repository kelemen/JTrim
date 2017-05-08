package org.jtrim2.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines an {@link ImageTransformationStep} scaling the
 * {@link TransformationStepInput#getInputImage() input image} to fit the
 * display.
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
 * <P>
 * <B>Note</B>: The transformation is applied to the input image (i.e.:
 * {@link TransformationStepInput#getInputImage()}.
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
public final class ZoomToFitTransformationStep
implements
        ImageTransformationStep {
    private final BasicImageTransformations transBase;
    private final Set<ZoomToFitOption> options;
    private final Color bckgColor;
    private final InterpolationType interpolationType;

    /**
     * Creates a new {@code ZoomToFitTransformationStep} with the specified
     * properties.
     *
     * @param transBase the additional transformations to be applied to the
     *   input image. The scaling and offset property of this
     *   {@code BasicImageTransformations} are ignored. This argument cannot be
     *   {@code null}.
     * @param options the rules to be applied for scaling the image. This
     *   argument and its elements cannot be {@code null}. The content of this
     *   set is copied and no reference to the set will be kept by the newly
     *   created instance.
     * @param bckgColor the {@code Color} to set the pixels of the destination
     *   image to where no pixels of the input image are transformed. This
     *   argument cannot be {@code null}.
     * @param interpolationType the interpolation algorithm to be used when
     *   transforming the input image. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public ZoomToFitTransformationStep(
            BasicImageTransformations transBase,
            Set<ZoomToFitOption> options,
            Color bckgColor,
            InterpolationType interpolationType) {
        Objects.requireNonNull(transBase, "transBase");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(bckgColor, "bckgColor");
        Objects.requireNonNull(interpolationType, "interpolationType");

        this.transBase = transBase;
        this.options = options.isEmpty()
                ? Collections.<ZoomToFitOption>emptySet()
                : EnumSet.copyOf(options);
        this.bckgColor = bckgColor;
        this.interpolationType = interpolationType;

        ExceptionHelper.checkNotNullElements(this.options, "options");
    }

    private static Point2D.Double getTransformedWidthAndHeight(
            AffineTransform transf, double srcWidth, double srcHeight) {

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
        srcPoint.setLocation(srcWidth, 0.0);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = Math.min(minX, destPoint.getX());
        maxX = Math.max(maxX, destPoint.getX());

        minY = Math.min(minY, destPoint.getY());
        maxY = Math.max(maxY, destPoint.getY());

        // lower left corner
        srcPoint.setLocation(0.0, srcHeight);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = Math.min(minX, destPoint.getX());
        maxX = Math.max(maxX, destPoint.getX());

        minY = Math.min(minY, destPoint.getY());
        maxY = Math.max(maxY, destPoint.getY());

        // lower left corner
        srcPoint.setLocation(srcWidth, srcHeight);
        destPoint = transf.transform(srcPoint, destPoint);
        minX = Math.min(minX, destPoint.getX());
        maxX = Math.max(maxX, destPoint.getX());

        minY = minY > destPoint.getY() ? destPoint.getY() : minY;
        maxY = Math.max(maxY, destPoint.getY());

        double dx = maxX - minX;
        double dy = maxY - minY;
        return new Point2D.Double(dx, dy);
    }

    private static RotateType getRotateType(double rad) {
        if (rad == BasicImageTransformations.RAD_0) {
            return RotateType.ROTATE_180;
        }
        if (rad == BasicImageTransformations.RAD_90) {
            return RotateType.ROTATE_90;
        }
        if (rad == BasicImageTransformations.RAD_180) {
            return RotateType.ROTATE_180;
        }
        if (rad == BasicImageTransformations.RAD_270) {
            return RotateType.ROTATE_90;
        }
        return RotateType.ROTATE_ARBITRARY;
    }

    private static void maximizeZoom(
            BasicImageTransformations.Builder transf,
            boolean keepAspectRatio,
            double maxZoomX,
            double maxZoomY) {
        double zoomX = transf.getZoomX();
        double zoomY = transf.getZoomY();

        if (zoomX >= maxZoomX || zoomY >= maxZoomY) {
            if (keepAspectRatio) {
                double maxZoom = Math.max(zoomX, zoomY);
                zoomX = zoomX / maxZoom;
                zoomY = zoomY / maxZoom;
            }
            else {
                if (zoomX > maxZoomX) zoomX = maxZoomX;
                if (zoomY > maxZoomY) zoomY = maxZoomY;
            }
        }

        transf.setZoomX(zoomX);
        transf.setZoomY(zoomY);
    }

    /**
     * Returns the image transformations required to be applied to an image to
     * fit a display with the particular size. The transformation assumes that
     * (0, 0) offset means, that the center of the image is displayed at the
     * center of the display.
     * <P>
     * <B>Special cases</B>: See the class definition for special cases of the
     * zoom to fit options.
     * <P>
     * Note that {@code ZoomToFitTransformationStep} will use the transformation
     * returned by this method, so you may use this method to check what
     * transformation would the transformation use.
     *
     * @param inputWidth the width of the image to be scaled to fit the display.
     *   If this argument is less or equal to zero, an identity transformation
     *   is returned.
     * @param inputHeight the height of the image to be scaled to fit the display.
     *   If this argument is less or equal to zero, an identity transformation
     *   is returned.
     * @param destWidth the width of display, the input image needs to fit.
     *   This argument must be greater than or equal to zero.
     * @param destHeight the height of display, the input image needs to fit.
     *   This argument must be greater than or equal to zero.
     * @param options the rules to be applied for scaling the image. This
     *   argument cannot be {@code null}.
     * @param transBase the additional transformations to be applied to the
     *   input image. The scaling and offset property of this
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
            int inputWidth, int inputHeight, int destWidth, int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase) {
        ExceptionHelper.checkArgumentInRange(destWidth, 0, Integer.MAX_VALUE, "destWidth");
        ExceptionHelper.checkArgumentInRange(destHeight, 0, Integer.MAX_VALUE, "destHeight");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(transBase, "transBase");

        if (inputWidth <= 0 || inputHeight <= 0) {
            return BasicImageTransformations.identityTransformation();
        }

        boolean magnify = options.contains(ZoomToFitOption.MAY_MAGNIFY);
        boolean keepAspectRatio = options.contains(ZoomToFitOption.KEEP_ASPECT_RATIO);
        boolean fitWidth = options.contains(ZoomToFitOption.FIT_WIDTH);
        boolean fitHeight = options.contains(ZoomToFitOption.FIT_HEIGHT);

        AffineTransform transf = new AffineTransform();
        transf.rotate(transBase.getRotateInRadians());

        Point2D.Double transformedWidthAndHeight
                = getTransformedWidthAndHeight(transf, inputWidth, inputHeight);
        double transformedWidth = transformedWidthAndHeight.x;
        double transformedHeight = transformedWidthAndHeight.y;

        double zoomX = (double)destWidth / transformedWidth;
        double zoomY = (double)destHeight / transformedHeight;

        if (keepAspectRatio) {
            double zoom = chooseZoom(fitWidth, fitHeight, zoomX, zoomY);
            zoomX = zoom;
            zoomY = zoom;
        }
        else {
            RotateType rotateType = getRotateType(transBase.getRotateInRadians());

            if (rotateType.isNormal()) {
                if (!fitWidth && (zoomX < 1.0 || !magnify)) {
                    zoomX = 1.0;
                }

                if (!fitHeight && (zoomY < 1.0 || !magnify)) {
                    zoomY = 1.0;
                }
            }

            if (!rotateType.isNormal() || fitWidth != fitHeight) {
                double zoom = chooseZoom(fitWidth, fitHeight, zoomX, zoomY);
                zoomX = zoom;
                zoomY = zoom;
            }
            else if (rotateType == RotateType.ROTATE_90) {
                double tmpZoom = zoomX;
                zoomX = zoomY;
                zoomY = tmpZoom;
            }
        }

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder(transBase);
        result.setOffset(0.0, 0.0);
        result.setZoomX(zoomX);
        result.setZoomY(zoomY);

        if (!magnify) {
            maximizeZoom(result, keepAspectRatio, 1.0, 1.0);
        }

        return result.create();
    }

    private static double chooseZoom(boolean fitWidth, boolean fitHeight, double zoomX, double zoomY) {
        if (fitWidth) {
            if (fitHeight) {
                return Math.min(zoomX, zoomY);
            }
            else {
                return zoomX;
            }
        }
        else {
            if (fitHeight) {
                return zoomY;
            }
            else {
                return 1.0;
            }
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

        BufferedImage inputImage = input.getInputImage().getImage();
        if (inputImage == null) {
            return TransformedImage.NULL_IMAGE;
        }

        BasicImageTransformations transformations;
        transformations = getBasicTransformations(
                inputImage.getWidth(),
                inputImage.getHeight(),
                input.getDestinationWidth(),
                input.getDestinationHeight(),
                options,
                transBase);

        boolean simpleTransf = AffineTransformationStep.isSimpleTransformation(transformations);
        InterpolationType appliedInterpolation = simpleTransf
                ? InterpolationType.NEAREST_NEIGHBOR
                : interpolationType;

        ImageTransformationStep applied = new AffineTransformationStep(
                transformations, bckgColor, appliedInterpolation);

        return applied.render(cancelToken, input, offeredBuffer);
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

    private enum RotateType {
        // 90 or 270
        ROTATE_90(true),
        // 0 or 180
        ROTATE_180(true),
        // not a multiple of 90
        ROTATE_ARBITRARY(false);

        private final boolean normal;

        private RotateType(boolean normal) {
            this.normal = normal;
        }

        public boolean isNormal() {
            return normal;
        }
    }
}
