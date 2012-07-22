/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;


import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import org.jtrim.cache.ReferenceType;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.async.AsyncDataConverter;
import org.jtrim.concurrent.async.AsyncFormatHelper;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.transform.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class BasicTransformationContainer {
    private static final double MIN_ZOOM_VALUE = 0.0001;

    private final EventDispatcher<TransformationListener, Void> zoomEventHandler
            = new ZoomEventHandler();

    private final EventDispatcher<TransformationListener, Void> offsetEventHandler
            = new OffsetEventHandler();

    private final EventDispatcher<TransformationListener, Void> flipEventHandler
            = new FlipEventHandler();

    private final EventDispatcher<TransformationListener, Void> rotateEventHandler
            = new RotateEventHandler();

    private final EventDispatcher<TransformationListener, Void> zoomToFitEnterEventHandler
            = new ZoomToFitEnterEventHandler();

    private static final EventDispatcher<TransformationListener, Void> zoomToFitLeaveEventHandler
            = new ZoomToFitLeaveEventHandler();

    private final RecursionState zoomState;
    private final RecursionState offsetState;
    private final RecursionState flipState;
    private final RecursionState rotateState;
    private Color lastBckgColor;

    private boolean enableRecursion;

    private final ListenerManager<TransformationListener, Void> transfListeners;
    private InterpolationType[] interpolationTypes;

    private final ListenerManager<Runnable, Void> changeListeners;
    private final BasicImageTransformations.Builder transformations;
    private int lastTransformationIndex;
    private int lastTransformationCount;

    private Set<ZoomToFitOption> zoomToFit;

    private TaskExecutorService defaultExecutor;
    private final Map<InterpolationType, TaskExecutorService> executors;

    public BasicTransformationContainer() {
        this.lastBckgColor = null;
        this.enableRecursion = false;
        this.zoomState = new RecursionState();
        this.offsetState = new RecursionState();
        this.flipState = new RecursionState();
        this.rotateState = new RecursionState();

        this.changeListeners = new CopyOnTriggerListenerManager<>();
        this.transfListeners = new CopyOnTriggerListenerManager<>();
        this.transformations = new BasicImageTransformations.Builder();
        this.zoomToFit = null;

        this.lastTransformationIndex = 0;
        this.lastTransformationCount = 0;
        this.interpolationTypes = new InterpolationType[]{
            InterpolationType.BILINEAR
        };

        this.executors = new EnumMap<>(InterpolationType.class);
        this.defaultExecutor = SyncTaskExecutor.getDefaultInstance();
    }

    public void setDefaultExecutor(TaskExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        defaultExecutor = executor;
    }

    public void setExecutor(InterpolationType interpolationType, TaskExecutorService executor) {
        if (executor == null) {
            removeExecutor(interpolationType);
        }
        else {
            executors.put(interpolationType, executor);
        }
    }

    public void removeExecutor(InterpolationType interpolationType) {
        executors.remove(interpolationType);
    }

    public TaskExecutorService getExecutor(InterpolationType interpolationType) {
        TaskExecutorService executor = executors.get(interpolationType);
        return executor != null ? executor : defaultExecutor;
    }

    public boolean isInZoomToFitMode() {
        return zoomToFit != null;
    }

    public boolean isEnableRecursion() {
        return enableRecursion;
    }

    public void setEnableRecursion(boolean enableRecursion) {
        this.enableRecursion = enableRecursion;
    }

    public ListenerRef addTransformationListener(TransformationListener listener) {
        return transfListeners.registerListener(listener);
    }

    private void clearLastTransformations(AsyncImageDisplay<?> display) {
        int endIndex = lastTransformationIndex + lastTransformationCount;

        for (int i = lastTransformationIndex; i < endIndex; i++) {
            display.removeImageTransformer(i);
        }

        lastTransformationCount = 0;
    }

    public void prepareTransformations(AsyncImageDisplay<?> display, int index, Color bckgColor) {
        if (lastTransformationIndex != index || lastTransformationCount != 1) {
            clearLastTransformations(display);
        }

        lastBckgColor = bckgColor;
        setCurrentTransformations(display, index, bckgColor);

        lastTransformationIndex = index;
        lastTransformationCount = 1;
    }

    private void setCurrentTransformations(AsyncImageDisplay<?> display, int index, Color bckgColor) {
        final BasicImageTransformations currentTransf = transformations.create();

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

                display.setImageTransformer(index, ReferenceType.NoRefType, query);
            }
            else {
                ImageTransformer imageTransformer = new AffineImageTransformer(
                        currentTransf, bckgColor, InterpolationType.NEAREST_NEIGHBOR);

                TaskExecutorService executor;
                executor = getExecutor(InterpolationType.NEAREST_NEIGHBOR);

                ImageTransfromerQuery query;
                query = new ImageTransfromerQuery(executor, imageTransformer);

                display.setImageTransformer(index, ReferenceType.NoRefType, query);
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

            display.setImageTransformer(index, ReferenceType.NoRefType, query);
        }
    }

    private boolean allowZoom() {
        return enableRecursion || !zoomState.isCalled();
    }

    private boolean allowRotate() {
        return enableRecursion || !rotateState.isCalled();
    }

    private boolean allowTranslate() {
        return enableRecursion || !offsetState.isCalled();
    }

    private boolean allowFlip() {
        return enableRecursion || !flipState.isCalled();
    }

    private void setDirtyTransformations() {
        changeListeners.onEvent(RunnableDispatcher.INSTANCE, null);
    }

    public ListenerRef addChangeListener(Runnable listener) {
        return changeListeners.registerListener(listener);
    }

    public void setInterpolationTypes(InterpolationType... interpolationTypes) {
        ExceptionHelper.checkArgumentInRange(interpolationTypes.length, 1, Integer.MAX_VALUE, "interpolationTypes.length");

        InterpolationType prevLastType
                = this.interpolationTypes[this.interpolationTypes.length - 1];

        this.interpolationTypes = interpolationTypes.clone();

        if (prevLastType != interpolationTypes[interpolationTypes.length - 1]) {
            setDirtyTransformations();
        }
    }

    public BasicImageTransformations getTransformations() {
        return transformations.create();
    }

    public void setDefaultTransformations() {
        setTransformations(BasicImageTransformations.identityTransformation());
    }

    private double convertZoom(double zoom) {
        return zoom > MIN_ZOOM_VALUE ? zoom : MIN_ZOOM_VALUE;
    }

    private void setTransformations(BasicImageTransformations transformations, boolean changeOnly) {
        double newZoomX = convertZoom(transformations.getZoomX());
        double newZoomY = convertZoom(transformations.getZoomY());

        boolean flipChange = (changeOnly || allowFlip()) &&
                (this.transformations.isFlipHorizontal() != transformations.isFlipHorizontal() ||
                this.transformations.isFlipVertical() != transformations.isFlipVertical());

        boolean zoomChange = (changeOnly || allowZoom()) &&
                (this.transformations.getZoomX() != newZoomX ||
                this.transformations.getZoomY() != newZoomY);

        boolean offsetChange = (changeOnly || allowTranslate()) &&
                (this.transformations.getOffsetX() != transformations.getOffsetX() ||
                this.transformations.getOffsetY() != transformations.getOffsetY());

        boolean rotateChange = (changeOnly || allowRotate()) &&
                (this.transformations.getRotateInRadians() != transformations.getRotateInRadians() ||
                this.transformations.getRotateInDegrees() != transformations.getRotateInDegrees());

        if (zoomChange) {
            this.transformations.setZoomX(newZoomX);
            this.transformations.setZoomY(newZoomY);
        }

        if (offsetChange) {
            this.transformations.setOffset(transformations.getOffsetX(), transformations.getOffsetY());
        }

        if (rotateChange) {
            this.transformations.setRotateInRadians(transformations.getRotateInRadians());
        }

        if (flipChange) {
            this.transformations.setFlipHorizontal(transformations.isFlipHorizontal());
            this.transformations.setFlipVertical(transformations.isFlipVertical());
        }

        if (flipChange) {
            flipChanged();
        }

        if (zoomChange) {
            zoomChanged();
        }

        if (offsetChange) {
            offsetChanged();
        }

        if (rotateChange) {
            rotateChanged();
        }

        if (!changeOnly) {
            clearZoomToFit();

            if (flipChange || zoomChange || offsetChange || rotateChange) {
                setDirtyTransformations();
            }
        }
    }

    public void setTransformations(BasicImageTransformations transformations) {
        setTransformations(transformations, false);
    }

    public void setZoomY(double zoomY) {
        if (!allowZoom()) return;

        double oldZoomY = transformations.getZoomY();
        transformations.setZoomY(zoomY > MIN_ZOOM_VALUE ? zoomY : MIN_ZOOM_VALUE);

        if (oldZoomY != transformations.getZoomY()) {
            zoomChanged();
            setDirtyTransformations();
        }

        clearZoomToFit();
    }

    public void setZoomX(double zoomX) {
        if (!allowZoom()) return;

        double oldZoomX = transformations.getZoomX();
        transformations.setZoomX(zoomX > MIN_ZOOM_VALUE ? zoomX : MIN_ZOOM_VALUE);

        if (oldZoomX != transformations.getZoomX()) {
            zoomChanged();
            setDirtyTransformations();
        }

        clearZoomToFit();
    }

    public void setZoom(double zoom) {
        if (!allowZoom()) return;

        double oldZoomX = transformations.getZoomX();
        double oldZoomY = transformations.getZoomY();
        transformations.setZoom(zoom > MIN_ZOOM_VALUE ? zoom : MIN_ZOOM_VALUE);

        if (oldZoomX != transformations.getZoomX() || oldZoomY != transformations.getZoomY()) {
            zoomChanged();
            setDirtyTransformations();
        }

        clearZoomToFit();
    }

    public void setRotateInRadians(double radians) {
        if (!allowRotate()) return;

        double oldRotate = transformations.getRotateInRadians();
        transformations.setRotateInRadians(radians);

        if (oldRotate != transformations.getRotateInRadians()) {
            rotateChanged();
            setDirtyTransformations();
        }
    }

    public void setRotateInDegrees(int degrees) {
        if (!allowRotate()) return;

        double oldRotate = transformations.getRotateInRadians();
        transformations.setRotateInDegrees(degrees);

        if (oldRotate != transformations.getRotateInRadians()) {
            rotateChanged();
            setDirtyTransformations();
        }
    }

    public void setOffset(double offsetX, double offsetY) {
        if (!allowTranslate()) return;

        double oldOffsetX = transformations.getOffsetX();
        double oldOffsetY = transformations.getOffsetY();
        transformations.setOffset(offsetX, offsetY);

        if (oldOffsetX != transformations.getOffsetX() || oldOffsetY != transformations.getOffsetY()) {
            offsetChanged();
            setDirtyTransformations();
        }

        clearZoomToFit();
    }

    public void setFlipVertical(boolean flipVertical) {
        if (!allowFlip()) return;

        if (transformations.isFlipVertical() != flipVertical) {
            transformations.setFlipVertical(flipVertical);
            flipChanged();
            setDirtyTransformations();
        }
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        if (!allowFlip()) return;

        if (transformations.isFlipHorizontal() != flipHorizontal) {
            transformations.setFlipHorizontal(flipHorizontal);
            flipChanged();
            setDirtyTransformations();
        }
    }

    public void flipVertical() {
        if (!allowFlip()) return;

        transformations.flipVertical();
        flipChanged();
        setDirtyTransformations();
    }

    public void flipHorizontal() {
        if (!allowFlip()) return;

        transformations.flipHorizontal();
        flipChanged();
        setDirtyTransformations();
    }

    public void clearZoomToFit() {
        if (zoomToFit != null) {
            zoomToFit = null;
            leaveZoomToFitMode();

            setDirtyTransformations();
        }
    }

    public void setZoomToFit(AsyncImageDisplay<?> display, boolean keepAspectRatio, boolean magnify) {
        setZoomToFit(display, keepAspectRatio, magnify, true, true);
    }

    public void setZoomToFit(AsyncImageDisplay<?> display, boolean keepAspectRatio, boolean magnify,
            boolean fitWidth, boolean fitHeight) {

        EnumSet<ZoomToFitOption> newZoomToFit;

        newZoomToFit = EnumSet.noneOf(ZoomToFitOption.class);
        if (keepAspectRatio) newZoomToFit.add(ZoomToFitOption.KeepAspectRatio);
        if (magnify) newZoomToFit.add(ZoomToFitOption.MayMagnify);
        if (fitWidth) newZoomToFit.add(ZoomToFitOption.FitWidth);
        if (fitHeight) newZoomToFit.add(ZoomToFitOption.FitHeight);

        setZoomToFit(display, newZoomToFit);
    }

    public void setZoomToFit(AsyncImageDisplay<?> display, Set<ZoomToFitOption> zoomToFitOptions) {
        EnumSet<ZoomToFitOption> newZoomToFit;
        newZoomToFit = EnumSet.copyOf(zoomToFitOptions);

        if (!newZoomToFit.equals(zoomToFit)) {
            ImageMetaData imageMetaData = display.getImageMetaData();
            if (imageMetaData != null) {
                int imageWidth = imageMetaData.getWidth();
                int imageHeight = imageMetaData.getHeight();

                if (imageWidth > 0 && imageHeight > 0) {
                    int currentWidth = display.getWidth();
                    int currentHeight = display.getHeight();

                    setTransformations(
                            ZoomToFitTransformation.getBasicTransformations(
                            imageWidth, imageHeight,
                            currentWidth, currentHeight,
                            newZoomToFit, transformations.create()), true);
                }
            }

            zoomToFit = newZoomToFit;
            enterZoomToFitMode();
            setDirtyTransformations();
        }
    }

    private void zoomChanged() {
        zoomState.enterCall();
        try {
            transfListeners.onEvent(zoomEventHandler, null);
        } finally {
            zoomState.leaveCall();
        }
    }

    private void offsetChanged() {
        offsetState.enterCall();
        try {
            transfListeners.onEvent(offsetEventHandler, null);
        } finally {
            offsetState.leaveCall();
        }
    }

    private void flipChanged() {
        flipState.enterCall();
        try {
            transfListeners.onEvent(flipEventHandler, null);
        } finally {
            flipState.leaveCall();
        }
    }

    private void rotateChanged() {
        rotateState.enterCall();
        try {
            transfListeners.onEvent(rotateEventHandler, null);
        } finally {
            rotateState.leaveCall();
        }
    }

    private void enterZoomToFitMode() {
        if (zoomToFit == null) {
            throw new IllegalStateException("Cannot enter zoom to fit mode.");
        }

        transfListeners.onEvent(zoomToFitEnterEventHandler, null);
    }

    private void leaveZoomToFitMode() {
        transfListeners.onEvent(zoomToFitLeaveEventHandler, null);
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
                        if (originalZoomToFit == zoomToFit) {
                            setTransformations(newTransformations, true);
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

    // Event handler classes

    private class ZoomEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.zoomChanged(
                    transformations.getZoomX(),
                    transformations.getZoomY());
        }
    }

    private class OffsetEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.offsetChanged(
                    transformations.getOffsetX(),
                    transformations.getOffsetY());
        }
    }

    private class FlipEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.flipChanged(
                    transformations.isFlipHorizontal(),
                    transformations.isFlipVertical());
        }
    }

    private class RotateEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.rotateChanged(transformations.getRotateInRadians());
        }
    }

    private class ZoomToFitEnterEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.enterZoomToFitMode(EnumSet.copyOf(zoomToFit));
        }
    }

    private static class ZoomToFitLeaveEventHandler
    implements
            EventDispatcher<TransformationListener, Void> {

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.leaveZoomToFitMode();
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }

    private static class RecursionState {
        private final AtomicLong callCount;

        public RecursionState() {
            this.callCount = new AtomicLong(0);
        }

        public void enterCall() {
            callCount.incrementAndGet();
        }

        public void leaveCall() {
            if (callCount.decrementAndGet() < 0) {
                throw new IllegalStateException("There are too many leave calls.");
            }
        }

        public boolean isRecursive() {
            return getRecursionCount() > 1;
        }

        public boolean isCalled() {
            return getRecursionCount() > 0;
        }

        public long getRecursionCount() {
            return callCount.get();
        }
    }
}
