package org.jtrim.swing.component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.transform.BasicImageTransformations;
import org.jtrim.image.transform.ZoomToFitOption;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class BasicTransformationModel {
    private final ListenerManager<TransformationListener, Void> transfListeners;

    private final BasicImageTransformations.Builder transformations;
    private Set<ZoomToFitOption> zoomToFit;


    public BasicTransformationModel() {
        this.transfListeners = new CopyOnTriggerListenerManager<>();

        this.transformations = new BasicImageTransformations.Builder();
        this.zoomToFit = null;
    }

    public ListenerRef addChangeListener(Runnable listener) {
        return addTransformationListener(new TransformationListenerForwarder(listener));
    }

    public ListenerRef addTransformationListener(TransformationListener listener) {
        return transfListeners.registerListener(listener);
    }

    private void fireZoomChange() {
        transfListeners.onEvent(ZoomChangeDispatcher.INSTANCE, null);
    }

    private void fireOffsetChange() {
        transfListeners.onEvent(OffsetChangedDispatcher.INSTANCE, null);
    }

    private void fireFlipChange() {
        transfListeners.onEvent(FlipChangedDispatcher.INSTANCE, null);
    }

    private void fireRotateChange() {
        transfListeners.onEvent(RotateChangedDispatcher.INSTANCE, null);
    }

    private void fireEnterZoomToFitMode() {
        transfListeners.onEvent(new ZoomToFitEnterDispatcher(zoomToFit), null);
    }

    private void fireLeaveZoomToFitMode() {
        transfListeners.onEvent(ZoomToFitLeaveDispatcher.INSTANCE, null);
    }

    public BasicImageTransformations getTransformations() {
        return transformations.create();
    }

    public void setFlip(boolean flipHorizontal, boolean flipVertical) {
        if (isFlipHorizontal() == flipHorizontal && isFlipVertical() == flipVertical) {
            return;
        }

        transformations.setFlipHorizontal(flipHorizontal);
        transformations.setFlipVertical(flipVertical);
        fireFlipChange();
    }

    public boolean isFlipHorizontal() {
        return transformations.isFlipHorizontal();
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        if (isFlipHorizontal() == flipHorizontal) {
            return;
        }

        transformations.setFlipHorizontal(flipHorizontal);
        fireFlipChange();
    }

    public void flipHorizontal() {
        transformations.flipHorizontal();
        fireFlipChange();
    }

    public boolean isFlipVertical() {
        return transformations.isFlipVertical();
    }

    public void setFlipVertical(boolean flipVertical) {
        if (isFlipVertical() == flipVertical) {
            return;
        }

        transformations.setFlipVertical(flipVertical);
        fireFlipChange();
    }

    public void flipVertical() {
        transformations.flipVertical();
        fireFlipChange();
    }

    public double getOffsetX() {
        return transformations.getOffsetX();
    }

    public double getOffsetY() {
        return transformations.getOffsetY();
    }

    public void setOffset(double offsetX, double offsetY) {
        if (getOffsetX() == offsetX && getOffsetY() == offsetY) {
            return;
        }

        transformations.setOffset(offsetX, offsetY);
        fireOffsetChange();
    }

    public double getRotateInRadians() {
        return transformations.getRotateInRadians();
    }

    public void setRotateInRadians(double radians) {
        if (getRotateInRadians() == radians) {
            return;
        }

        transformations.setRotateInRadians(radians);
        fireRotateChange();
    }

    public int getRotateInDegrees() {
        return transformations.getRotateInDegrees();
    }

    public void setRotateInDegrees(int degrees) {
        double prevRotateInRad = getRotateInRadians();
        transformations.setRotateInDegrees(degrees);
        if (prevRotateInRad != getRotateInRadians()) {
            fireRotateChange();
        }
    }

    public double getZoomX() {
        return transformations.getZoomX();
    }

    public void setZoom(double zoomX, double zoomY) {
        if (getZoomX() == zoomX && getZoomY() == zoomY) {
            return;
        }

        transformations.setZoomX(zoomX);
        transformations.setZoomY(zoomY);
        fireZoomChange();
    }

    public void setZoomX(double zoomX) {
        if (getZoomX() == zoomX) {
            return;
        }

        transformations.setZoomX(zoomX);
        fireZoomChange();
    }

    public double getZoomY() {
        return transformations.getZoomY();
    }

    public void setZoomY(double zoomY) {
        if (getZoomY() == zoomY) {
            return;
        }

        transformations.setZoomY(zoomY);
        fireZoomChange();
    }

    public void setZoom(double zoom) {
        if (getZoomX() == zoom && getZoomY() == zoom) {
            return;
        }

        transformations.setZoom(zoom);
        fireZoomChange();
    }

    public boolean isInZoomToFitMode() {
        return zoomToFit != null;
    }

    public Set<ZoomToFitOption> getZoomToFitOptions() {
        return zoomToFit != null
                ? EnumSet.copyOf(zoomToFit)
                : null;
    }

    public void clearZoomToFit() {
        if (zoomToFit != null) {
            zoomToFit = null;
            fireLeaveZoomToFitMode();
        }
    }

    public void setZoomToFit(boolean keepAspectRatio, boolean magnify) {
        setZoomToFit(keepAspectRatio, magnify, true, true);
    }

    public void setZoomToFit(boolean keepAspectRatio, boolean magnify,
            boolean fitWidth, boolean fitHeight) {

        EnumSet<ZoomToFitOption> newZoomToFit;

        newZoomToFit = EnumSet.noneOf(ZoomToFitOption.class);
        if (keepAspectRatio) newZoomToFit.add(ZoomToFitOption.KeepAspectRatio);
        if (magnify) newZoomToFit.add(ZoomToFitOption.MayMagnify);
        if (fitWidth) newZoomToFit.add(ZoomToFitOption.FitWidth);
        if (fitHeight) newZoomToFit.add(ZoomToFitOption.FitHeight);

        setZoomToFit(newZoomToFit);
    }

    public void setZoomToFit(Set<ZoomToFitOption> zoomToFitOptions) {
        ExceptionHelper.checkNotNullArgument(zoomToFitOptions, "zoomToFitOptions");

        EnumSet<ZoomToFitOption> newZoomToFit;
        newZoomToFit = EnumSet.copyOf(zoomToFitOptions);

        if (!newZoomToFit.equals(zoomToFit)) {
            zoomToFit = newZoomToFit;
            fireEnterZoomToFitMode();
        }
    }

    public void setTransformations(BasicImageTransformations newTransformations) {
        ExceptionHelper.checkNotNullArgument(newTransformations, "newTransformations");

        setOffset(newTransformations.getOffsetX(), newTransformations.getOffsetY());
        setRotateInRadians(newTransformations.getRotateInRadians());
        setZoom(newTransformations.getZoomX(), newTransformations.getZoomY());
        setFlip(newTransformations.isFlipHorizontal(), newTransformations.isFlipVertical());
    }

    private enum ZoomChangeDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {
        INSTANCE;

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.zoomChanged();
        }
    }

    private enum OffsetChangedDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {
        INSTANCE;

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.offsetChanged();
        }
    }

    private enum FlipChangedDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {
        INSTANCE;

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.flipChanged();
        }
    }

    private enum RotateChangedDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {
        INSTANCE;

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.rotateChanged();
        }
    }

    private static class ZoomToFitEnterDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {

        private final Set<ZoomToFitOption> zoomToFit;

        public ZoomToFitEnterDispatcher(Set<ZoomToFitOption> zoomToFit) {
            this.zoomToFit = Collections.unmodifiableSet(EnumSet.copyOf(zoomToFit));
        }

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.enterZoomToFitMode(zoomToFit);
        }
    }

    private enum ZoomToFitLeaveDispatcher
    implements
            EventDispatcher<TransformationListener, Void> {
        INSTANCE;

        @Override
        public void onEvent(TransformationListener eventArgument, Void arg) {
            eventArgument.leaveZoomToFitMode();
        }
    }

    private static class TransformationListenerForwarder implements TransformationListener {
        private final Runnable listener;

        public TransformationListenerForwarder(Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");
            this.listener = listener;
        }

        @Override
        public void zoomChanged() {
            listener.run();
        }

        @Override
        public void offsetChanged() {
            listener.run();
        }

        @Override
        public void flipChanged() {
            listener.run();
        }

        @Override
        public void rotateChanged() {
            listener.run();
        }

        @Override
        public void enterZoomToFitMode(Set<ZoomToFitOption> options) {
            listener.run();
        }

        @Override
        public void leaveZoomToFitMode() {
            listener.run();
        }
    }
}
