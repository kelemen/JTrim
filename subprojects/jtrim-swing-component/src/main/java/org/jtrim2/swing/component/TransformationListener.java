package org.jtrim2.swing.component;

import java.util.Set;
import org.jtrim2.image.transform.ZoomToFitOption;

/**
 * The interface to listen for changes in the transformation applied to an
 * image. This interface was designed to use by the
 * {@link BasicTransformationModel}.
 *
 * <h3>Thread safety</h3>
 * Instances of this listener interface can only be notified from the
 * <I>AWT Event Dispatch Thread</I> and therefore does not need to be
 * thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface does not need to be
 * <I>synchronization transparent</I> but they must be aware that they are
 * called from the <I>AWT Event Dispatch Thread</I>.
 *
 * @see BasicTransformationModel
 */
public interface TransformationListener {
    /**
     * Invoked when the scaling property of the image has changed. When using
     * {@link BasicTransformationModel}, this means that either the
     * {@link BasicTransformationModel#getZoomX() ZoomX} or the
     * {@link BasicTransformationModel#getZoomY() ZoomY} property has changed
     * (or both).
     * <P>
     * This method is called only after the change took place.
     */
    public void zoomChanged();

    /**
     * Invoked when the offset used to display image has changed. When using
     * {@link BasicTransformationModel}, this means that either the
     * {@link BasicTransformationModel#getOffsetX() OffsetX} or the
     * {@link BasicTransformationModel#getOffsetY() OffsetY} property has
     * changed (or both).
     * <P>
     * This method is called only after the change took place.
     */
    public void offsetChanged();

    /**
     * Invoked when the image has been flipped vertically or horizontally. When
     * using {@link BasicTransformationModel}, this means that either the
     * {@link BasicTransformationModel#isFlipHorizontal() FlipHorizontal} or the
     * {@link BasicTransformationModel#isFlipVertical() FlipVertical} property
     * has changed (or both).
     * <P>
     * This method is called only after the change took place.
     */
    public void flipChanged();

    /**
     * Invoked when the image has been rotated. When using
     * {@link BasicTransformationModel}, this means that either the
     * {@link BasicTransformationModel#getRotateInDegrees() RotateInDegrees} or
     * the {@link BasicTransformationModel#getRotateInRadians() RotateInRadians}
     * property has changed (or both).
     * <P>
     * This method is called only after the change took place.
     */
    public void rotateChanged();

    /**
     * Invoked when the image should be drawn to fit the display. When using
     * {@link BasicTransformationModel}, this means that one of the
     * {@link BasicTransformationModel#setZoomToFit(Set) setZoomToFit} methods
     * has been called and does change the {@code ZoomToFit} property.
     * <P>
     * This method is called only after the change took place.
     *
     * @param options the zoom to fit rules needed to display the image. This
     *   argument cannot be {@code null}. This argument is the rules to which
     *   the {@code ZoomToFit} property was set but note that listeners
     *   previously called might have already adjusted the {@code ZoomToFit}
     *   property. If this is the case, the listener will be notified in another
     *   {@code enterZoomToFitMode} method call.
     */
    public void enterZoomToFitMode(Set<ZoomToFitOption> options);

    /**
     * Invoked when the image no longer need to be drawn to fit the display.
     * When using {@link BasicTransformationModel}, this means that the
     * {@link BasicTransformationModel#clearZoomToFit()} method has been called
     * and it needed to actually clear the {@code ZoomToFit} property.
     */
    public void leaveZoomToFitMode();
}
