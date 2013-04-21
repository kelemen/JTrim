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
import org.jtrim.image.transform.AffineImagePointTransformer;
import org.jtrim.image.transform.ImagePointTransformer;
import org.jtrim.image.transform.SerialImagePointTransformer;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.PropertyVerifier;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.BasicRenderingArguments;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.utils.ExceptionHelper;
import org.jtrim.utils.TimeDuration;

/**
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
     */
    public final PropertySource<ImagePointTransformer> getDisplayedPointTransformer() {
        return PropertyFactory.protectedView(displayedPointTransformer);
    }

    /**
     */
    public final PropertySource<ImageMetaData> getImageMetaData() {
        return PropertyFactory.protectedView(imageMetaData);
    }

    /**
     */
    public final PropertySource<Boolean> getImageShown() {
        return PropertyFactory.protectedView(imageShown);
    }

    /**
     */
    public final MutableProperty<TimeDuration> getOldImageHideTime() {
        return oldImageHideTime;
    }

    private long getOldImageHideTimeNanos() {
        return oldImageHideTime.getValue().toNanos();
    }

    /**
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
            imageSource.setValue(newLink);
        }
    }

    private void prepareRenderingArgs() {
        prepareSteps();

        if (preparedRenderingArgs) {
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
                return true;
            }

            @Override
            public void postPaintComponent(RenderingState state, PaintResult renderingResult, Graphics2D g) {
                // TODO: implement
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    private void invalidateTransformations() {
        preparedStep = false;
        repaint();
    }

    public static ImageTransformationStep cachedStep(
            ReferenceType refType,
            ImageTransformationStep step,
            ImageTransformationStep.InputCmp cacheCmp) {
        return new CachingImageTransformationStep(refType, step, cacheCmp);
    }

    /**
     */
    protected final TransformationStepDef addFirstStep() {
        if (!steps.isEmpty()) {
            throw new IllegalStateException("This method may only be called once.");
        }

        RefList.ElementRef<PreparedOutputBufferStep> stepRef = steps.addFirstGetReference(null);

        invalidateTransformations();
        return new StepDefImpl(stepRef);
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
        super.paintDefault(state, g);

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

    private static ImagePointTransformer combineTransfomers(List<ImagePointTransformer> transformers) {
        switch (transformers.size()) {
            case 0:
                return AffineImagePointTransformer.IDENTITY;
            case 1:
                return transformers.get(0);
            default:
                return new SerialImagePointTransformer(transformers);
        }
    }

    private final class RendererImpl implements ImageRenderer<ImageResult, PaintResult> {
        private final AsyncDataLink<?> dataLink;
        private final BasicRenderingArguments basicArgs;
        private boolean renderedSomething;

        public RendererImpl(AsyncDataLink<?> dataLink, BasicRenderingArguments basicArgs) {
            assert basicArgs != null;
            this.dataLink = dataLink;
            this.basicArgs = basicArgs;
            this.renderedSomething = false;
        }

        @Override
        public RenderingResult<PaintResult> startRendering(
                CancellationToken cancelToken,
                BufferedImage drawingSurface) {
            this.renderedSomething = false;
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
                            lastOutput, combineTransfomers(pointTransformers));

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

            renderedSomething = true;
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
                        combineTransfomers(pointTransformers),
                        false));
            }
            else {
                return RenderingResult.significant(new PaintResult(
                        dataLink,
                        data.getMetaData(),
                        combineTransfomers(pointTransformers),
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
            else if (renderedSomething) {
                return RenderingResult.noRendering();
            }
            else {
                Graphics2D g2d = drawingSurface.createGraphics();
                try {
                    g2d.setColor(basicArgs.getBackgroundColor());
                    g2d.fillRect(0, 0, drawingSurface.getWidth(), drawingSurface.getHeight());
                } finally {
                    g2d.dispose();
                }

                PaintResult result = new PaintResult(
                        dataLink, null, AffineImagePointTransformer.IDENTITY, false);
                return RenderingResult.significant(result);
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

