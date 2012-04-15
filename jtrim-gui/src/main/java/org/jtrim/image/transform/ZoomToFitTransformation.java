/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Kelemen Attila
 */
public final class ZoomToFitTransformation implements ImageTransformer {
    public static BasicImageTransformations getBasicTransformations(
            int srcWidth, int srcHeight, int destWidth, int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase) {

        if (srcWidth <= 0 || srcHeight <= 0) {
            return BasicImageTransformations.identityTransformation();
        }

        boolean magnify = false;
        boolean keepAspectRatio = true;
        boolean fitWidth = true;
        boolean fitHeight = true;

        if (options != null) {
            keepAspectRatio = options.contains(ZoomToFitOption.KeepAspectRatio);
            magnify = options.contains(ZoomToFitOption.MayMagnify);
            fitWidth = options.contains(ZoomToFitOption.FitWidth);
            fitHeight = options.contains(ZoomToFitOption.FitHeight);
        }

        AffineTransform transf = new AffineTransform();

        if (transBase != null) {
            transf.rotate(transBase.getRotateInRadians());
        }

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

            if (transBase != null) {
                double baseRotate = transBase.getRotateInRadians();

                if (baseRotate == Math.PI / 2.0 || baseRotate == 3 * Math.PI / 2) {
                    rotate90 = true;
                }
                else if (baseRotate != 0.0 && baseRotate != Math.PI) {
                    normalRotate = false;
                }
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
        result = transBase != null
                ? new BasicImageTransformations.Builder(transBase)
                : new BasicImageTransformations.Builder();

        result.setOffset(0.0, 0.0);
        result.setZoomX(zoomX);
        result.setZoomY(zoomY);

        return result.create();
    }

    private final BasicImageTransformations transBase;
    private final Set<ZoomToFitOption> options;
    private final Color bckgColor;
    private final InterpolationType interpolationType;

    public ZoomToFitTransformation(BasicImageTransformations transBase,
            Set<ZoomToFitOption> options, Color bckgColor,
            InterpolationType interpolationType) {

        this.transBase = transBase;
        this.options = EnumSet.copyOf(options);
        this.bckgColor = bckgColor;
        this.interpolationType = interpolationType;
    }

    @Override
    public TransformedImage convertData(ImageTransformerData input) throws ImageProcessingException {
        BasicImageTransformations transformations;
        transformations = ZoomToFitTransformation.getBasicTransformations(input.getSrcWidth(),
                input.getSrcHeight(),
                input.getDestWidth(),
                input.getDestHeight(),
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
        return transformer.convertData(input);
    }

    @Override
    public String toString() {
        return "ZoomToFit " + options
                + " use interpolation " + interpolationType;
    }
}
