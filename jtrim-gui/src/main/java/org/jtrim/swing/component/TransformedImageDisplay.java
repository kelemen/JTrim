package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cache.GenericReference;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.MultiAsyncDataState;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageMetaData;
import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.ImagePointTransformer;
import org.jtrim.image.transform.SerialImagePointTransformer;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.PropertyVerifier;
import org.jtrim.swing.concurrent.async.AsyncRenderer;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.BasicRenderingArguments;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.utils.TimeDuration;

/**
 * Defines a <I>Swing</I> component which is able to display an image applying
 * a series of user defined transformations.
 * <P>
 * There are three kind of properties you need to specify for this component:
 * <ul>
 *  <li>
 *   The address of the image to be displayed. The address will be specified
 *   for the query used to retrieve the image. So the address can be any type,
 *   for example: {@code java.net.URI}. The address can be set by the
 *   {@link #getImageAddress() imageAddress property}.
 *  </li>
 *  <li>
 *   The {@link AsyncDataQuery} used to retrieve the image. The image is
 *   retrieved by the address specified previously. The query can be set by
 *   the {@link #getImageQuery() imageQuery property}.
 *   methods.
 *  </li>
 *  <li>
 *   The transformations which will be used to transform the image retrieved by
 *   the image query. The first transformation must be added by a call to the
 *   {@link #addFirstStep()} method subsequent transformations must be added
 *   through this step through the {@link TransformationStepPos} interface. The
 *   {@code addFirstStep} method is recommended to be called by the constructor
 *   of the extending class, which ensures that noone else might call this
 *   method successfully.
 *  </li>
 * </ul>
 * <P>
 * Note that this component is an {@link TransformedImageDisplay} and relies on
 * an {@link AsyncRenderer}. Therefore it must be set before displaying this
 * component, either by passing an {@link AsyncRendererFactory} to the
 * appropriate constructor or by
 * {@link #setAsyncRenderer(AsyncRendererFactory) setting it later}.
 * <P>
 * An example implementation which contains a single template transformation
 * is shown here:
 * <pre>
 * class SampleDisplay&lt;ImageAddress&gt;
 * extends
 *         TransformedImageDisplay&lt;ImageAddress&gt; {
 *     private final TransformationStepDef defaultStep;
 *
 *     public SampleDisplay(AsyncRendererFactory asyncRenderer) {
 *         super(asyncRenderer);
 *
 *         defaultStep = addFirstStep();
 *         defaultStep.setTransformation(new ImageTransformationStep() {
 *             &#064;Override
 *             public TransformedImage render(
 *                     CancellationToken cancelToken,
 *                     TransformationStepInput input,
 *                     BufferedImage offeredBuffer) {
 *                 BufferedImage inputImage = input.getInputImage().getImage();
 *                 if (inputImage == null) {
 *                     return new TransformedImage(null, null);
 *                 }
 *
 *                 int destWidth = input.getDestinationWidth();
 *                 int destHeight = input.getDestinationHeight();
 *                 if (offeredBuffer == null
 *                         || offeredBuffer.getWidth() != destWidth
 *                         || offeredBuffer.getHeight() != destHeight) {
 *                     offeredBuffer = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_ARGB);
 *                 }
 *
 *                 Graphics2D g = offeredBuffer.createGraphics();
 *                 try {
 *                     // TODO: Draw something to the output based on the input.
 *                     // The input you want is most likely the "inputImage".
 *                 } finally {
 *                     g.dispose();
 *                 }
 *                 return new TransformedImage(offeredBuffer, null);
 *             }
 *         });
 *     }
 *
 *     public final TransformationStepPos getDefaultStep() {
 *         return defaultStep.getPosition();
 *     }
 * }
 * </pre>
 * You may add subsequent transformations to this component by using the
 * {@code TransformationStepPos} returned by the {@code getDefaultStep} method.
 * Transformations can be added either after or before the initial
 * transformation.
 * <P>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread after made displayable. Note however
 * that {@link MutableProperty} and {@link PropertySource} instances can be read
 * by any thread (even concurrently with writes to the property).
 *
 * @param <ImageAddress> the type of the address of the image to be
 *   displayed. That is, the input of the
 *   {@link #setImageQuery(AsyncDataQuery, Object) image query}.
 *
 * @see SimpleAsyncImageDisplay
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial")
public abstract class TransformedImageDisplay<ImageAddress> extends AsyncRenderingComponent {
    // TODO: Make this configurable.
    private static final ReferenceType TMP_BUFFER_REFERENCE_TYPE = ReferenceType.WeakRefType;

    private static final int RENDERING_STATE_POLL_TIME_MS = 100;
    private static final TimeDuration DEFAULT_OLD_IMAGE_HIDE = new TimeDuration(1000, TimeUnit.MILLISECONDS);

    private final RefList<PreparedOutputBufferStep> steps;
    private final ListenerManager<Runnable> transformationChangeListeners;

    private boolean preparedStep;
    private volatile List<PreparedOutputBufferStep> stepsSnapshot;

    private final MutableProperty<AsyncDataQuery<? super ImageAddress, ? extends ImageResult>> imageQuery;
    private final MutableProperty<ImageAddress> imageAddress;
    private final MutableProperty<AsyncDataLink<? extends ImageResult>> imageSource;
    private final MutableProperty<ImageMetaData> imageMetaData;
    private final MutableProperty<Boolean> imageShown;
    private final MutableProperty<TimeDuration> oldImageHideTime;
    private final MutableProperty<TimeDuration> longRenderingTimeout;
    private final MutableProperty<ImagePointTransformer> displayedPointTransformer;

    private long imageReplaceTime;
    private long imageShownTime;

    private boolean preparedRenderingArgs;

    /**
     * Creates a new {@code TransformedImageDisplay} without setting the
     * {@link AsyncRenderer} to be used. Therefore the
     * {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} must be
     * called before displaying the component.
     */
    public TransformedImageDisplay() {
        this(null);
    }

    /**
     * Creates a new {@code TransformedImageDisplay} using an {@link AsyncRenderer}
     * created by the specified {@link AsyncRendererFactory}. Note however, that
     * if you pass {@code null} for the argument of this constructor, you still
     * have to call the {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
     * method before displaying the component.
     *
     * @param asyncRenderer the {@code AsyncRendererFactory} to be used to
     *   render this component. This argument can be {@code null}, in which
     *   case, the {@code AsyncRendererFactory} must be set later by the
     *   {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} method.
     */
    public TransformedImageDisplay(AsyncRendererFactory asyncRenderer) {
        super(asyncRenderer);

        this.transformationChangeListeners = new CopyOnTriggerListenerManager<>();
        this.steps = new RefLinkedList<>();
        this.preparedStep = false;
        this.stepsSnapshot = Collections.emptyList();

        this.imageMetaData = PropertyFactory.memProperty(null, true);
        this.imageQuery = PropertyFactory.memProperty(null, new ImageQueryVerifier());
        this.imageAddress = PropertyFactory.memProperty(null, new ImageAddressVerifier());
        this.imageSource = PropertyFactory.memProperty(null, true);
        this.imageShown = PropertyFactory.memProperty(false);
        this.oldImageHideTime = PropertyFactory.memProperty(DEFAULT_OLD_IMAGE_HIDE);
        this.longRenderingTimeout = PropertyFactory.memProperty(null, true);
        this.displayedPointTransformer = PropertyFactory.memProperty(null, true);

        this.imageReplaceTime = System.nanoTime();
        this.imageShownTime = imageReplaceTime;

        this.preparedRenderingArgs = false;

        setRenderingArgs();

        // Must be called as the last instruction of the constructor
        addInitialListeners();
    }

    private void addInitialListeners() {
        addPrePaintListener(new Runnable() {
            @Override
            public void run() {
                prepareRenderingArgs();
            }
        });

        imageSource.addChangeListener(new Runnable() {
            @Override
            public void run() {
                invalidateRenderingArgs();
            }
        });

        Runnable setImageSourceAction = new Runnable() {
            @Override
            public void run() {
                setImageSource();
            }
        };
        imageQuery.addChangeListener(setImageSourceAction);
        imageAddress.addChangeListener(setImageSourceAction);

        Runnable repaintTask = new Runnable() {
            @Override
            public void run() {
                repaint();
            }
        };
        oldImageHideTime.addChangeListener(repaintTask);
        longRenderingTimeout.addChangeListener(repaintTask);
    }

    /**
     * Adds a listener which is to be notified after the transformation
     * applied to retrieved image is changed.
     *
     * @param listener the {@code listener} whose {@code run} method is called
     *   on chage. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which might be used to remove the
     *   currently added listener. This method never returns {@code null}.
     */
    public final ListenerRef addTransformationChangeListener(Runnable listener) {
        return transformationChangeListeners.registerListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        invalidateRenderingArgs();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        invalidateRenderingArgs();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        invalidateRenderingArgs();
    }

    /**
     * Returns the property defining the coordinate transformation between image
     * coordinates and display coordinates according to the currently displayed
     * image.
     * <P>
     * The source coordinate system of the returned transformation is the
     * coordinate system of the source image and destination coordinate system
     * is the coordinate system of this component. That is, transforming a
     * coordinate from the source coordinates to the destination coordinate will
     * transform the location of a pixel on the source image to the location of
     * that pixel on this component. Note that the result may lay outside this
     * component's bounds.
     * <P>
     * This method always return the coordinate transformation according what
     * is currently displayed on this component which may differ from the one
     * which could be deduced from the currently set properties.
     *
     * @return the coordinate transformation between image coordinates and
     *   display coordinates according to the currently displayed image. This
     *   method never returns {@code null} but the value of the returned
     *   property can be {@code null} if it is not yet available.
     */
    public final PropertySource<ImagePointTransformer> getDisplayedPointTransformer() {
        return PropertyFactory.protectedView(displayedPointTransformer);
    }

    /**
     * Returns the property containing the meta-data of the last retrieved
     * image. The value of the returned property can be {@code null} if it is
     * not available. Note that if this component is never displayed, no attempt
     * will be made to fetch the image, therefore the value of this property
     * will always return {@code null}.
     * <P>
     * The value of this property can be {@code null}, if the meta-data is not
     * available.
     *
     * @return the meta-data of the last retrieved image. This method never
     *   returns {@code null} but the value of the returned property can be
     *   {@code null} if it is not yet available.
     */
    public final PropertySource<ImageMetaData> getImageMetaData() {
        return PropertyFactory.protectedView(imageMetaData);
    }

    /**
     * Returns the property defining if this component is currently displaying
     * an image which was fetched from the currently set image query and address
     * or not.
     * <P>
     * The value of this property cannot be {@code null}.
     *
     * @return the property defining if this component is currently displaying
     *   an image which was fetched from the currently set image query and
     *   address or not. This method never returns {@code null} and the value of
     *   the returned property is never {@code null}. In case the image from the
     *   currently set query is being displayed on this component, the value of
     *   the property is {@code true}, otherwise {@code false}.
     */
    public final PropertySource<Boolean> getImageShown() {
        return PropertyFactory.protectedView(imageShown);
    }

    /**
     * Returns the property which defines how much time must elapse after a
     * previously shown image must be cleared from this component after changing
     * the source image. That is, setting this property prevents the user to see
     * the previous image shown for a long time, if the new image takes too much
     * time to be displayed.
     * <P>
     * The value of this property cannot be {@code null}.
     *
     * @return the property which defines how much time must elapse after a
     *   previously shown image must be cleared from this component after
     *   changing the source image. This method never returns {@code null} and
     *   the value of this property cannot be {@code null}.
     */
    public final MutableProperty<TimeDuration> getOldImageHideTime() {
        return oldImageHideTime;
    }

    private long getOldImageHideTimeNanos() {
        return oldImageHideTime.getValue().toNanos();
    }

    /**
     * Returns the property which defines the time duration which is considered
     * long for rendering this component. After this timeout elapses, the
     * {@link #displayLongRenderingState(Graphics2D, MultiAsyncDataState) displayLongRenderingState}
     * method will be called periodically to render something (e.g.: progress or
     * a "please wait" text) on this component.
     * <P>
     * The value of this property can be {@code null}, which means that the
     * value of this property is infinite. This is similar to setting this
     * property to an extremely long duration (like years) except that the
     * "infinite" case is implemented more efficiently.
     *
     * @return the property which defines the time duration which is considered
     *   long for rendering this component. This method never returns
     *   {@code null} but its value can be {@code null} if the timeout is to be
     *   considered infinite.
     */
    public final MutableProperty<TimeDuration> getLongRenderingTimeout() {
        return longRenderingTimeout;
    }

    /**
     * Returns the time since the source image of this component has been
     * changed in the given time unit. This can either occur due to changing the
     * {@link #setImageAddress(Object) image address} or due to changing the
     * {@link #setImageQuery(AsyncDataQuery, Object) image query}.
     *
     * @param timeunit the time unit in which to result is to be returned.
     *   This argument cannot be {@code null}.
     * @return the time since the source image of this component has been
     *   changed
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final long getTimeSinceImageChange(TimeUnit timeunit) {
        return timeunit.convert(System.nanoTime() - imageReplaceTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the time since the last rendering of this component in the given
     * time unit. This method only cares about image retrieval and image
     * transformation, simply calling {@code repaint} has no effect on this
     * method.
     *
     * @param timeunit the time unit in which to result is to be returned.
     *   This argument cannot be {@code null}.
     * @return the time since the last rendering of this component
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final long getTimeSinceLastImageShow(TimeUnit timeunit) {
        return timeunit.convert(System.nanoTime() - imageShownTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the address of the image to be passed to the current image
     * {@link #getImageQuery()}.
     * <P>
     * You may set this property to {@code null} and if it is {@code null},
     * no image will be displayed by this component. That is, if this property
     * is {@code null}, this component will be painted with the background color
     * of this component.
     * <B>Note</B>: When the {@link #getImageQuery() image query} is
     * {@code null}, the image address property must also be {@code null}.
     *
     * @return the address of the image to be passed to the current image
     *   {@link #getImageQuery()}. This method never returns {@code null}.
     */
    public final MutableProperty<ImageAddress> getImageAddress() {
        return imageAddress;
    }

    /**
     * Returns the image query used to retrieve the image to be displayed.
     * The image provided by the image query will be transformed by the
     * applied transformations.
     * <P>
     * You may set this property to {@code null} and if it is {@code null},
     * no image will be displayed by this component. That is, if this property
     * is {@code null}, this component will be painted with the background color
     * of this component.
     * <P>
     * <B>Note</B>: When the image query is {@code null}, the
     * {@link #getImageAddress() image address} property must also be
     * {@code null}.
     *
     * @return the image query property used to retrieve the image to be
     *   displayed. This method never returns {@code null}.
     */
    public final MutableProperty<AsyncDataQuery<? super ImageAddress, ? extends ImageResult>> getImageQuery() {
        return imageQuery;
    }

    /**
     * Returns the current source of image to be displayed by this component.
     * <P>
     * This property can be {@code null}, in which case no image will be
     * displayed by this component.
     *
     * @return the current source of image to be displayed by this component.
     *   This method never returns {@code null}.
     */
    public final PropertySource<AsyncDataLink<? extends ImageResult>> getImageSource() {
        return PropertyFactory.protectedView(imageSource);
    }

    /**
     * Called when an exception occurred while trying to retrieve the image
     * or during rendering the image. This method may update the specified
     * drawing surface.
     * <P>
     * This method is called in the context of the {@link AsyncRenderer} of this
     * component and may do some more expensive computation without blocking the
     * input of the user.
     * <P>
     * This method may be overridden in subclasses. The default implementation
     * displays the exception message in the upper left corner of the drawing
     * surface (which is the upper left corner of this component).
     *
     * @param renderingArgs the properties of this component at the time when
     *   the rendering has been requested. This argument cannot be {@code null}.
     * @param drawingSurface the {@code BufferedImage} which needs to be updated
     *   to display the error. This argument cannot be {@code null}.
     * @param exception the exception describing the reason of failure. Note
     *   that, this exception might have causes (and suppressed exceptions)
     *   which need to be inspected to fully understand the causes.
     */
    protected void onRenderingError(
            BasicRenderingArguments renderingArgs,
            BufferedImage drawingSurface,
            Throwable exception) {

        Graphics2D g = drawingSurface.createGraphics();
        try {
            g.setColor(renderingArgs.getBackgroundColor());
            g.fillRect(0, 0, drawingSurface.getWidth(), drawingSurface.getHeight());

            g.setColor(renderingArgs.getForegroundColor());
            g.setFont(renderingArgs.getFont());

            String errorText = "Error: " + exception.getMessage();
            RenderHelper.drawMessage(g, errorText);
        } finally {
            g.dispose();
        }
    }

    private void invalidateRenderingArgs() {
        preparedRenderingArgs = false;
        transformationChangeListeners.onEvent(RunnableDispatcher.INSTANCE, null);
        repaint();
    }

    private void setImageSource() {
        AsyncDataQuery<? super ImageAddress, ? extends ImageResult> queryValue = imageQuery.getValue();
        ImageAddress addressValue = imageAddress.getValue();

        AsyncDataLink<? extends ImageResult> newLink = queryValue != null && addressValue != null
                ? queryValue.createDataLink(addressValue)
                : null;

        if (newLink != imageSource.getValue()) {
            imageMetaData.setValue(null);
            imageShown.setValue(false);
            imageSource.setValue(newLink);
        }
    }

    private void prepareRenderingArgs() {
        prepareSteps();

        if (!preparedRenderingArgs) {
            preparedRenderingArgs = true;
            setRenderingArgs();
        }
    }

    private void prepareSteps() {
        if (!preparedStep) {
            preparedStep = true;
            stepsSnapshot = Collections.unmodifiableList(new ArrayList<>(steps));
        }
    }

    private void setRenderingArgs() {
        BasicRenderingArguments basicArgs = new BasicRenderingArguments(this);

        AsyncDataLink<? extends ImageResult> dataLink = imageSource.getValue();
        setRenderingArgs(dataLink, new RendererImpl(dataLink, basicArgs), new PaintHook<PaintResult>() {
            @Override
            public boolean prePaintComponent(RenderingState state, Graphics2D g) {
                // We do this to fill the possible remainder of this component
                // with the background color until the asynchronous renderer
                // fills the gap on size change.
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                return true;
            }

            @Override
            public void postPaintComponent(RenderingState state, PaintResult renderingResult, Graphics2D g) {
                if (imageSource.getValue() == null) {
                    inheritedPaintDefault(state, g);
                }
                postRendering(state, renderingResult, g);
            }
        });
    }

    private void invalidateTransformations() {
        preparedStep = false;
        repaint();
    }

    /**
     * Returns an {@code ImageTransformationStep} which delegates its call
     * to the specified {@code ImageTransformationStep} but caches its result if
     * possible. Caching might be done with weak, soft or hard references.
     * <P>
     * Caching is done by retaining a reference to both the input and the output
     * of the passed {@code ImageTransformationStep} and whenever a new input
     * is available checks if it is the same as the one cached. If the inputs
     * are matching, then the cached result is returned.
     *
     * @param refType the type of the reference used to retain both the input
     *   and the output of the cached {@code ImageTransformationStep}. For this
     *   argument {@link ReferenceType#UserRefType} is equivalent to
     *   {@link ReferenceType#NoRefType}. This argument cannot be {@code null}.
     * @param step the {@code ImageTransformationStep} to which calls are
     *   delegated to if the cached output is not available or out-of-date. This
     *   argument cannot be {@code null}.
     * @param cacheCmp the comparison which is able to tell if the specified
     *   {@code ImageTransformationStep} will yield the same output for two
     *   inputs or not. This comparison should be quick and may return
     *   {@code false} if the exact check would be too slow. That is, the
     *   comparison is expected to do little more work than comparing
     *   references. This argument cannot be {@code null}.
     * @return the {@code ImageTransformationStep} which delegates its call to
     *   the specified {@code ImageTransformationStep} but caches its result if
     *   possible. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see TransformationStepDef#setTransformation(ImageTransformationStep)
     */
    public static ImageTransformationStep cachedStep(
            ReferenceType refType,
            ImageTransformationStep step,
            ImageTransformationStep.InputCmp cacheCmp) {
        return new CachingImageTransformationStep(refType, step, cacheCmp);
    }

    /**
     * Adds the first transformation step used to transform the source image.
     * Subclasses should call this method to add the first step for transforming
     * the source image and later use {@link TransformationStepDef#getPosition()}
     * to apply additional transformations before or after the first
     * transformation step.
     * <P>
     * This method may only be called once and it is recommended for subclasses
     * to call this method in the constructor.
     *
     * @return the first transformation step used to transform the source image.
     *   This method never returns {@code null}.
     *
     * @throws IllegalStateException thrown if this method was attempted to
     *   be called more than once
     */
    protected final TransformationStepDef addFirstStep() {
        if (!steps.isEmpty()) {
            throw new IllegalStateException("This method may only be called once.");
        }

        RefList.ElementRef<PreparedOutputBufferStep> stepRef = steps.addFirstGetReference(null);

        invalidateTransformations();
        return new StepDefImpl(stepRef);
    }

    private void inheritedPaintDefault(RenderingState state, Graphics2D g) {
        super.paintDefault(state, g);
    }

    /**
     * Clears the passed {@code Graphics2D} object with currently specified
     * background color and does some other bookkeeping required by this
     * component.
     *
     * @param state the state of the current asynchronous rendering, or
     *   {@code null} if there is no rendering in progress
     * @param g the {@code Graphics2D} object to be cleared with the background
     *   color. This argument cannot be {@code null}.
     */
    @Override
    protected final void paintDefault(RenderingState state, Graphics2D g) {
        inheritedPaintDefault(state, g);

        postRendering(state, null, g);
    }

    /**
     * Called when the rendering is still in progress and a given
     * {@link #setLongRenderingTimeout(long, TimeUnit) timeout} elapsed. This
     * method may update the display with addition information. Note however,
     * that this method is called on the AWT Event Dispatch Thread and as such,
     * should not do expensive computations.
     * <P>
     * This method may be overridden in subclasses. The default implementation
     * display some information of the image to be loaded and the current
     * progress.
     *
     * @param g the {@code Graphics2D} to which this method need to draw to.
     *   This argument cannot be {@code null}. This method does not need to
     *   preserve the graphic context.
     * @param dataStates the current state of the image retrieving and rendering
     *   process. This state contains the state of the image retrieval process
     *   and all the applied transformations. This argument can never be
     *   {@code null} but can contain {@code null} states.
     */
    protected void displayLongRenderingState(Graphics2D g, MultiAsyncDataState dataStates) {
        ImageMetaData currentMetaData = getImageMetaData().getValue();
        int stateCount = dataStates.getSubStateCount();

        if (stateCount > 0) {
            StringBuilder message = new StringBuilder();
            message.append("Rendering: ");

            for (int i = 0; i < stateCount; i++) {
                if (i > 0) {
                    message.append(", ");
                }

                message.append(Math.round(100.0 * dataStates.getSubProgress(i)));
                message.append("%");
            }

            if (currentMetaData != null) {
                message.append("\nDim: ");
                message.append(currentMetaData.getWidth());
                message.append("X");
                message.append(currentMetaData.getHeight());
            }

            RenderHelper.drawMessage(g, message.toString());
        }
    }

    private void postRendering(RenderingState state, PaintResult renderingResult, Graphics2D g) {
        postRenderingAction(renderingResult);

        if (isLongRendering()) {
            postLongRendering(g, state);
        }

        checkLongRendering(state);
    }

    private void postLongRendering(Graphics2D g, RenderingState state) {
        if (!imageShown.getValue()
                && getTimeSinceLastImageShow(TimeUnit.NANOSECONDS) > getOldImageHideTimeNanos()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        g.setColor(getForeground());
        g.setFont(getFont());
        g.setBackground(getBackground());

        AsyncDataState dataState = state != null ? state.getAsyncDataState() : null;
        MultiAsyncDataState states = dataState instanceof MultiAsyncDataState
                ? (MultiAsyncDataState)dataState
                : new MultiAsyncDataState(dataState);

        displayLongRenderingState(g, states);
    }

    private void updateMetaDataIfNeeded(PaintResult renderingResult) {
        ImageMetaData currentMetaData = imageMetaData.getValue();
        if (currentMetaData == null || !currentMetaData.isComplete()) {
            ImageMetaData newMetaData = renderingResult.metaData;
            if (newMetaData != null) {
                imageMetaData.setValue(newMetaData);
            }
        }
    }

    private void postRenderingAction(PaintResult renderingResult) {
        if (renderingResult != null && renderingResult.imageSource == imageSource.getValue()) {
            updateMetaDataIfNeeded(renderingResult);

            boolean newImageShown = imageShown.getValue();
            if (renderingResult.imageReceived) {
                newImageShown = true;
            }

            if (newImageShown) {
                imageShownTime = System.nanoTime();
            }

            ImagePointTransformer currentPointTransformer;
            currentPointTransformer = renderingResult.srcToDestTransform;
            if (currentPointTransformer != null) {
                displayedPointTransformer.setValue(currentPointTransformer);
            }

            if (newImageShown != imageShown.getValue().booleanValue())  {
                imageShown.setValue(newImageShown);
            }
        }
    }

    private boolean isLongRendering() {
        TimeDuration renderingPatience = longRenderingTimeout.getValue();
        if (renderingPatience == null) {
            return false;
        }
        if (!isRendering()) {
            return false;
        }
        return getSignificantRenderingTime(TimeUnit.NANOSECONDS) >= renderingPatience.toNanos();
    }

    private void checkLongRendering(RenderingState state) {
        if (state == null || state.isRenderingFinished()) {
            return;
        }

        if (!isLongRendering()) {
            startLongRenderingListener();
        }
    }

    private void startLongRenderingListener() {
        if (longRenderingTimeout.getValue() == null) {
            return;
        }
        if (!isDisplayable()) {
            return;
        }

        javax.swing.Timer timer;
        timer = new javax.swing.Timer(RENDERING_STATE_POLL_TIME_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private final class StepDefImpl implements TransformationStepDef {
        private final RefList.ElementRef<PreparedOutputBufferStep> ref;
        private final TransformationStepPos pos;
        // Only used in the rendering loop.
        private final AtomicReference<VolatileReference<BufferedImage>> offeredRef;

        public StepDefImpl(RefList.ElementRef<PreparedOutputBufferStep> ref) {
            this.ref = ref;
            this.pos = new StepPosImpl(ref);
            this.offeredRef = new AtomicReference<>(GenericReference.<BufferedImage>getNoReference());
        }

        @Override
        public TransformationStepPos getPosition() {
            return pos;
        }

        @Override
        public void setTransformation(ImageTransformationStep transformation) {
            if (transformation == null) {
                ref.setElement(null);
            }
            else {
                ref.setElement(new PreparedOutputBufferStep(transformation, offeredRef));
            }
            invalidateTransformations();
        }

        @Override
        public void removeStep() {
            ref.remove();
        }
    }

    private static boolean isCompatible(BufferedImage image1, BufferedImage image2) {
        int type1 = image1.getType();
        int type2 = image2.getType();
        if (type1 == BufferedImage.TYPE_CUSTOM || type2 == BufferedImage.TYPE_CUSTOM) {
            return false;
        }

        if (type1 != type2) {
            return false;
        }
        if (image1.getWidth() != image2.getWidth()) {
            return false;
        }
        if (image2.getHeight() != image2.getHeight()) {
            return false;
        }
        return true;
    }

    private final class PreparedOutputBufferStep implements ImageTransformationStep {
        private final ImageTransformationStep wrapped;
        // This is safe to return because we only use it in a single rendering
        // loop and two loops cannot run concurrently for the same component.
        private final AtomicReference<VolatileReference<BufferedImage>> offeredRef;

        public PreparedOutputBufferStep(
                ImageTransformationStep wrapped,
                AtomicReference<VolatileReference<BufferedImage>> offeredRef) {
            this.wrapped = wrapped;
            this.offeredRef = offeredRef;
        }

        public void tryStealBufferFrom(PreparedOutputBufferStep other) {
            VolatileReference<BufferedImage> otherBufferRef = other.offeredRef.get();
            BufferedImage ourBuffer = offeredRef.get().get();

            if (ourBuffer == null) {
                offeredRef.set(otherBufferRef);
            }

            BufferedImage otherBuffer = otherBufferRef.get();
            if (otherBuffer != null && isCompatible(ourBuffer, otherBuffer)) {
                offeredRef.set(otherBufferRef);
            }
        }

        @Override
        public TransformedImage render(
                CancellationToken cancelToken,
                TransformationStepInput input,
                BufferedImage offeredBuffer) {

            BufferedImage offered = offeredRef.get().get();
            TransformedImage output = wrapped.render(cancelToken, input, offered);

            if (output.getImage() != offered) {
                offeredRef.set(GenericReference.createReference(output.getImage(), TMP_BUFFER_REFERENCE_TYPE));
            }
            return output;
        }
    }

    private final class StepPosImpl implements TransformationStepPos {
        private final RefList.ElementRef<PreparedOutputBufferStep> ref;

        public StepPosImpl(RefList.ElementRef<PreparedOutputBufferStep> ref) {
            this.ref = ref;
        }

        @Override
        public TransformationStepDef addBefore() {
            RefList.ElementRef<PreparedOutputBufferStep> newRef = ref.addBefore(null);
            return new StepDefImpl(newRef);
        }

        @Override
        public TransformationStepDef addAfter() {
            RefList.ElementRef<PreparedOutputBufferStep> newRef = ref.addAfter(null);
            return new StepDefImpl(newRef);
        }
    }

    private static final class PaintResult {
        public final AsyncDataLink<?> imageSource;
        public final ImageMetaData metaData;
        public final ImagePointTransformer srcToDestTransform;
        public final boolean imageReceived;

        public PaintResult(
                AsyncDataLink<?> imageSource,
                ImageMetaData metaData,
                ImagePointTransformer srcToDestTransform,
                boolean imageReceived) {
            this.imageSource = imageSource;
            this.metaData = metaData;
            this.srcToDestTransform = srcToDestTransform;
            this.imageReceived = imageReceived;
        }
    }

    private final class RendererImpl implements ImageRenderer<ImageResult, PaintResult> {
        private final AsyncDataLink<?> dataLink;
        private final BasicRenderingArguments basicArgs;

        public RendererImpl(AsyncDataLink<?> dataLink, BasicRenderingArguments basicArgs) {
            assert basicArgs != null;
            this.dataLink = dataLink;
            this.basicArgs = basicArgs;
        }

        @Override
        public RenderingResult<PaintResult> startRendering(
                CancellationToken cancelToken,
                BufferedImage drawingSurface) {
            return RenderingResult.noRendering();
        }

        @Override
        public boolean willDoSignificantRender(ImageResult data) {
            return data != null && data.getImage() != null;
        }

        @Override
        public RenderingResult<PaintResult> render(
                CancellationToken cancelToken,
                ImageResult data,
                BufferedImage drawingSurface) {

            int destWidth = drawingSurface.getWidth();
            int destHeight = drawingSurface.getHeight();
            BufferedImage lastOutput = data != null ? data.getImage() : null;

            List<ImagePointTransformer> pointTransformers = new ArrayList<>(stepsSnapshot.size());

            PreparedOutputBufferStep prevStep = null;
            for (PreparedOutputBufferStep step: stepsSnapshot) {
                if (step != null) {
                    if (prevStep != null) {
                        step.tryStealBufferFrom(prevStep);
                    }

                    TransformedImage imageInput = new TransformedImage(
                            lastOutput, SerialImagePointTransformer.combine(pointTransformers));

                    TransformationStepInput input = new TransformationStepInput(
                            data, destWidth, destHeight, imageInput);

                    TransformedImage output = step.render(cancelToken, input, null);
                    lastOutput = output.getImage();

                    pointTransformers.add(output.getPointTransformer());
                }
            }

            if (lastOutput == null) {
                return RenderingResult.noRendering();
            }

            Graphics2D g2d = drawingSurface.createGraphics();
            try {
                g2d.drawImage(lastOutput, null, 0, 0);
            } finally {
                g2d.dispose();
            }

            if (data == null || data.getImage() == null) {
                return RenderingResult.insignificant(new PaintResult(
                        dataLink,
                        data != null ? data.getMetaData() : null,
                        SerialImagePointTransformer.combine(pointTransformers),
                        false));
            }
            else {
                return RenderingResult.significant(new PaintResult(
                        dataLink,
                        data.getMetaData(),
                        SerialImagePointTransformer.combine(pointTransformers),
                        true));
            }
        }

        @Override
        public RenderingResult<PaintResult> finishRendering(
                CancellationToken cancelToken,
                AsyncReport report,
                BufferedImage drawingSurface) {
            if (report.getException() != null) {
                onRenderingError(basicArgs, drawingSurface, report.getException());
                return RenderingResult.significant(null);
            }
            else {
                return RenderingResult.noRendering();
            }
        }
    }

    private static final class RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        public static final RunnableDispatcher INSTANCE = new RunnableDispatcher();

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }

    private final class ImageQueryVerifier
    implements
            PropertyVerifier<AsyncDataQuery<? super ImageAddress, ? extends ImageResult>> {

        @Override
        public AsyncDataQuery<? super ImageAddress, ? extends ImageResult> storeValue(
                AsyncDataQuery<? super ImageAddress, ? extends ImageResult> value) {

            // The imageAddress check is needed so that we do not fail during
            // construction time.
            if (value == null && imageAddress != null && imageAddress.getValue() != null) {
                throw new IllegalStateException("null image query cannot query images."
                        + " Current image address: " + imageAddress);
            }
            return value;
        }
    }

    private final class ImageAddressVerifier
    implements
            PropertyVerifier<ImageAddress> {

        @Override
        public ImageAddress storeValue(ImageAddress value) {
            if (value != null && imageQuery.getValue() == null) {
                throw new IllegalStateException("null image query cannot query images: " + value);
            }
            return value;
        }
    }
}

