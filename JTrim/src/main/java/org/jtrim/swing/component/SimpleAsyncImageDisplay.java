/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.concurrent.ExecutorService;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.*;
import org.jtrim.swing.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial") // Not serializable
public class SimpleAsyncImageDisplay<ImageAddressType> extends AsyncImageDisplay<ImageAddressType> {
    private ExecutorService defaultExecutor;
    private final BasicTransformationContainer transformations;
    private boolean alwaysClearZoomToFit;

    public SimpleAsyncImageDisplay() {
        this.alwaysClearZoomToFit = false;
        this.transformations = new BasicTransformationContainer(this);
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();
    }

    public final ExecutorService getDefaultExecutor() {
        return defaultExecutor;
    }

    public final void setDefaultExecutor(ExecutorService defaultExecutor) {
        ExceptionHelper.checkNotNullArgument(defaultExecutor, "defaultExecutor");

        this.defaultExecutor = defaultExecutor;
        transformations.setDefaultExecutor(defaultExecutor);
    }

    public final void setExecutor(InterpolationType interpolationType,
            ExecutorService executor) {
        transformations.setExecutor(interpolationType, executor);
    }

    public final void removeExecutor(InterpolationType interpolationType) {
        transformations.removeExecutor(interpolationType);
    }

    public final void setInterpolationTypes(InterpolationType... interPolationTypes) {
        transformations.setInterpolationTypes(interPolationTypes);
    }

    public final boolean isInZoomToFitMode() {
        return transformations.isInZoomToFitMode();
    }

    public final boolean isAlwaysClearZoomToFit() {
        return alwaysClearZoomToFit;
    }

    public final void setAlwaysClearZoomToFit(boolean alwaysClearZoomToFit) {
        this.alwaysClearZoomToFit = alwaysClearZoomToFit;
    }

    public final boolean isEnableRecursion() {
        return transformations.isEnableRecursion();
    }

    public final void setEnableRecursion(boolean enableRecursion) {
        transformations.setEnableRecursion(enableRecursion);
    }

    public final void addTransformationListener(TransformationListener listener) {
        transformations.addTransformationListener(listener);
    }

    public final void removeTransformationListener(TransformationListener listener) {
        transformations.removeTransformationListener(listener);
    }

    @Override
    public ImagePointTransformer getPointTransformer() {
        ImageMetaData metaData = getImageMetaData();
        if (metaData != null) {
            int srcWidth = metaData.getWidth();
            int srcHeight = metaData.getHeight();

            int destWidth = getWidth();
            int destHeight = getHeight();

            BasicImageTransformations transf = transformations.getTransformations();
            return new AffineImagePointTransformer(AffineImageTransformer.getTransformationMatrix(transf, srcWidth, srcHeight, destWidth, destHeight));
        }
        else {
            return getDisplayedPointTransformer();
        }
    }

    public final Point2D getDisplayPoint(Point2D imagePoint) {
        ImagePointTransformer pointTransformer = getPointTransformer();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            pointTransformer.transformSrcToDest(imagePoint, result);
        }
        else {
            result.setLocation(imagePoint);
        }

        return result;
    }

    public final Point2D getImagePoint(Point2D displayPoint) {
        ImagePointTransformer pointTransformer = getPointTransformer();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            try {
                pointTransformer.transformDestToSrc(displayPoint, result);
            } catch (NoninvertibleTransformException ex) {
                throw new IllegalStateException("Non-invertible transformation", ex);
            }
        }
        else {
            result.setLocation(displayPoint);
        }

        return result;
    }

    public final void moveImagePointToDisplayPoint(Point2D imagePoint, Point2D displayPoint) {
        BasicImageTransformations transf = getTransformations();
        ImagePointTransformer pointTransformer = getPointTransformer();

        if (pointTransformer != null && transf != null) {
            double offsetErrX;
            double offsetErrY;

            Point2D currentDisplayPoint = new Point2D.Double();
            pointTransformer.transformSrcToDest(imagePoint, currentDisplayPoint);

            offsetErrX = displayPoint.getX() - currentDisplayPoint.getX();
            offsetErrY = displayPoint.getY() - currentDisplayPoint.getY();

            double newOffsetX = transf.getOffsetX() + offsetErrX;
            double newOffsetY = transf.getOffsetY() + offsetErrY;

            setOffset(newOffsetX, newOffsetY);
        }
    }

    @Override
    protected void prepareTransformations() {
        transformations.prepareTransformations(0, getBackground());
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        setDirty();
    }

    public final BasicImageTransformations getTransformations() {
        return transformations.getTransformations();
    }

    public final void setDefaultTransformations() {
        transformations.setDefaultTransformations();
    }

    public final void setTransformations(BasicImageTransformations transformations) {
        this.transformations.setTransformations(transformations);
    }

    public final void setZoomY(double zoomY) {
        transformations.setZoomY(zoomY);
    }

    public final void setZoomX(double zoomX) {
        transformations.setZoomX(zoomX);
    }

    public final void setZoom(double zoom) {
        transformations.setZoom(zoom);
    }

    public final void setRotateInRadians(double radians) {
        transformations.setRotateInRadians(radians);
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void setRotateInDegrees(int degrees) {
        transformations.setRotateInDegrees(degrees);
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void setOffset(double offsetX, double offsetY) {
        transformations.setOffset(offsetX, offsetY);
    }

    public final void setFlipVertical(boolean flipVertical) {
        transformations.setFlipVertical(flipVertical);
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void setFlipHorizontal(boolean flipHorizontal) {
        transformations.setFlipHorizontal(flipHorizontal);
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void flipVertical() {
        transformations.flipVertical();
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void flipHorizontal() {
        transformations.flipHorizontal();
        if (alwaysClearZoomToFit) {
            transformations.clearZoomToFit();
        }
    }

    public final void clearZoomToFit() {
        transformations.clearZoomToFit();
    }

    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify) {
        transformations.setZoomToFit(keepAspectRatio, magnify);
    }

    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify, boolean fitWidth, boolean fitHeight) {
        transformations.setZoomToFit(keepAspectRatio, magnify, fitWidth, fitHeight);
    }
}
