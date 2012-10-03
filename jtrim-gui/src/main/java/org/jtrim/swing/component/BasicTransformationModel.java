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
 * Defines a container for {@link BasicImageTransformations basic  image transformations}
 * and for the zoom to fit transformation. Client code may listen for changes
 * in a {@code BasicTransformationModel} by
 * {@link #addTransformationListener(TransformationListener) adding listeners}.
 * Therefore this class is suited to be used by <I>Swing</I> components.
 * <P>
 * Notice that when using the zoom to fit transformation, there are properties
 * which might cannot be fulfilled (such as zoom). In these cases the zoom to
 * fit should take precedence.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are not safe to be used by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>. Note
 * therefore that you should avoid organizing access to instances of this class
 * using locks because methods of this class might invoke listeners (and you
 * should have little expectations on what listeners do). The best way to use
 * this class in a multi-thread environment is to use it from a dedicated
 * thread, or from a {@link org.jtrim.concurrent.TaskExecutor TaskExecutor}
 * which does not execute tasks concurrently. You may also use the
 * {@link org.jtrim.concurrent.TaskExecutors#inOrderExecutor(TaskExecutor) TaskExecutors.inOrderExecutor(TaskExecutor)}
 * method to create such an executor.
 * <P>
 * Note however, that this class was designed for <I>Swing</I> components and
 * if you are using this with one, you should only use this class on the
 * <I>AWT Event Dispatch Thread</I>.
 *
 * @see BasicImageTransformations
 *
 * @author Kelemen Attila
 */
public final class BasicTransformationModel {
    private final ListenerManager<TransformationListener, Void> transfListeners;

    private final BasicImageTransformations.Builder transformations;
    private Set<ZoomToFitOption> zoomToFit;

    /**
     * Creates a new {@code BasicTransformationModel} with the identity
     * transformation and with zoom to fit being disabled (i.e., the
     * {@link #isInZoomToFitMode() isInZoomToFitMode()} method returns
     * {@code false} initially).
     */
    public BasicTransformationModel() {
        this.transfListeners = new CopyOnTriggerListenerManager<>();

        this.transformations = new BasicImageTransformations.Builder();
        this.zoomToFit = null;
    }

    /**
     * Adds listener which will be notified whenever any of the properties of
     * this {@code BasicTransformationModel} changes. This is equivalent to
     * {@link #addTransformationListener(TransformationListener) adding a transformation listener}
     * and notifying the listener in every methods of the listener.
     *
     * @param listener the {@code Runnable} whose {@code run} method is to be
     *   called when any of the properties changes. This argument cannot be
     *   {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     *
     * @see #addTransformationListener(TransformationListener)
     */
    public ListenerRef addChangeListener(Runnable listener) {
        return addTransformationListener(new TransformationListenerForwarder(listener));
    }

    /**
     * Adds a listener whose appropriate method will be called when a property
     * of this {@code BasicTransformationModel} changes.
     * <P>
     * If you want to execute the same code for every property changes, consider
     * using the {@link #addChangeListener(Runnable) addChangeListener} method
     * instead.
     *
     * @param listener the listener whose appropriate method will be called
     *   when a property changes. This argument cannot be {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     *
     * @see #addChangeListener(Runnable)
     */
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

    /**
     * Returns the snapshot of the transformations currently set
     * transformations. The return value does not include to currently set
     * zoom to fit mode.
     *
     * @return the snapshot of the transformations currently set
     *   transformations. This method never returns {@code null}.
     */
    public BasicImageTransformations getTransformations() {
        return transformations.create();
    }

    /**
     * Sets if the image should be flipped horizontally or vertically.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param flipHorizontal if {@code true} the image should be flipped
     *   horizontally, so that the left side of the image becomes the right side
     *   of the image, otherwise set it to {@code false}
     * @param flipVertical if {@code true} the image should be flipped
     *   vertically, so that the top side of the image becomes the bottom side
     *   of the image, otherwise set it to {@code false}
     *
     * @see TransformationListener#flipChanged()
     */
    public void setFlip(boolean flipHorizontal, boolean flipVertical) {
        if (isFlipHorizontal() == flipHorizontal && isFlipVertical() == flipVertical) {
            return;
        }

        transformations.setFlipHorizontal(flipHorizontal);
        transformations.setFlipVertical(flipVertical);
        fireFlipChange();
    }

    /**
     * Returns {@code true} if the image needs to be flipped horizontally.
     * That is, the left side of the image need to be the right side of the
     * image.
     *
     * @return {@code true} if the image needs to be flipped horizontally,
     *   {@code false} otherwise
     */
    public boolean isFlipHorizontal() {
        return transformations.isFlipHorizontal();
    }

    /**
     * Sets if the image should be flipped horizontally. That is, if the left
     * side of the image should become the right side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param flipHorizontal if {@code true} the image should be flipped
     *   horizontally, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public void setFlipHorizontal(boolean flipHorizontal) {
        if (isFlipHorizontal() == flipHorizontal) {
            return;
        }

        transformations.setFlipHorizontal(flipHorizontal);
        fireFlipChange();
    }

    /**
     * Inverts the {@link #isFlipHorizontal() "FlipHorizontal" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipHorizontal(!isFlipHorizontal())}.
     * <P>
     * This method will notify the appropriate listeners.
     *
     * @see TransformationListener#flipChanged()
     */
    public void flipHorizontal() {
        transformations.flipHorizontal();
        fireFlipChange();
    }

    /**
     * Returns {@code true} if the image needs to be flipped vertically.
     * That is, the top side of the image need to be the bottom side of the
     * image.
     *
     * @return {@code true} if the image needs to be flipped vertically,
     *   {@code false} otherwise
     */
    public boolean isFlipVertical() {
        return transformations.isFlipVertical();
    }

    /**
     * Sets if the image should be flipped vertically. That is, if the top
     * side of the image should become the bottom side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param flipVertical if {@code true} the image should be flipped
     *   vertically, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public void setFlipVertical(boolean flipVertical) {
        if (isFlipVertical() == flipVertical) {
            return;
        }

        transformations.setFlipVertical(flipVertical);
        fireFlipChange();
    }

    /**
     * Inverts the {@link #isFlipVertical() "FlipVertical" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipVertical(!isFlipVertical())}.
     * <P>
     * This method will notify the appropriate listeners.
     *
     * @see TransformationListener#flipChanged()
     */
    public void flipVertical() {
        transformations.flipVertical();
        fireFlipChange();
    }

    /**
     * Returns the offset along the horizontal axis. Note that the offset is to
     * be applied after zoom has been applied.
     *
     * @return the offset along the horizontal axis
     */
    public double getOffsetX() {
        return transformations.getOffsetX();
    }

    /**
     * Returns the offset along the vertical axis. Note that the offset is to
     * be applied after zoom has been applied.
     *
     * @return the offset along the vertical axis
     */
    public double getOffsetY() {
        return transformations.getOffsetY();
    }

    /**
     * Sets both the horizontal an vertical offset need to be applied on the
     * image.
     * <P>
     * Note that these offsets are to be applied after zoom has been applied.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param offsetX the offset along the horizontal axis. Subsequent
     *   {@link #getOffsetX() getOffsetX()} method calls will return this value.
     * @param offsetY the offset along the vertical axis. Subsequent
     *   {@link #getOffsetY() getOffsetY()} method calls will return this value.
     */
    public void setOffset(double offsetX, double offsetY) {
        if (getOffsetX() == offsetX && getOffsetY() == offsetY) {
            return;
        }

        transformations.setOffset(offsetX, offsetY);
        fireOffsetChange();
    }

    /**
     * Returns the angle meaning how much the image need to be rotated around
     * its center in radians. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     *
     * @return the angle meaning how much the image need to be rotated around
     *   its center in radians. This method returns always returns a normalized
     *   angle. That is, a value between 0 and {@code 2*pi} or {@code NaN} if
     *   {@code NaN} was set.
     */
    public double getRotateInRadians() {
        return transformations.getRotateInRadians();
    }

    /**
     * Sets the angle meaning how much the image need to be rotated around
     * its center in radians. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Note: Settings this property also changes the value returned by the
     * {@link #getRotateInDegrees() getRotateInDegrees()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param radians the angle meaning how much the image need to be rotated
     *   around its center in radians. Note that this property is stored in a
     *   normalized form. So this property will be set to value between
     *   0 and {@code 2*pi} or {@code NaN}.
     */
    public void setRotateInRadians(double radians) {
        if (getRotateInRadians() == radians) {
            return;
        }

        transformations.setRotateInRadians(radians);
        fireRotateChange();
    }

    /**
     * Returns the angle meaning how much the image need to be rotated around
     * its center in degrees. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Notice that this property is an {@code int}, if you need better
     * precision, use the {@link #getRotateInRadians() getRotateInRadians()}
     * method.
     * <P>
     * If the rotation amount was set by the
     * {@link #setRotateInRadians(double) setRotateInRadians} method, this
     * method will round the value to the closest integer (after converting
     * radians to degrees).
     *
     * @return the angle meaning how much the image need to be rotated around
     *   its center in radians. This method returns always returns a normalized
     *   angle. That is, a value which is greater than or equal to zero and
     *   less than (not equal) to 360.
     */
    public int getRotateInDegrees() {
        return transformations.getRotateInDegrees();
    }

    /**
     * Sets the angle meaning how much the image need to be rotated around
     * its center in degrees. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Notice that this property is an {@code int}, if you need better
     * precision, use the {@link #setRotateInRadians(double) setRotateInRadians}
     * method.
     * <P>
     * Note: Settings this property also changes the value returned by the
     * {@link #getRotateInRadians() getRotateInRadians()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param degrees the angle meaning how much the image need to be rotated
     *   around its center in degrees. Note that this property is stored in a
     *   normalized form. So this property will be set to value which is greater
     *   than or equal to zero and less than (not equal) to 360.
     */
    public void setRotateInDegrees(int degrees) {
        double prevRotateInRad = getRotateInRadians();
        transformations.setRotateInDegrees(degrees);
        if (prevRotateInRad != getRotateInRadians()) {
            fireRotateChange();
        }
    }

    /**
     * Returns the scaling factor used to scale the image horizontally. For
     * example: If this method returns {@code 2.0}, the image must be scaled
     * to an image which has twice the width of the original image.
     * Respectively, a value of {@code 0.5} means that the resulting image must
     * be scaled to an image which has half the width of the original image.
     * <P>
     * This method may return a negative value which should be interpreted as
     * {@link #flipHorizontal() flipping the image horizontally} and scaling
     * with the absolute value.
     * <P>
     * The client code should also expect that this property might be zero,
     * {@code NaN} or even infinity. How these special values are handled are
     * left to the client code.
     *
     * @return the scaling factor used to scale the image horizontally
     */
    public double getZoomX() {
        return transformations.getZoomX();
    }

    /**
     * Sets the scaling factors used to scale the image horizontally and
     * vertically. The scaling must be interpreted in the coordinate system of
     * the original image. That is, {@code zoomX} adjusts the width and
     * {@code zoomY} adjusts the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param zoomX the scaling factor used to scale the image horizontally
     * @param zoomY the scaling factor used to scale the image vertically
     */
    public void setZoom(double zoomX, double zoomY) {
        if (getZoomX() == zoomX && getZoomY() == zoomY) {
            return;
        }

        transformations.setZoomX(zoomX);
        transformations.setZoomY(zoomY);
        fireZoomChange();
    }

    /**
     * Sets the scaling factor used to scale the image horizontally. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomX} adjusts the width of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param zoomX the scaling factor used to scale the image horizontally
     */
    public void setZoomX(double zoomX) {
        if (getZoomX() == zoomX) {
            return;
        }

        transformations.setZoomX(zoomX);
        fireZoomChange();
    }

    /**
     * Returns the scaling factor used to scale the image vertically. For
     * example: If this method returns {@code 2.0}, the image must be scaled
     * to an image which has twice the height of the original image.
     * Respectively, a value of {@code 0.5} means that the resulting image must
     * be scaled to an image which has half the height of the original image.
     * <P>
     * This method may return a negative value which should be interpreted as
     * {@link #flipVertical() flipping the image vertically} and scaling
     * with the absolute value.
     * <P>
     * The client code should also expect that this property might be zero,
     * {@code NaN} or even infinity. How these special values are handled are
     * left to the client code.
     *
     * @return the scaling factor used to scale the image vertically
     */
    public double getZoomY() {
        return transformations.getZoomY();
    }

    /**
     * Sets the scaling factor used to scale the image vertically. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomY} adjusts the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param zoomY the scaling factor used to scale the image vertically
     */
    public void setZoomY(double zoomY) {
        if (getZoomY() == zoomY) {
            return;
        }

        transformations.setZoomY(zoomY);
        fireZoomChange();
    }

    /**
     * Sets the scaling factors used to scale the image horizontally and
     * vertically to the same value. The scaling must be interpreted in the
     * coordinate system of the original image. That is, {@code zoom} adjusts
     * both the width and the height of the original image.
     * <P>
     * This method is effectively equivalent to calling:
     * {@code setZoom(zoom, zoom)}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param zoom the scaling factor used to scale the image both horizontally
     *   and vertically
     */
    public void setZoom(double zoom) {
        if (getZoomX() == zoom && getZoomY() == zoom) {
            return;
        }

        transformations.setZoom(zoom);
        fireZoomChange();
    }

    /**
     * Returns {@code true} if the image should scaled to fit the display in
     * which it is displayed. That is, this method returns the same value as
     * the following expression: {@code getZoomToFitOptions() != null}.
     * <P>
     * If the zoom to fit requirement conflicts with any of the properties
     * stored in this {@code BasicTransformationMode}, the zoom to fit mode
     * should take precedence.
     *
     * @return {@code true} if the image should scaled to fit the display in
     *   which it is displayed, {@code false} if it should be displayed
     *   according to the other properties of this
     *   {@code BasicTransformationMode}
     */
    public boolean isInZoomToFitMode() {
        return zoomToFit != null;
    }

    /**
     * Returns the set of rules which should be used when displaying the image
     * in zoom to fit mode or {@code null} if other properties of this
     * {@code BasicTransformationMode} should be used instead.
     *
     * @return the set of rules which should be used when displaying the image
     *   in zoom to fit mode or {@code null} if other properties of this
     *   {@code BasicTransformationMode} should be used instead
     */
    public Set<ZoomToFitOption> getZoomToFitOptions() {
        return zoomToFit != null
                ? EnumSet.copyOf(zoomToFit)
                : null;
    }

    /**
     * Causes subsequent {@link #getZoomToFitOptions() getZoomToFitOptions()}
     * method calls to return {@code null}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     */
    public void clearZoomToFit() {
        if (zoomToFit != null) {
            zoomToFit = null;
            fireLeaveZoomToFitMode();
        }
    }

    /**
     * Sets the zoom to fit mode with the following rules:
     * {@link ZoomToFitOption#FitHeight}, {@link ZoomToFitOption#FitWidth} and
     * with the ones specified in the arguments.
     * <P>
     * This method call is equivalent to calling:
     * {@code setZoomToFit(keepAspectRatio, magnify, true, true)}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param keepAspectRatio if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#KeepAspectRatio}
     * @param magnify if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#MayMagnify}
     */
    public void setZoomToFit(boolean keepAspectRatio, boolean magnify) {
        setZoomToFit(keepAspectRatio, magnify, true, true);
    }

    /**
     * Sets the zoom to fit mode with the specified rules.
     * <P>
     * Subsequent {@link #getZoomToFitOptions() getZoomToFitOptions()} method
     * calls will return these set of rules.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param keepAspectRatio if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#KeepAspectRatio}
     * @param magnify if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#MayMagnify}
     * @param fitWidth if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FitWidth}
     * @param fitHeight if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FitHeight}
     */
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

    /**
     * Sets the zoom to fit mode with the specified rules.
     * <P>
     * Subsequent {@link #getZoomToFitOptions() getZoomToFitOptions()} method
     * calls will return these set of rules.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param zoomToFitOptions the zoom to fit options to be returned by
     *   subsequent {@code getZoomToFitOptions()} method calls. The content of
     *   this set will be copied and this method will not store a reference to
     *   it. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public void setZoomToFit(Set<ZoomToFitOption> zoomToFitOptions) {
        ExceptionHelper.checkNotNullArgument(zoomToFitOptions, "zoomToFitOptions");

        EnumSet<ZoomToFitOption> newZoomToFit;
        newZoomToFit = EnumSet.copyOf(zoomToFitOptions);

        if (!newZoomToFit.equals(zoomToFit)) {
            zoomToFit = newZoomToFit;
            fireEnterZoomToFitMode();
        }
    }

    /**
     * Sets all the transformation properties of this
     * {@code BasicTransformationModel} from the specified
     * {@link BasicImageTransformations}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners. Note that this method does not necessarily invoke the
     * listeners only after all properties have been set. That is, the effect of
     * this method cannot be considered atomic in any way.
     *
     * @param newTransformations the {@code BasicImageTransformations} from
     *   which the properties are to be extracted. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
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
