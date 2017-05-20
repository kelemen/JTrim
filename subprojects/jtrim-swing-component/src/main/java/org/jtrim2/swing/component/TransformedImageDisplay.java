package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cache.GenericReference;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cache.VolatileReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataQuery;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.MultiAsyncDataState;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.image.ImageMetaData;
import org.jtrim2.image.ImageResult;
import org.jtrim2.image.transform.ImagePointTransformer;
import org.jtrim2.image.transform.ImageTransformationStep;
import org.jtrim2.image.transform.SerialImagePointTransformer;
import org.jtrim2.image.transform.TransformationStepInput;
import org.jtrim2.image.transform.TransformedImage;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.ui.concurrent.query.AsyncRendererFactory;
import org.jtrim2.ui.concurrent.query.RenderingState;
import org.jtrim2.utils.TimeDuration;

import static org.jtrim2.property.PropertyFactory.*;

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
 *   {@link #imageAddress() imageAddress property}.
 *  </li>
 *  <li>
 *   The {@link AsyncDataQuery} used to retrieve the image. The image is
 *   retrieved by the address specified previously. The query can be set by
 *   the {@link #imageQuery() imageQuery property}.
 *   methods.
 *  </li>
 *  <li>
 *   The transformations which will be used to transform the image retrieved by
 *   the image query. The first transformation must be added by a call to the
 *   {@link #addFirstStep()} method subsequent transformations must be added
 *   through this step through the {@link TransformationStepPos} interface. The
 *   {@code addFirstStep} method is recommended to be called by the constructor
 *   of the extending class, which ensures that no-one else might call this
 *   method successfully.
 *  </li>
 * </ul>
 * <P>
 * Note that this component is an {@link TransformedImageDisplay} and relies on
 * an {@link AsyncRendererFactory}. Therefore it must be set before displaying
 * this component, either by passing an {@link AsyncRendererFactory} to the
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
 *                     // Draw something to the output based on the input.
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
 * only from the AWT Event Dispatch Thread. Note however that
 * {@link MutableProperty} and {@link PropertySource} instances can be read by
 * any thread (even concurrently with writes to the property).
 *
 * @param <ImageAddress> the type of the address of the image to be
 *   displayed. That is, the input of the
 *   {@link #imageQuery() image query}.
 */
@SuppressWarnings("serial")
public abstract class TransformedImageDisplay<ImageAddress> extends AsyncRenderingComponent {
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
    private final MutableProperty<ReferenceType> tmpBufferReferenceType;

    private final Queue<Runnable> lazyTransformationUpdaters;

    private long imageReplaceTime;
    private long imageShownTime;

    private boolean preparedRenderingArgs;
    private final AutoRepainter autoRepainter;

    /**
     * Creates a new {@code TransformedImageDisplay} without setting the
     * {@link AsyncRendererFactory} to be used. Therefore the
     * {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} must be
     * called before displaying the component.
     */
    public TransformedImageDisplay() {
        this(null);
    }

    /**
     * Creates a new {@code TransformedImageDisplay} using an
     * {@link org.jtrim2.ui.concurrent.query.AsyncRenderer} created by the
     * specified {@link AsyncRendererFactory}. Note however, that if you pass
     * {@code null} for the argument of this constructor, you still have to call
     * the {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
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
        this.lazyTransformationUpdaters = new LinkedList<>();

        this.imageMetaData = lazyNullableProperty(null);
        this.imageQuery = lazilySetProperty(memProperty(null, this::validImageQuery),
                Equality.referenceEquality());
        this.imageAddress = lazilySetProperty(memProperty(null, this::validImageAddress));
        this.imageSource = lazyNullableProperty(null, Equality.referenceEquality());
        this.imageShown = lazyProperty(false);
        this.oldImageHideTime = lazyProperty(DEFAULT_OLD_IMAGE_HIDE);
        this.longRenderingTimeout = lazyNullableProperty(null);
        this.displayedPointTransformer = lazyNullableProperty(null);
        this.tmpBufferReferenceType = lazyProperty(ReferenceType.WeakRefType);
        this.autoRepainter = new AutoRepainter();

        this.imageReplaceTime = System.nanoTime();
        this.imageShownTime = imageReplaceTime;

        this.preparedRenderingArgs = false;

        setRenderingArgs();

        // Must be called as the last instruction of the constructor
        addInitialListeners();
    }

    private static <ValueType> MutableProperty<ValueType> lazyProperty(ValueType initialValue) {
        return lazilySetProperty(memProperty(initialValue));
    }

    private static <ValueType> MutableProperty<ValueType> lazyNullableProperty(ValueType initialValue) {
        return lazilySetProperty(memProperty(initialValue, true));
    }

    private static <ValueType> MutableProperty<ValueType> lazyNullableProperty(
            ValueType initialValue,
            EqualityComparator<? super ValueType> equality) {
        return lazilySetProperty(memProperty(initialValue, true), equality);
    }

    private void addInitialListeners() {
        addPrePaintListener(this::prepareRenderingArgs);

        imageSource.addChangeListener(this::invalidateRenderingArgs);

        Runnable setImageSourceAction = this::setImageSource;
        imageQuery.addChangeListener(setImageSourceAction);
        imageAddress.addChangeListener(setImageSourceAction);

        Runnable repaintTask = this::repaint;
        oldImageHideTime.addChangeListener(repaintTask);
        longRenderingTimeout.addChangeListener(repaintTask);
    }

    /**
     * Adds a listener which is to be notified after the transformation
     * applied to retrieved image is changed.
     *
     * @param listener the {@code listener} whose {@code run} method is called
     *   on change. This argument cannot be {@code null}.
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
     * Returns the property defining the reference type to use for temporary
     * buffers passed for transformations. That is, the offered buffer passed to the
     * {@link ImageTransformationStep#render(CancellationToken, TransformationStepInput, BufferedImage) ImageTransformationStep.render}
     * method is cached and will be referenced according to this property.
     * <P>
     * Note that setting this property does not necessarily takes effect until
     * the currently cached buffers disappear from memory. Therefore, it is
     * recommended to set this property before any rendering takes place:
     * immediately after creating this component.
     * <P>
     * The value of this property cannot be {@code null}.
     *
     * @return the property defining the reference type to use for temporary
     *   buffers passed for transformations. This method never returns
     *   {@code null}.
     */
    public MutableProperty<ReferenceType> tmpBufferReferenceType() {
        return tmpBufferReferenceType;
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
    public final PropertySource<ImagePointTransformer> displayedPointTransformer() {
        return protectedView(displayedPointTransformer);
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
    public final PropertySource<ImageMetaData> imageMetaData() {
        return protectedView(imageMetaData);
    }

    /**
     * Returns the property defining if this component is currently displaying
     * an image which was fetched from the currently set image query and address
     * or not. This is also {@code true} if there was an error fetching the
     * image and something was rendered instead of it.
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
    public final PropertySource<Boolean> imageShown() {
        return protectedView(imageShown);
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
    public final MutableProperty<TimeDuration> oldImageHideTime() {
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
    public final MutableProperty<TimeDuration> longRenderingTimeout() {
        return longRenderingTimeout;
    }

    /**
     * Returns the time since the source image of this component has been
     * changed in the given time unit. This can either occur due to changing the
     * {@link #imageAddress() image address} or due to changing the
     * {@link #imageQuery() image query}.
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
     * {@link #imageQuery()}.
     * <P>
     * You may set this property to {@code null} and if it is {@code null},
     * no image will be displayed by this component. That is, if this property
     * is {@code null}, this component will be painted with the background color
     * of this component.
     * <B>Note</B>: When the {@link #imageQuery() image query} is
     * {@code null}, the image address property must also be {@code null}.
     *
     * @return the address of the image to be passed to the current image
     *   {@link #imageQuery()}. This method never returns {@code null}.
     */
    public final MutableProperty<ImageAddress> imageAddress() {
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
     * {@link #imageAddress() image address} property must also be
     * {@code null}.
     *
     * @return the image query property used to retrieve the image to be
     *   displayed. This method never returns {@code null}.
     */
    public final MutableProperty<AsyncDataQuery<? super ImageAddress, ? extends ImageResult>> imageQuery() {
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
    public final PropertySource<AsyncDataLink<? extends ImageResult>> imageSource() {
        return protectedView(imageSource);
    }

    /**
     * Called when an exception occurred while trying to retrieve the image
     * or during rendering the image. This method may update the specified
     * drawing surface.
     * <P>
     * This method is called in the context of the
     * {@link org.jtrim2.ui.concurrent.query.AsyncRenderer} of this component
     * and may do some more expensive computation without blocking the input of
     * the user.
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
        EventListeners.dispatchRunnable(transformationChangeListeners);
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

    /**
     * Adds a task to be run just before the applied transformations are
     * collected for the renderer. This method is intended to allow subclasses
     * to lazily build and set their transformations via
     * {@link TransformationStepDef#setTransformation(ImageTransformationStep) TransformationStepDef.setTransformation}.
     * <P>
     * Subclasses should also consider using an
     * {@link org.jtrim2.executor.UpdateTaskExecutor} to add tasks via this
     * method.
     *
     * @param updaterTask the {@code Runnable} whose {@code run} method is to
     *   be executed before actually collecting the applied transformations to
     *   the renderer. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is {@code null}
     */
    protected final void addLazyTransformationUpdater(Runnable updaterTask) {
        Objects.requireNonNull(updaterTask, "updaterTask");
        lazyTransformationUpdaters.add(updaterTask);
        repaint();
    }

    private void dispatchLazyTransformationUpdaters() {
        while (!lazyTransformationUpdaters.isEmpty()) {
            Runnable task = lazyTransformationUpdaters.poll();
            task.run();
        }
    }

    private void prepareRenderingArgs() {
        dispatchLazyTransformationUpdaters();
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

    private static void clearImage(BufferedImage image, Color color) {
        Graphics2D g = image.createGraphics();
        clearGraphics(g, color, image.getWidth(), image.getHeight());
        g.dispose();
    }

    private static void clearGraphics(Graphics2D g, Color color, int width, int height) {
        g.setColor(color);
        g.fillRect(0, 0, width, height);
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
                clearGraphics(g, getBackground(), getWidth(), getHeight());
                return true;
            }

            @Override
            public void postPaintComponent(RenderingState state, PaintResult renderingResult, Graphics2D g) {
                postRendering(state, renderingResult, g);
            }
        });
    }

    private void invalidateTransformations() {
        preparedStep = false;
        invalidateRenderingArgs();
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
     * {@link #longRenderingTimeout() timeout} elapsed. This method may
     * update the display with addition information. Note however, that this
     * method is called on the AWT Event Dispatch Thread and as such, should not
     * do expensive computations.
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
        ImageMetaData currentMetaData = imageMetaData().getValue();
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
        if (renderingResult == null) {
            inheritedPaintDefault(state, g);
        }
        else {
            postRenderingAction(renderingResult);
        }

        if (imageShown.getValue()) {
            autoRepainter.repaintLater(RENDERING_STATE_POLL_TIME_MS);
        }

        if (!imageShown.getValue()
                && getTimeSinceLastImageShow(TimeUnit.NANOSECONDS) > getOldImageHideTimeNanos()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        tryStartLongRenderingListener();

        if (isLongRendering()) {
            postLongRendering(g, state);
        }
    }

    private void postLongRendering(Graphics2D g, RenderingState state) {
        g.setColor(getForeground());
        g.setFont(getFont());
        g.setBackground(getBackground());

        AsyncDataState dataState = state.getAsyncDataState();
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

    // This is only needed for testing to detect if the component going to be
    // repainted, so the test code should wait until repainting is done.
    PropertySource<Boolean> repaintTimerActive() {
        return protectedView(autoRepainter.repaintActive);
    }

    private void postRenderingAction(PaintResult renderingResult) {
        if (renderingResult.imageSource == imageSource.getValue()) {
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

    private void tryStartLongRenderingListener() {
        if (longRenderingTimeout.getValue() == null) {
            return;
        }
        if (!isRendering()) {
            return;
        }

        // We should not pass a large value because if this component becomes
        // undisplayable, we will have a useless pending timer preventing the
        // application from terminating (also wasting resources).
        autoRepainter.repaintLater(RENDERING_STATE_POLL_TIME_MS);
    }

    private AsyncDataQuery<? super ImageAddress, ? extends ImageResult> validImageQuery(
            AsyncDataQuery<? super ImageAddress, ? extends ImageResult> value) {

        // The imageAddress check is needed so that we do not fail during
        // construction time.
        if (value == null && imageAddress != null && imageAddress.getValue() != null) {
            throw new IllegalStateException("null image query cannot query images."
                    + " Current image address: " + imageAddress);
        }
        return value;
    }

    private ImageAddress validImageAddress(ImageAddress value) {
        if (value != null && imageQuery.getValue() == null) {
            throw new IllegalStateException("null image query cannot query images: " + value);
        }
        return value;
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

    private final class PreparedOutputBufferStep implements ImageTransformationStep {
        private final ImageTransformationStep wrapped;
        private final AtomicReference<VolatileReference<BufferedImage>> offeredRef;

        public PreparedOutputBufferStep(
                ImageTransformationStep wrapped,
                AtomicReference<VolatileReference<BufferedImage>> offeredRef) {
            this.wrapped = wrapped;
            this.offeredRef = offeredRef;
        }

        @Override
        public TransformedImage render(
                CancellationToken cancelToken,
                TransformationStepInput input,
                BufferedImage offeredBuffer) {

            BufferedImage offered = offeredRef.get().get();
            TransformedImage output = wrapped.render(cancelToken, input, offered);

            BufferedImage outputImage = output.getImage();
            if (outputImage != offered && outputImage != input.getInputImage().getImage()) {
                ReferenceType cacheRef = tmpBufferReferenceType.getValue();
                offeredRef.set(GenericReference.createReference(outputImage, cacheRef));
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
            if (dataLink == null) {
                clearImage(drawingSurface, basicArgs.getBackgroundColor());
                return RenderingResult.significant(null);
            }
            else {
                return RenderingResult.noRendering();
            }
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

            for (PreparedOutputBufferStep step: stepsSnapshot) {
                cancelToken.checkCanceled();

                if (step != null) {
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
                clearImage(drawingSurface, basicArgs.getBackgroundColor());
                return RenderingResult.insignificant(new PaintResult(
                        dataLink,
                        data != null ? data.getMetaData() : null,
                        SerialImagePointTransformer.combine(pointTransformers),
                        false));
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
                return RenderingResult.significant(new PaintResult(dataLink, null, null, true));
            }
            else {
                return RenderingResult.noRendering();
            }
        }
    }

    private final class AutoRepainter {
        private final MutableProperty<Boolean> repaintActive;
        private javax.swing.Timer currentTimer;
        private int currentTimerMs;
        private long startTime;

        public AutoRepainter() {
            this.repaintActive = PropertyFactory.memProperty(false);
            this.currentTimer = null;
            this.currentTimerMs = 0; // doesn't matter
            this.startTime = 0; // doesn't matter
        }

        public void repaintLater(int timeoutMs) {
            if (currentTimer != null) {
                long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                if (currentTimerMs - elapsedMs < timeoutMs) {
                    // We will call repaint sooner than the one specified
                    // in timeoutMs.
                    return;
                }

                currentTimer.stop();
            }

            if (!isDisplayable() || timeoutMs <= 0) {
                currentTimer = null;
                repaintActive.setValue(false);
                repaint(); // Although repaint is only needed if isDisplayable() is true.
                return;
            }

            startTime = System.nanoTime();
            currentTimerMs = timeoutMs;

            final AtomicReference<javax.swing.Timer> startedTimerRef = new AtomicReference<>(null);
            currentTimer = new javax.swing.Timer(timeoutMs, (ActionEvent e) -> {
                if (currentTimer == startedTimerRef.get()) {
                    repaintActive.setValue(false);
                    repaint();
                }
            });
            startedTimerRef.set(currentTimer);
            currentTimer.setRepeats(false);

            repaintActive.setValue(true);
            currentTimer.start();
        }
    }
}

