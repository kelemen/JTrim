package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.image.transform.AffineImagePointTransformer;
import org.jtrim2.image.transform.AffineTransformationStep;
import org.jtrim2.image.transform.BasicImageTransformations;
import org.jtrim2.image.transform.ImagePointTransformer;
import org.jtrim2.image.transform.ImageTransformationStep;
import org.jtrim2.image.transform.InterpolationType;
import org.jtrim2.image.transform.TransformationStepInput;
import org.jtrim2.image.transform.TransformedImage;
import org.jtrim2.image.transform.ZoomToFitOption;
import org.jtrim2.image.transform.ZoomToFitTransformationStep;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.ui.concurrent.query.AsyncRendererFactory;

import static org.jtrim2.property.PropertyFactory.*;

/**
 * Defines a <I>Swing</I> component which is able to display an image, applying
 * a series of user defined transformations and convenience methods to apply
 * basic image transformations. This component supports every transformation
 * of the {@link BasicTransformationModel}.
 * <P>
 * Additionally, this component is able to display the image using various
 * {@link #interpolationType() interpolation types}. The default one is the
 * bilinear interpolation.
 * <P>
 * This component defines the {@link #setOffset(double, double) offset} of the
 * image so that if it is (0.0, 0.0), the center of the image will be in the
 * center of the component.
 * <P>
 * About other properties of this component see the description of
 * {@link TransformedImageDisplay}.
 *
 * <h3>Applying additional transformations</h3>
 * If you need to apply additional transformations before or after the affine
 * transformation applied by this component, you may use the
 * {@link #getAffineTransformationPos()} reference (e.g.:
 * {@code getAffineTransformationPos().addBefore}). Note however, that this
 * component assumes that transformations applied <I>after</I> the affine
 * transformation will not further adjust the coordinate transformation applied
 * to the source image (i.e., they will return the identity transformation in
 * the {@code pointTransformer} property of
 * {@link org.jtrim2.image.transform.TransformedImage TransformedImage}). That
 * is, transformations applied after the affine transformation are only expected
 * to draw some additional overlay on the image, or do some color conversion.
 * There is no such expectation on transformations applied before the affine
 * transformation.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread. Exceptions from this rule are made
 * only for reading {@link MutableProperty} and {@link PropertySource}
 * instances. This is true, even for getting those properties (in particular, it
 * is safe to call the {@link #transformations()} method from any thread).
 *
 * @param <ImageAddress> the type of the address of the image to be
 *   displayed. That is, the input of the {@link #imageQuery() image query}.
 *
 * @see TransformedImageDisplay
 */
@SuppressWarnings("serial")
public class BasicTransformedImageDisplay<ImageAddress>
extends
        TransformedImageDisplay<ImageAddress> {

    private final BasicTransformationModel transformations;
    private final MutableProperty<Boolean> alwaysClearZoomToFit;

    private final MutableProperty<InterpolationType> interpolationType;
    private final BasicTransformationProperty transformationProperties;

    private final UpdateTaskExecutor affineInputSetterExecutor;
    private final MutableProperty<ImageDimension> affineInputDimension;
    private final PropertySource<ImagePointTransformer> affineCoordTransfProperty;

    private final TransformationStepDef affineStepDef;

    private final UpdateTaskExecutor transformationUpdater;

    /**
     * Creates a {@link BasicTransformedImageDisplay} with identity
     * transformation applying bilinear transformations (the interpolation type
     * only matters if the transformation is changed to something else than
     * identity).
     * <P>
     * The {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
     * method must be called before displaying the component.
     */
    public BasicTransformedImageDisplay() {
        this(null);
    }

    /**
     * Creates a {@link BasicTransformedImageDisplay} with identity
     * transformation applying bilinear transformations (the interpolation type
     * only matters if the transformation is changed to something else than
     * identity).
     *
     * @param asyncRenderer the {@code AsyncRendererFactory} to be used to
     *   render this component. This argument can be {@code null}, in which
     *   case, the {@code AsyncRendererFactory} must be set later by the
     *   {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} method.
     */
    public BasicTransformedImageDisplay(AsyncRendererFactory asyncRenderer) {
        super(asyncRenderer);

        this.alwaysClearZoomToFit = lazilySetProperty(memProperty(false));
        this.transformations = new BasicTransformationModel();
        this.interpolationType = lazilySetProperty(memProperty(InterpolationType.BILINEAR));
        this.transformationProperties = new BasicTransformationProperty(transformations);
        this.affineInputDimension = lazilySetProperty((memProperty((ImageDimension) null, true)));
        this.affineInputSetterExecutor = SwingExecutors.getSwingUpdateExecutor(false);
        this.affineStepDef = addFirstStep();
        this.affineCoordTransfProperty = new AffineCoordinateTransformation();
        this.transformationUpdater = new GenericUpdateTaskExecutor(this::addLazyTransformationUpdater);

        initListeners();

        SwingUtilities.invokeLater(this::applyAffineTransformation);
    }

    private void initListeners() {
        Runnable applyZoomToFitTask = this::applyZoomToFit;

        imageMetaData().addChangeListener(applyZoomToFitTask);
        transformationProperties.zoomToFit().addChangeListener(applyZoomToFitTask);
        affineInputDimension.addChangeListener(applyZoomToFitTask);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyZoomToFit();
                applyAffineTransformationLazily();
            }
        });

        Runnable applyAffineTransformationLazilyTask = this::applyAffineTransformationLazily;

        interpolationType.addChangeListener(applyAffineTransformationLazilyTask);
        transformations.addChangeListener(applyAffineTransformationLazilyTask);
    }

    private void applyZoomToFit() {
        Set<ZoomToFitOption> zoomToFitOptions = getZoomToFitOptions();
        if (zoomToFitOptions == null) {
            return;
        }

        ImageDimension affineDim = affineInputDimension.getValue();
        if (affineDim == null) {
            return;
        }

        BasicImageTransformations newTransf = ZoomToFitTransformationStep.getBasicTransformations(
                affineDim.width,
                affineDim.height,
                getWidth(),
                getHeight(),
                zoomToFitOptions,
                getTransformations());

        transformations.setTransformations(newTransf);
    }

    private void applyAffineTransformationLazily() {
        transformationUpdater.execute(this::applyAffineTransformation);
    }

    private void applyAffineTransformation() {
        ImageTransformationStep transformation;

        Set<ZoomToFitOption> zoomToFitOptions = getZoomToFitOptions();
        BasicImageTransformations currentTransf = getTransformations();

        if (zoomToFitOptions != null) {
            transformation = new ZoomToFitTransformationStep(
                    currentTransf,
                    zoomToFitOptions,
                    getBackground(),
                    interpolationType.getValue());
        } else {
            boolean simpleTransformation = AffineTransformationStep.isSimpleTransformation(currentTransf);
            InterpolationType appliedInterpolation = simpleTransformation
                    ? InterpolationType.NEAREST_NEIGHBOR
                    : interpolationType.getValue();

            transformation = new AffineTransformationStep(
                    currentTransf,
                    getBackground(),
                    appliedInterpolation);
        }

        transformation = new ImageDimensionGatherer(transformation);
        affineStepDef.setTransformation(transformation);
    }

    private void setAffineInputDimension(final ImageDimension dimension) {
        affineInputSetterExecutor.execute(() -> {
            if (!Objects.equals(affineInputDimension.getValue(), dimension)) {
                affineInputDimension.setValue(dimension);
            }
        });
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        applyAffineTransformationLazily();
    }

    /**
     * Adds a listener whose appropriate method will be called when the affine
     * transformation applied to the source image changes.
     *
     * @param listener the listener whose appropriate method will be called
     *   when a property changes. This argument cannot be {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public final ListenerRef addAffineTransformationListener(TransformationListener listener) {
        return transformations.addTransformationListener(listener);
    }

    /**
     * Returns the property defining what interpolation algorithm is to be used
     * by the applied affine transformation.
     * <P>
     * The value of this property can never be {@code null}, and is
     * {@link InterpolationType#BILINEAR} by default.
     *
     * @return the property defining what interpolation algorithm is to be used
     *   by the applied affine transformation. This method may never return
     *   {@code null}.
     */
    public final MutableProperty<InterpolationType> interpolationType() {
        return interpolationType;
    }

    /**
     * Returns the position of the affine transformation applied to the source
     * image. You can use this position the add addition transformations
     * before or after the affine transformation is applied.
     * <P>
     * <B>Note</B>: You may only add transformations after the affine
     * transformation which do not further adjust the coordinate transformation
     * applied to the source image.
     *
     * @return the position of the affine transformation applied to the source
     *   image. This method never returns {@code null}.
     */
    public final TransformationStepPos getAffineTransformationPos() {
        return affineStepDef.getPosition();
    }

    /**
     * Returns a container for the properties ({@link MutableProperty}) of the
     * applied affine transformations. The returned transformation can be used
     * when generic handling of the properties are needed or the calling code
     * needs to be notified when a particular property changes.
     * <P>
     * For example, you can use {@code transformations().rotateInRadians().addChangeListener}
     * to listen for changes made to the rotation attribute.
     * <P>
     * Note that although you can also change the values of properties via the
     * reference returned by this method, it is recommended to use the setter
     * methods directly provided by {@code BasicTransformedImageDisplay}. This
     * is recommended because they offer better performance and are (usually)
     * more convenient.
     *
     * @return a container for the properties ({@link MutableProperty}) of the
     *   applied affine transformations. This method never returns {@code null}.
     */
    public final BasicTransformationProperty transformations() {
        return transformationProperties;
    }

    /**
     * Returns {@code true} if the image will be scaled to fit this component.
     * That is, this method returns the same value as the following expression:
     * {@code getZoomToFitOptions() != null}.
     * <P>
     * If the zoom to fit requirement conflicts with any of the properties
     * of this component, the zoom to fit mode will take precedence.
     *
     * @return {@code true} if the image will be scaled to fit the display in
     *   which it is displayed, {@code false} if it should be displayed
     *   according to the other properties of this component
     */
    public final boolean isInZoomToFitMode() {
        return transformations.isInZoomToFitMode();
    }

    /**
     * Returns the set of rules to be used when displaying the image
     * in zoom to fit mode or {@code null} if other properties of this
     * component will be used instead.
     *
     * @return the set of rules which will be used when displaying the image
     *   in zoom to fit mode or {@code null} if other properties of this
     *   component will be used instead
     */
    public final Set<ZoomToFitOption> getZoomToFitOptions() {
        return transformations.getZoomToFitOptions();
    }

    /**
     * Returns the property defining if this component will leave the zoom to
     * fit mode when a transformation property of this component is modified
     * even if setting the property does not conflict with the zoom to fit mode
     * (such as rotate). That is, if the value of this property is {@code true},
     * setting any property related to the affine transformation will clear the
     * zoom to fit mode. If this property is {@code false}, only properties
     * conflicting with the zoom to fit mode will cause the zoom to fit mode to
     * be disabled.
     * <P>
     * The value of this property can never be {@code null}, and its default
     * value right after constructing this {@code BasicTransformedImageDisplay}
     * is {@code false}.
     *
     * @return the property defining if this component will leave the zoom to
     *   fit mode when a transformation property of this component is modified
     *   even if setting the property does not conflict with the zoom to fit
     *   mode. This method never returns {@code null}.
     */
    public final MutableProperty<Boolean> alwaysClearZoomToFit() {
        return alwaysClearZoomToFit;
    }

    /**
     * Returns the property holding the value of the coordinate transformation,
     * transforming coordinates from coordinates of the input image to the
     * result applied affine transformation. That is, if you have no more
     * transformation applied other than the affine transformation (or only ones
     * defining identity coordinate transformation), then the
     * {@code ImagePointTransformer} of this property will transform the
     * coordinates of the source image to display coordinates of this component.
     * <P>
     * The value of this property depends on the currently set properties, not
     * on the last rendering. For example, calling {@link #setRotateInRadians(double)}
     * will have an immediate effect on the returned property (assuming that the
     * size of the input image is known).
     * <P>
     * The value of this property can be {@code null}, if the size of the input
     * image is not yet known. Once it is known (i.e.: the image has been
     * rendered at least once), this property will never revert back to
     * {@code null}, even if you change the source image.
     *
     * @return the property holding the value of the coordinate transformation,
     *   transforming coordinates from coordinates of the input image to the
     *   result applied affine transformation. This method never returns
     *   {@code null} (but its value can be {@code null}).
     */
    public final PropertySource<ImagePointTransformer> affinePointTransformer() {
        return affineCoordTransfProperty;
    }

    /**
     * Returns where the pixel of the input image passed to the affine
     * transformation is displayed in this component. The result of (0, 0) means
     * the top-left corner of this component. Note that this method may return a
     * coordinate which lies outside this component and does not need to return
     * integer coordinates.
     * <P>
     * This is simply a convenience method which relies on the
     * {@link #affinePointTransformer() affinePointTransformer()} method except
     * that if the coordinate transformation is not yet available this method
     * will return the same coordinates as specified in the argument.
     * <P>
     * Note: This method expects that transformations applied after the affine
     * transformation will not further adjust the coordinate transformation.
     *
     * @param beforeAffinePoint the coordinates of the pixel on the input image
     *   of the affine transformation to be transformed to display coordinates.
     *   This argument cannot be {@code null} but the coordinates may lay
     *   outside the boundaries of the image.
     * @return the coordinates where the specified pixel of the input image of
     *   the affine transformation is displayed. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified coordinate is
     *   {@code null}
     */
    public final Point2D getDisplayPointFromPreAffinePoint(Point2D beforeAffinePoint) {
        Objects.requireNonNull(beforeAffinePoint, "beforeAffinePoint");

        ImagePointTransformer pointTransformer = affinePointTransformer().getValue();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            pointTransformer.transformSrcToDest(beforeAffinePoint, result);
        } else {
            result.setLocation(beforeAffinePoint);
        }

        return result;
    }

    /**
     * Returns which pixel of the input image of the affine transformation is
     * displayed at the given display coordinate. The result of (0, 0) means the
     * top-left corner of the image. Note that this method may return a
     * coordinate which lies outside the boundaries of the image.
     * <P>
     * This is simply a convenience method which relies on the
     * {@link #affinePointTransformer() affinePointTransformer()} method except
     * that if the coordinate transformation is not yet available this method
     * will return the same coordinates as specified in the argument.
     *
     * @param displayPoint the coordinates of the display to be converted to
     *   image coordinates. This argument cannot be {@code null} but the
     *   coordinates may lay outside the boundaries of the display. The
     *   coordinates (0, 0) specifies the top-left corner of this component.
     * @return the coordinates of the pixel of the source image at the given
     *   display coordinate. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified coordinate is
     *   {@code null}
     * @throws IllegalStateException thrown if the transformation applied is
     *   not invertible
     */
    public final Point2D getPreAffinePoint(Point2D displayPoint) {
        Objects.requireNonNull(displayPoint, "displayPoint");

        ImagePointTransformer pointTransformer = affinePointTransformer().getValue();

        Point2D result = new Point2D.Double();

        if (pointTransformer != null) {
            try {
                pointTransformer.transformDestToSrc(displayPoint, result);
            } catch (NoninvertibleTransformException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            result.setLocation(displayPoint);
        }

        return result;
    }

    /**
     * Sets the {@link #setOffset(double, double) offset} of the displayed
     * image, so that the given pixel of the input image of the affine
     * transformation will be displayed above the given coordinates of this
     * component.
     * <P>
     * If the coordinate transformation is not yet available (i.e.,
     * {@link #affinePointTransformer() affinePointTransformer().getValue()}
     * returns {@code null}), this method is a no-op.
     * <P>
     * Note that if this method ends up setting the offset of the displayed
     * image, it will clear the zoom to fit mode (i.e.: calls
     * {@code clearZoomToFit()}).
     *
     * @param preAffinePoint the coordinates of the input image of the affine
     *   transformation to be displayed at the specified position. This argument
     *   cannot be {@code null}. These coordinates may lay outside the
     *   boundaries of the image.
     * @param displayPoint the coordinates of this component where the specified
     *   pixel of the source image need to be displayed. These coordinates
     *   may lay outside the boundaries of this component.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public final void movePreAffinePointToDisplayPoint(
            Point2D preAffinePoint,
            Point2D displayPoint) {

        Objects.requireNonNull(preAffinePoint, "preAffinePoint");
        Objects.requireNonNull(displayPoint, "displayPoint");

        ImagePointTransformer pointTransformer = affinePointTransformer().getValue();

        if (pointTransformer != null) {
            double offsetErrX;
            double offsetErrY;

            Point2D currentDisplayPoint = new Point2D.Double();
            pointTransformer.transformSrcToDest(preAffinePoint, currentDisplayPoint);

            offsetErrX = displayPoint.getX() - currentDisplayPoint.getX();
            offsetErrY = displayPoint.getY() - currentDisplayPoint.getY();

            double currentOffsetX = transformations.getOffsetX();
            double currentOffsetY = transformations.getOffsetY();

            double newOffsetX = currentOffsetX + offsetErrX;
            double newOffsetY = currentOffsetY + offsetErrY;

            setOffset(newOffsetX, newOffsetY);
        }
    }

    /**
     * Returns the snapshot of the currently set transformations. The return
     * value does not include to currently set zoom to fit mode.
     *
     * @return the snapshot of the currently set transformations. This method
     *   never returns {@code null}.
     */
    public final BasicImageTransformations getTransformations() {
        return transformations.getTransformations();
    }

    /**
     * Sets the applied transformations to the identity transformations. This
     * method will also remove the zoom to fit mode if set.
     */
    public final void setDefaultTransformations() {
        clearZoomToFit();
        transformations.setTransformations(BasicImageTransformations.identityTransformation());
    }

    /**
     * Sets all the transformation properties of this component from the
     * specified {@link BasicImageTransformations}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners. Note that this method does not necessarily invoke the
     * listeners only after all properties have been set. That is, the effect of
     * this method cannot be considered atomic in any way.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the transformations.
     *
     * @param transformations the {@code BasicImageTransformations} from
     *   which the properties are to be extracted. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final void setTransformations(BasicImageTransformations transformations) {
        clearZoomToFit();
        this.transformations.setTransformations(transformations);
    }

    /**
     * Sets the scaling factor used to scale the image vertically. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomY} adjusts the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoomY the scaling factor used to scale the image vertically
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoomY(double zoomY) {
        clearZoomToFit();
        transformations.setZoomY(zoomY);
    }

    /**
     * Sets the scaling factor used to scale the image horizontally. The scaling
     * must be interpreted in the coordinate system of the original image.
     * That is, {@code zoomX} adjusts the width of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoomX the scaling factor used to scale the image horizontally
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoomX(double zoomX) {
        clearZoomToFit();
        transformations.setZoomX(zoomX);
    }

    /**
     * Sets the scaling factors used to scale the image horizontally and
     * vertically to the same value. The scaling must be interpreted in the
     * coordinate system of the original image. That is, {@code zoom} adjusts
     * both the width and the height of the original image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param zoom the scaling factor used to scale the image both horizontally
     *   and vertically
     *
     * @see TransformationListener#zoomChanged()
     */
    public final void setZoom(double zoom) {
        clearZoomToFit();
        transformations.setZoom(zoom);
    }

    /**
     * Sets the angle meaning how much the image need to be rotated around
     * its center in radians. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Note: Settings this property also changes the value returned by the
     * {@code getTransformations().getRotateInDegrees()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if
     * {@link #alwaysClearZoomToFit() alwaysClearZoomToFit} has been set to
     * {@code true}.
     *
     * @param radians the angle meaning how much the image need to be rotated
     *   around its center in radians. Note that this property is stored in a
     *   normalized form. So this property will be set to value between
     *   0 and {@code 2*pi} or {@code NaN}.
     *
     * @see TransformationListener#rotateChanged()
     */
    public final void setRotateInRadians(double radians) {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.setRotateInRadians(radians);
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
     * {@code getTransformations().getRotateInRadians()} method.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if
     * {@link #alwaysClearZoomToFit() alwaysClearZoomToFit} has been set to
     * {@code true}.
     *
     * @param degrees the angle meaning how much the image need to be rotated
     *   around its center in degrees. Note that this property is stored in a
     *   normalized form. So this property will be set to value which is greater
     *   than or equal to zero and less than (not equal) to 360.
     *
     * @see TransformationListener#rotateChanged()
     */
    public final void setRotateInDegrees(int degrees) {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.setRotateInDegrees(degrees);
    }

    /**
     * Sets both the horizontal an vertical offset need to be applied on the
     * image.
     * <P>
     * Note that these offsets are to be applied after zoom has been applied.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method always calls {@code clearZoomToFit()} prior to setting
     * the property.
     *
     * @param offsetX the offset along the horizontal axis. Subsequent
     *   {@code getTransformations().getOffsetX()} method calls will return this
     *   value.
     * @param offsetY the offset along the vertical axis. Subsequent
     *   {@code getTransformations().getOffsetY()} method calls will return this
     *   value.
     *
     * @see TransformationListener#offsetChanged()
     */
    public final void setOffset(double offsetX, double offsetY) {
        clearZoomToFit();
        transformations.setOffset(offsetX, offsetY);
    }

    /**
     * Sets if the image should be flipped vertically. That is, if the top
     * side of the image should become the bottom side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if
     * {@link #alwaysClearZoomToFit() alwaysClearZoomToFit} has been set to
     * {@code true}.
     *
     * @param flipVertical if {@code true} the image should be flipped
     *   vertically, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public final void setFlipVertical(boolean flipVertical) {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.setFlipVertical(flipVertical);
    }

    /**
     * Sets if the image should be flipped horizontally. That is, if the left
     * side of the image should become the right side of the image.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if
     * {@link #alwaysClearZoomToFit() alwaysClearZoomToFit} has been set to
     * {@code true}.
     *
     * @param flipHorizontal if {@code true} the image should be flipped
     *   horizontally, set it to {@code false} otherwise
     *
     * @see TransformationListener#flipChanged()
     */
    public final void setFlipHorizontal(boolean flipHorizontal) {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.setFlipHorizontal(flipHorizontal);
    }

    /**
     * Inverts the {@link #setFlipVertical(boolean) "FlipVertical" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipVertical(!getTransformations().isFlipVertical())}.
     * <P>
     * This method will notify the appropriate listeners.
     *
     * @see TransformationListener#flipChanged()
     */
    public final void flipVertical() {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.flipVertical();
    }

    /**
     * Inverts the {@link #setFlipHorizontal(boolean) "FlipHorizontal" property}.
     * Calling this method is effectively equivalent to calling:
     * {@code setFlipHorizontal(!getTransformations().isFlipHorizontal())}.
     * <P>
     * This method will notify the appropriate listeners.
     * <P>
     * This method will {@code clearZoomToFit()} prior to setting
     * the property, if and only, if the
     * {@link #alwaysClearZoomToFit() alwaysClearZoomToFit} has been
     * set to {@code true}.
     *
     * @see TransformationListener#flipChanged()
     */
    public final void flipHorizontal() {
        if (alwaysClearZoomToFit.getValue()) {
            clearZoomToFit();
        }
        transformations.flipHorizontal();
    }

    /**
     * Removes this component from zoom to fit mode. That is, the image
     * displayed is no longer required to fit the display.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @see TransformationListener#leaveZoomToFitMode()
     */
    public final void clearZoomToFit() {
        transformations.clearZoomToFit();
    }

    /**
     * Sets the zoom to fit mode with the following rules:
     * {@link ZoomToFitOption#FIT_HEIGHT}, {@link ZoomToFitOption#FIT_WIDTH} and
     * with the ones specified in the arguments.
     * <P>
     * This method call is equivalent to calling:
     * {@code setZoomToFit(keepAspectRatio, magnify, true, true)}.
     * <P>
     * If this method changes anything, it will invoke the appropriate
     * listeners.
     *
     * @param keepAspectRatio if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#KEEP_ASPECT_RATIO}
     * @param magnify if {@code true} the zoom to fit mode will also
     *   include the rule: {@link ZoomToFitOption#MAY_MAGNIFY}
     *
     * @see TransformationListener#enterZoomToFitMode(Set)
     */
    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify) {
        transformations.setZoomToFit(keepAspectRatio, magnify);
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
     *   include the rule: {@link ZoomToFitOption#KEEP_ASPECT_RATIO}
     * @param magnify if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#MAY_MAGNIFY}
     * @param fitWidth if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FIT_WIDTH}
     * @param fitHeight if {@code true} the zoom to fit mode will
     *   include the rule: {@link ZoomToFitOption#FIT_HEIGHT}
     */
    public final void setZoomToFit(boolean keepAspectRatio, boolean magnify, boolean fitWidth, boolean fitHeight) {
        transformations.setZoomToFit(keepAspectRatio, magnify, fitWidth, fitHeight);
    }

    private final class AffineCoordinateTransformation
    implements
            PropertySource<ImagePointTransformer> {

        @Override
        public ImagePointTransformer getValue() {
            ImageDimension inputDim = affineInputDimension.getValue();
            if (inputDim == null) {
                return null;
            }

            BasicImageTransformations transf = transformations.getTransformations();

            int srcWidth = inputDim.width;
            int srcHeight = inputDim.height;

            int destWidth = getWidth();
            int destHeight = getHeight();

            AffineTransform transfMatrix = AffineTransformationStep.getTransformationMatrix(
                    transf,
                    srcWidth,
                    srcHeight,
                    destWidth,
                    destHeight);
            return new AffineImagePointTransformer(transfMatrix);
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            final ComponentListener resizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    listener.run();
                }
            };

            addComponentListener(resizeListener);
            ListenerRef ref1 = () -> {
                removeComponentListener(resizeListener);
            };
            ListenerRef ref2 = transformations.addChangeListener(listener);
            ListenerRef ref3 = affineInputDimension.addChangeListener(listener);
            return ListenerRefs.combineListenerRefs(ref1, ref2, ref3);
        }
    }

    private static final class ImageDimension {
        public final int width;
        public final int height;

        public ImageDimension(BufferedImage image) {
            this(image.getWidth(), image.getHeight());
        }

        public ImageDimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        // We will only call the equals method and we won't compare this object
        // to anything but another ImageDimension. Despite this, we implement
        // equals and hashCode properly.
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + this.width;
            hash = 29 * hash + this.height;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ImageDimension other = (ImageDimension) obj;
            return this.width == other.width && this.height == other.height;
        }
    }

    private final class ImageDimensionGatherer implements ImageTransformationStep {
        private final ImageTransformationStep wrapped;

        public ImageDimensionGatherer(ImageTransformationStep wrapped) {
            assert wrapped != null;
            this.wrapped = wrapped;
        }

        @Override
        public TransformedImage render(
                CancellationToken cancelToken,
                TransformationStepInput input,
                BufferedImage offeredBuffer) {

            BufferedImage inputImage = input.getInputImage().getImage();

            if (inputImage != null) {
                setAffineInputDimension(new ImageDimension(inputImage));
            }

            return wrapped.render(cancelToken, input, offeredBuffer);
        }
    }
}
