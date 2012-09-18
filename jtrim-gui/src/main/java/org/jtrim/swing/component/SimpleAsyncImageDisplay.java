/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataConverter;
import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.*;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial") // Not serializable
public class SimpleAsyncImageDisplay<ImageAddressType> extends AsyncImageDisplay<ImageAddressType> {
    private TaskExecutorService defaultExecutor;
    private final BasicTransformationModel transformations;
    private boolean alwaysClearZoomToFit;
    // Tasks to set arguments affecting painting of this component
    // should be executed by this executor for better performance.
    private final UpdateTaskExecutor argUpdater;

    private int lastTransformationIndex;
    private int lastTransformationCount;
    private InterpolationType[] interpolationTypes;
    private final Map<InterpolationType, TaskExecutorService> executors;

    public SimpleAsyncImageDisplay() {
        this.alwaysClearZoomToFit = false;
        this.transformations = new BasicTransformationModel();
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();
        this.argUpdater = new SwingUpdateTaskExecutor(true);

        this.lastTransformationIndex = 0;
        this.lastTransformationCount = 0;
        this.interpolationTypes = new InterpolationType[]{
            InterpolationType.BILINEAR
        };
        this.executors = new EnumMap<>(InterpolationType.class);
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();

        this.transformations.addChangeListener(new Runnable() {
            @Override
            public void run() {
                setTransformations();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setTransformations();
            }
        });

        this.transformations.addTransformationListener(new TransformationListener() {
            @Override
            public void zoomChanged() {
            }

            @Override
            public void offsetChanged() {
            }

            @Override
            public void flipChanged() {
            }

            @Override
            public void rotateChanged() {
            }

            @Override
            public void enterZoomToFitMode(Set<ZoomToFitOption> options) {
                ImageMetaData imageMetaData = getImageMetaData();
                if (imageMetaData != null) {
                    int imageWidth = imageMetaData.getWidth();
                    int imageHeight = imageMetaData.getHeight();

                    if (imageWidth > 0 && imageHeight > 0) {
                        int currentWidth = getWidth();
                        int currentHeight = getHeight();
                        BasicImageTransformations newTransformations;
                        newTransformations = ZoomToFitTransformation.getBasicTransformations(
                                imageWidth,
                                imageHeight,
                                currentWidth,
                                currentHeight,
                                options,
                                getTransformations());

                        transformations.setTransformations(newTransformations);
                    }
                }
            }

            @Override
            public void leaveZoomToFitMode() {
            }
        });
    }

    public final TaskExecutorService getDefaultExecutor() {
        return defaultExecutor;
    }

    public final void setDefaultExecutor(TaskExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        defaultExecutor = executor;
    }

    public final void setExecutor(InterpolationType interpolationType,
            TaskExecutorService executor) {

        if (executor == null) {
            removeExecutor(interpolationType);
        }
        else {
            executors.put(interpolationType, executor);
        }
    }

    public final void removeExecutor(InterpolationType interpolationType) {
        executors.remove(interpolationType);
    }

    public final void setInterpolationTypes(InterpolationType... interPolationTypes) {
        ExceptionHelper.checkArgumentInRange(interpolationTypes.length, 1, Integer.MAX_VALUE, "interpolationTypes.length");

        InterpolationType prevLastType
                = this.interpolationTypes[this.interpolationTypes.length - 1];

        this.interpolationTypes = interpolationTypes.clone();

        if (prevLastType != interpolationTypes[interpolationTypes.length - 1]) {
            setTransformations();
        }
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

    public final ListenerRef addTransformationListener(TransformationListener listener) {
        return transformations.addTransformationListener(listener);
    }

    public final void removeTransformationListener(TransformationListener listener) {
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

    private void setTransformations() {
        argUpdater.execute(new Runnable() {
            @Override
            public void run() {
                prepareTransformations(0, getBackground());
            }
        });
    }

    private TaskExecutorService getExecutor(InterpolationType interpolationType) {
        TaskExecutorService executor = executors.get(interpolationType);
        return executor != null ? executor : defaultExecutor;
    }

    private void clearLastTransformations() {
        int endIndex = lastTransformationIndex + lastTransformationCount;

        for (int i = lastTransformationIndex; i < endIndex; i++) {
            removeImageTransformer(i);
        }

        lastTransformationCount = 0;
    }

    private void prepareTransformations(int index, Color bckgColor) {
        if (lastTransformationIndex != index || lastTransformationCount != 1) {
            clearLastTransformations();
        }

        setCurrentTransformations(index, bckgColor);

        lastTransformationIndex = index;
        lastTransformationCount = 1;
    }

    private void setCurrentTransformations(int index, Color bckgColor) {
        final BasicImageTransformations currentTransf = transformations.getTransformations();

        Set<ZoomToFitOption> zoomToFit = transformations.getZoomToFitOptions();
        if (zoomToFit == null) {
            if (!AffineImageTransformer.isSimpleTransformation(currentTransf)) {
                List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers;
                imageTransformers = new ArrayList<>(interpolationTypes.length);

                for (InterpolationType interpolationType: interpolationTypes) {
                    TaskExecutorService executor = getExecutor(interpolationType);
                    ImageTransformer imageTransformer;
                    imageTransformer = new AffineImageTransformer(
                            currentTransf, bckgColor, interpolationType);

                    imageTransformers.add(new AsyncDataConverter<>(
                            imageTransformer, executor));
                }

                ImageTransfromerQuery query;
                query = new ImageTransfromerQuery(imageTransformers);

                setImageTransformer(index, ReferenceType.NoRefType, query);
            }
            else {
                ImageTransformer imageTransformer = new AffineImageTransformer(
                        currentTransf, bckgColor, InterpolationType.NEAREST_NEIGHBOR);

                TaskExecutorService executor;
                executor = getExecutor(InterpolationType.NEAREST_NEIGHBOR);

                ImageTransfromerQuery query;
                query = new ImageTransfromerQuery(executor, imageTransformer);

                setImageTransformer(index, ReferenceType.NoRefType, query);
            }
        }
        else {
            List<AsyncDataConverter<ImageTransformerData, TransformedImage>> imageTransformers;
            imageTransformers = new ArrayList<>(interpolationTypes.length);

            for (InterpolationType interpolationType: interpolationTypes) {
                TaskExecutorService executor = getExecutor(interpolationType);
                ImageTransformer imageTransformer;
                imageTransformer = new ZoomToFitTransformation(
                        currentTransf, zoomToFit, bckgColor, interpolationType);

                AsyncDataConverter<ImageTransformerData, TransformedImageData> asyncTransformer;

                if (imageTransformers.isEmpty()) {
                    imageTransformer = new ZoomToFitDataGatherer(
                            currentTransf, imageTransformer, zoomToFit);
                }

                imageTransformers.add(new AsyncDataConverter<>(
                        imageTransformer, executor));
            }

            ImageTransfromerQuery query;
            query = new ImageTransfromerQuery(imageTransformers);

            setImageTransformer(index, ReferenceType.NoRefType, query);
        }
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        renderAgain();
    }

    public final BasicImageTransformations getTransformations() {
        return transformations.getTransformations();
    }

    public final void setDefaultTransformations() {
        clearZoomToFit();
        transformations.setTransformations(BasicImageTransformations.identityTransformation());
    }

    public final void setTransformations(BasicImageTransformations transformations) {
        clearZoomToFit();
        this.transformations.setTransformations(transformations);
    }

    public final void setZoomY(double zoomY) {
        clearZoomToFit();
        transformations.setZoomY(zoomY);
    }

    public final void setZoomX(double zoomX) {
        clearZoomToFit();
        transformations.setZoomX(zoomX);
    }

    public final void setZoom(double zoom) {
        clearZoomToFit();
        transformations.setZoom(zoom);
    }

    public final void setRotateInRadians(double radians) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setRotateInRadians(radians);
    }

    public final void setRotateInDegrees(int degrees) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setRotateInDegrees(degrees);
    }

    public final void setOffset(double offsetX, double offsetY) {
        clearZoomToFit();
        transformations.setOffset(offsetX, offsetY);
    }

    public final void setFlipVertical(boolean flipVertical) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setFlipVertical(flipVertical);
    }

    public final void setFlipHorizontal(boolean flipHorizontal) {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.setFlipHorizontal(flipHorizontal);
    }

    public final void flipVertical() {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.flipVertical();
    }

    public final void flipHorizontal() {
        if (alwaysClearZoomToFit) {
            clearZoomToFit();
        }
        transformations.flipHorizontal();
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

    private final class ZoomToFitDataGatherer implements ImageTransformer {
        private final BasicImageTransformations transBase;
        private final ImageTransformer wrappedTransformer;
        private final Set<ZoomToFitOption> originalZoomToFit;
        private final Set<ZoomToFitOption> currentZoomToFit;

        public ZoomToFitDataGatherer(BasicImageTransformations transBase,
                ImageTransformer wrappedTransformer,
                Set<ZoomToFitOption> originalZoomToFit) {

            assert wrappedTransformer != null;
            assert transBase != null;

            this.transBase = transBase;
            this.wrappedTransformer = wrappedTransformer;
            this.originalZoomToFit = originalZoomToFit;
            this.currentZoomToFit = EnumSet.copyOf(originalZoomToFit);
        }

        @Override
        public TransformedImage convertData(ImageTransformerData input) throws ImageProcessingException {
            if (input.getSourceImage() != null) {
                final BasicImageTransformations newTransformations;
                newTransformations = ZoomToFitTransformation.getBasicTransformations(
                        input.getSrcWidth(),
                        input.getSrcHeight(),
                        input.getDestWidth(),
                        input.getDestHeight(),
                        currentZoomToFit,
                        transBase);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (Objects.equals(originalZoomToFit, transformations.getZoomToFitOptions())
                                && Objects.equals(transBase, transformations.getTransformations())) {
                            transformations.setTransformations(newTransformations);
                        }
                    }
                });
            }

            return wrappedTransformer.convertData(input);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(256);
            result.append("Collect ZoomToFit transformation data and ");
            AsyncFormatHelper.appendIndented(wrappedTransformer, result);

            return result.toString();
        }
    }
}
