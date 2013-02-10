package org.jtrim.swing.component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.image.ImageData;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.swing.concurrent.async.AsyncRenderer;
import org.jtrim.swing.concurrent.async.AsyncRendererFactory;
import org.jtrim.swing.concurrent.async.DataRenderer;
import org.jtrim.swing.concurrent.async.DrawingConnector;
import org.jtrim.swing.concurrent.async.GenericAsyncRendererFactory;
import org.jtrim.swing.concurrent.async.GraphicsCopyResult;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a base class for <I>Swing</I> components drawn in the background.
 * <P>
 * This class relies on {@link AsyncRenderer} to do the background painting.
 * This {@code AsyncRenderer} must be set <I>before</I> the component is
 * actually rendered, preferably by passing it to the constructor. To provide
 * input for the {@code AsyncRenderer} call one of the {@code setRenderingArgs}
 * methods. It is also mandatory to call any of the {@code setRenderingArgs}
 * before the component is actually displayed (the preferred way is to call
 * {@code setRenderingArgs} from within the constructor of the implementation.
 * <P>
 * This component also provides various methods to check the progress of the
 * painting of this component. This might be used by implementations to
 * do addition paintings in case the rendering process takes too much time.
 * <P>
 * In case you want to set the rendering arguments ({@code setRenderingArgs})
 * lazily, you may do so in a {@link #addPrePaintListener(Runnable) pre-paint listener}.
 * <P>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread after made displayable.
 *
 * @see AsyncRenderer
 * @see #setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook) setRenderingArgs
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial")
public abstract class AsyncRenderingComponent extends Graphics2DComponent {
    /**
     * Instances of this interface can be passed to the
     * {@link #setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook) setRenderingArgs}
     * method and the methods of this interface will be called prior to and
     * after rendering.
     * <P>
     * Methods of this interface are expected to be called from within
     * the {@link #paintComponent2D(Graphics2D) paintComponent2D} method and
     * therefore can expect to be called from the AWT Event Dispatch Thread.
     *
     * @param <T> the type of the object returned by {@link ImageRenderer}
     *   passed to the {@code setRenderingArgs} method
     */
    public static interface PaintHook<T> {
        /**
         * This method is called before actually painting the
         * {@code AsyncRenderingComponent} and may draw to the
         * {@code Graphics2D} prior rendering.
         * <P>
         * Note that since this method is called from the EDT, it should not do
         * any expensive painting operation. Also note that every invocation
         * of the {@link #paintComponent2D(Graphics2D) paintComponent2D} method
         * will call this method.
         *
         * @param state the {@code RenderingState} associated with the rendering
         *   process whose result is to be painted to the
         *   {@code AsyncRenderingComponent}. This argument cannot be
         *   {@code null}.
         * @param g the {@code Graphics2D} object to which this method may draw
         *   to prior the rendered data being copied to this {@code Graphics2D}
         *   object. This argument cannot be {@code null}.
         * @return {@code true} if the result of the asynchronous rendering
         *   operation need to be copied to the {@code Graphics2D} object,
         *   {@code false} otherwise. That is, if this method returns
         *   {@code false}, the result of the asynchronous rendering will be
         *   ignored, not even {@link #postPaintComponent(RenderingState, Object, Graphics2D) postPaintComponent}
         *   will be called.
         */
        public boolean prePaintComponent(RenderingState state, Graphics2D g);

        /**
         * Called after the result of asynchronous painting has been copied to
         * the {@code Graphics2D} object to be displayed. This method has a last
         * chance to render something on the {@code Graphics2D} to be displayed.
         * That is, anything this method draws on the passed {@code Graphics2D}
         * will be visible to the user and will not be overwritten.
         * <P>
         * Note that since this method is called from the EDT, it should not do
         * any expensive painting operation. Also note that every invocation
         * of the {@link #paintComponent2D(Graphics2D) paintComponent2D} method
         * will call this method if the previous {@code prePaintComponent}
         * method returned {@code true} and the asynchronous rendering operation
         * has completed at least once.
         *
         * @param state the {@code RenderingState} associated with the rendering
         *   process whose result is to be painted to the
         *   {@code AsyncRenderingComponent}. This argument cannot be
         *   {@code null}.
         * @param renderingResult the last object returned by an asynchronous
         *   rendering (via the {@link ImageRenderer}). This argument can be
         *   {@code null} if the asynchronous rendering operation has returned
         *   a {@code null} result.
         * @param g the {@code Graphics2D} object to which this method may draw
         *   something. This argument cannot be {@code null}.
         */
        public void postPaintComponent(RenderingState state, T renderingResult, Graphics2D g);
    }

    private static final AsyncRendererFactory DEFAULT_RENDERER
            = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

    private static final Logger LOGGER = Logger.getLogger(AsyncRenderingComponent.class.getName());

    private final SwingUpdateTaskExecutor repaintRequester;
    private final DrawingConnector<InternalResult<?>> drawingConnector;
    private ColorModel bufferTypeModel;
    private int bufferType;
    private AsyncRenderer asyncRenderer;
    private Renderer<?, ?> renderer;
    private Renderer<?, ?> lastExecutedRenderer;
    private RenderingState lastRenderingState;
    private RenderingState lastPaintedState;
    private RenderingState lastSignificantPaintedState;

    private final ListenerManager<Runnable, Void> prePaintEvents;

    /**
     * Initializes this {@code AsyncRenderingComponent} with a {@code null}
     * {@link AsyncRenderer}, so the {@code AsyncRenderer} must be set later
     * by calling the {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer}
     * method.
     * <P>
     * Note: Don't forget to set the rendering arguments by calling one of the
     * {@code setRenderingArgs} methods.
     */
    public AsyncRenderingComponent() {
        this(null);
    }

    /**
     * Initializes this {@code AsyncRenderingComponent} with a specified
     * {@link AsyncRenderer}.
     * <P>
     * Note: Don't forget to set the rendering arguments by calling one of the
     * {@code setRenderingArgs} methods.
     *
     * @param asyncRenderer the {@code AsyncRendererFactory} to be used to
     *   render this component. This argument can be {@code null}, in which
     *   case, the {@code AsyncRendererFactory} must be set later by the
     *   {@link #setAsyncRenderer(AsyncRendererFactory) setAsyncRenderer} method.
     */
    public AsyncRenderingComponent(AsyncRendererFactory asyncRenderer) {
        this.prePaintEvents = new CopyOnTriggerListenerManager<>();
        this.repaintRequester = new SwingUpdateTaskExecutor(true);
        this.asyncRenderer = asyncRenderer != null ? asyncRenderer.createRenderer() : null;
        this.bufferTypeModel = null;
        this.bufferType = BufferedImage.TYPE_INT_ARGB;
        this.drawingConnector = new DrawingConnector<>(1, 1);
        this.renderer = null;
        this.lastExecutedRenderer = null;
        this.lastRenderingState = null;
        this.lastPaintedState = new NoOpRenderingState();
        this.lastSignificantPaintedState = this.lastPaintedState;
    }

    private int getRequiredDrawingSurfaceType() {
        ColorModel colorModel = getColorModel();
        if (bufferTypeModel != colorModel) {
            bufferType = ImageData.getCompatibleBufferType(getColorModel());
            if (bufferType == BufferedImage.TYPE_CUSTOM) {
                bufferType = BufferedImage.TYPE_INT_ARGB;
            }
            bufferTypeModel = colorModel;
        }

        return bufferType;
    }

    private void setLastPaintedState(RenderingState state) {
        lastSignificantPaintedState = state != null
                ? state
                : new NoOpRenderingState();
    }

    private void setLastSignificantPaintedState(RenderingState state) {
        lastSignificantPaintedState = state != null
                ? state
                : new NoOpRenderingState();
    }

    /**
     * Adds a {@code Runnable} which is to be called in the
     * {@link #paintComponent2D(Graphics2D) paintComponent2D} method prior
     * actually painting. This listener can call the {@code setRenderingArgs}
     * methods to set the rendering arguments before the painting takes place.
     * <P>
     * This method may be called from any thread and is synchronization
     * transparent.
     *
     * @param listener the {@code Runnable} whose {@code run} method is to be
     *   invoked by the {@code paintComponent2D} prior painting. This
     *   argument cannot be {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}.
     */
    public final ListenerRef addPrePaintListener(Runnable listener) {
        return prePaintEvents.registerListener(listener);
    }

    /**
     * Returns the time elapsed since the last asynchronous painting has been
     * drawn to this component. If no painting was done yet, this method returns
     * the time since the creation of this component.
     * <P>
     * Note that this method differs from {@code getSignificantRenderingTime} in
     * that this method considers both
     * {@link RenderingType#SIGNIFICANT_RENDERING significant} and
     * {@link RenderingType#INSIGNIFICANT_RENDERING insignificant} renderings.
     *
     * @param unit the unit of time in which the result is needed. This argument
     *   cannot be {@code null}.
     * @return the time elapse since the last asynchronous painting has been
     *   drawn to this component
     *
     * @throws NullPointerException thrown if the specified time unit argument
     *   is {@code null}.
     */
    public final long getRenderingTime(TimeUnit unit) {
        return lastPaintedState.getRenderingTime(unit);
    }

    /**
     * Returns the time elapsed since the last significant asynchronous painting
     * has been drawn to this component. If no painting was done yet, this
     * method returns the time since the creation of this component.
     * <P>
     * Note that this method differs from {@code getRenderingTime} in that this
     * method only considers rendering which were
     * {@link RenderingType#SIGNIFICANT_RENDERING significant}.
     *
     * @param unit the unit of time in which the result is needed. This argument
     *   cannot be {@code null}.
     * @return the time elapse since the last significant asynchronous painting
     *   has been drawn to this component
     *
     * @throws NullPointerException thrown if the specified time unit argument
     *   is {@code null}.
     */
    public final long getSignificantRenderingTime(TimeUnit unit) {
        return lastSignificantPaintedState.getRenderingTime(unit);
    }

    /**
     * Returns {@code true} if an synchronous rendering of this component is
     * in progress.
     *
     * @return {@code true} if an synchronous rendering of this component is
     *   in progress, {@code false} otherwise
     */
    public boolean isRendering() {
        return !lastSignificantPaintedState.isRenderingFinished();
    }

    /**
     * Sets the {@link AsyncRenderer} to use by this component to render
     * asynchronously. The {@code AsyncRenderer} must be set before this
     * component is made displayable.
     * <P>
     * This method may only be called if the {@code AsyncRenderer} has not been
     * specified at construction time and this method has not yet been called.
     *
     * @param asyncRenderer the {@code AsyncRenderer} used to render this
     *   component. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public final void setAsyncRenderer(AsyncRendererFactory asyncRenderer) {
        ExceptionHelper.checkNotNullArgument(asyncRenderer, "asyncRenderer");
        if (this.asyncRenderer != null) {
            throw new IllegalStateException("The AsyncRenderer for this component has already been set.");
        }

        this.asyncRenderer = asyncRenderer.createRenderer();
        if (this.asyncRenderer == null) {
            throw new IllegalArgumentException("AsyncRendererFactory returned a null renderer.");
        }
    }

    /**
     * Sets the {@code ImageRenderer} which will be used to render the content
     * of this component. Using this method assumes that no data is provided
     * for the passed {@code ImageRenderer}, so its
     * {@link ImageRenderer#render(CancellationToken, Object, BufferedImage) render}
     * method will not be called, every rendering must be done in the
     * {@link ImageRenderer#startRendering(CancellationToken, BufferedImage) startRendering}
     * or in the {@link ImageRenderer#finishRendering(CancellationToken, AsyncReport, BufferedImage) finishRendering}
     * method.
     * <P>
     * This method is equivalent to calling
     * {@code setRenderingArgs(null, componentRenderer, null}.
     * <P>
     * <B>Note</B>: Calling any of the {@code setRenderingArgs} methods will
     * overwrite the previous {@code setRenderingArgs} method calls.
     *
     * @param componentRenderer the {@code ImageRenderer} which is used to
     *   render the content of this component. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code ImageRenderer} is {@code null}
     *
     * @see #setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook) setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook)
     */
    protected final void setRenderingArgs(ImageRenderer<?, ?> componentRenderer) {
        setRenderingArgsBridge(componentRenderer);
    }

    // This is just a bridge method to satisfy the compiler because it cannot
    // prove correctness without <DataType>.
    private <DataType> void setRenderingArgsBridge(ImageRenderer<DataType, ?> componentRenderer) {
        setRenderingArgs(null, componentRenderer, null);
    }

    /**
     * Sets the {@code ImageRenderer} which will be used to render the content
     * of this component with an {@link AsyncDataLink} which is used to provide
     * input for the {@link ImageRenderer#render(CancellationToken, Object, BufferedImage) render}
     * method of the passed {@code ImageRenderer}. The {@code render} method
     * will be called with the data received from the data link (possibly
     * omitting some of the data but the last).
     * <P>
     * This method is equivalent to calling
     * {@code setRenderingArgs(dataLink, componentRenderer, null}.
     * <P>
     * <B>Note</B>: Calling any of the {@code setRenderingArgs} methods will
     * overwrite the previous {@code setRenderingArgs} method calls.
     *
     * @param <DataType> the type of the data provided by the passed
     *   {@code AsyncDataLink}
     * @param dataLink the {@code AsyncDataLink} which is used to provide data
     *   for the {@code render} method of the {@code ImageRenderer}. This
     *   argument can be {@code null} which is equivalent to passing a data link
     *   which immediately completes successfully without providing any data.
     * @param componentRenderer the {@code ImageRenderer} which is used to
     *   render the content of this component. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code ImageRenderer} is {@code null}
     *
     * @see #setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook) setRenderingArgs(AsyncDataLink, ImageRenderer, PaintHook)
     */
    protected final <DataType> void setRenderingArgs(
            AsyncDataLink<DataType> dataLink,
            ImageRenderer<? super DataType, ?> componentRenderer) {
        setRenderingArgs(dataLink, componentRenderer, null);
    }

    /**
     * Sets the {@code ImageRenderer} which will be used to render the content
     * of this component with an {@link AsyncDataLink} which is used to provide
     * input for the {@link ImageRenderer#render(CancellationToken, Object, BufferedImage) render}
     * method of the passed {@code ImageRenderer}; and an interface which can
     * further define the rendering of this component. The {@code render} method
     * will be called with the data received from the data link (possibly
     * omitting some of the data but the last).
     * <P>
     * The provided {@code PaintHook} is called before and after the rendering
     * and so it can be used to do additional rendering on the AWT Event
     * Dispatch Thread.
     * <P>
     * This method is equivalent to calling
     * {@code setRenderingArgs(dataLink, componentRenderer, null}.
     * <P>
     * <B>Note</B>: Calling any of the {@code setRenderingArgs} methods will
     * overwrite the previous {@code setRenderingArgs} method calls.
     *
     * @param <DataType> the type of the data provided by the passed
     *   {@code AsyncDataLink}
     * @param <ResultType> the type of the object returned by the
     *   specified {@code ImageRenderer}. This result is to be passed to the
     *   {@link PaintHook#postPaintComponent(RenderingState, Object, Graphics2D) postPaintComponent}
     *   method of the specified {@code PaintHook} object.
     * @param dataLink the {@code AsyncDataLink} which is used to provide data
     *   for the {@code render} method of the {@code ImageRenderer}. This
     *   argument can be {@code null} which is equivalent to passing a data link
     *   which immediately completes successfully without providing any data.
     * @param componentRenderer the {@code ImageRenderer} which is used to
     *   render the content of this component. This argument cannot be
     *   {@code null}.
     * @param paintHook the {@code PaintHook} object which is called before
     *   and after the rendering. This argument can be {@code null} if
     *   such callback is not required.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code ImageRenderer} is {@code null}
     */
    protected final <DataType, ResultType> void setRenderingArgs(
            AsyncDataLink<DataType> dataLink,
            ImageRenderer<? super DataType, ResultType> componentRenderer,
            PaintHook<ResultType> paintHook) {
        setRenderingArgs(new Renderer<>(dataLink, componentRenderer, paintHook));
    }

    private void setRenderingArgs(Renderer<?, ?> renderer) {
        this.renderer = renderer;
        repaint();
    }

    /**
     * Causes the previously set {@code ImageRenderer} to be called again to
     * render this component. Calling this method is equivalent to calling
     * the {@code setRenderingArgs} method with the same arguments as done
     * previously. Calling this method before calling any of the
     * {@code setRenderingArgs} method has no effect.
     */
    protected final void renderAgain() {
        if (renderer != null) {
            setRenderingArgs(renderer.createCopy());
        }
    }

    /**
     * Called to render this component when no rendering has been done by the
     * previously set {@code ImageRenderer}. This method is called on the
     * AWT Event Dispatch Thread and subclasses may override this method to
     * provide a user defined implementation.
     * <P>
     * The default implementation will simply fill the passed {@code Graphics2D}
     * object with the currently set background color.
     *
     * @param g the {@code Graphics2D} to paint to render this component. This
     *   argument cannot be {@code null}. The graphics context is not need to be
     *   preserved by this method.
     */
    protected void paintDefault(Graphics2D g) {
        g.setBackground(getBackground());
        g.clearRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Renders this component. Subclasses cannot override this method, they
     * can call one of the {@code setRenderingArgs} methods and override the
     * {@link #paintDefault(Graphics2D) paintDefault} method to define how this
     * component should be rendered.
     *
     * @param g the {@code Graphics2D} to paint to. This argument cannot be
     *   {@code null}.
     */
    @Override
    protected final void paintComponent2D(Graphics2D g) {
        prePaintEvents.onEvent(RunnableDispatcher.INSTANCE, null);

        if (asyncRenderer == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "No component painter was specified "
                        + "for this component.");
            }

            asyncRenderer = DEFAULT_RENDERER.createRenderer();
        }

        final int width = getWidth();
        final int height = getHeight();
        drawingConnector.setRequiredWidth(width, height);

        if (renderer == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "setRenderingArgs has not yet been"
                        + " called and the component is being rendered.");
            }
            g.setBackground(getBackground());
            g.clearRect(0, 0, width, height);
        }
        else {
            RenderingState state = lastRenderingState;
            if (renderer != lastExecutedRenderer || state == null) {
                lastExecutedRenderer = renderer;
                lastRenderingState = null;

                state = renderer.render(getRequiredDrawingSurfaceType());
                lastRenderingState = state;
            }

            if (renderer.prePaintComponent(state, g)) {
                GraphicsCopyResult<InternalResult<?>> copyResult
                        = drawingConnector.copyMostRecentGraphics(g, width, height);
                InternalResult<?> internalResult = copyResult.getPaintResult();

                if (copyResult.isPainted() && internalResult != null) {
                    if (internalResult.getRenderingType() != RenderingType.NO_RENDERING) {
                        setLastPaintedState(state);
                    }

                    if (internalResult.getRenderingType() == RenderingType.SIGNIFICANT_RENDERING) {
                        setLastSignificantPaintedState(state);
                    }

                    internalResult.postPaintComponent(state, g);
                }
                else {
                    paintDefault(g);
                }
            }
        }
    }

    private void displayResult() {
        // Instead of calling repaint directly, we check if it was disposed.
        repaintRequester.execute(new Runnable() {
            @Override
            public void run() {
                if (isDisplayable()) {
                    repaint();
                }
            }
        });
    }

    private class InternalResult<ResultType> {
        private final RenderingResult<ResultType> result;
        private final PaintHook<ResultType> paintHook;

        public InternalResult(
                RenderingResult<ResultType> result,
                PaintHook<ResultType> paintHook) {
            assert result != null;
            assert paintHook != null;

            this.result = result;
            this.paintHook = paintHook;
        }

        public RenderingType getRenderingType() {
            return result.getType();
        }

        public void postPaintComponent(RenderingState state, Graphics2D g) {
            paintHook.postPaintComponent(state, result.getResult(), g);
        }
    }

    private class Renderer<DataType, ResultType> {
        private final AsyncDataLink<DataType> dataLink;
        private final ImageRenderer<? super DataType, ResultType> componentRenderer;
        private final PaintHook<ResultType> paintHook;

        public Renderer(
                AsyncDataLink<DataType> dataLink,
                ImageRenderer<? super DataType, ResultType> componentRenderer,
                PaintHook<ResultType> paintHook) {
            ExceptionHelper.checkNotNullArgument(componentRenderer, "componentRenderer");

            this.dataLink = dataLink;
            this.componentRenderer = componentRenderer;
            this.paintHook = paintHook;
        }

        // This method is needed because we detect that the component needs to
        // be rendered again by checking if the Renderer object has changed
        // (reference comparison).
        public Renderer<DataType, ResultType> createCopy() {
            return new Renderer<>(dataLink, componentRenderer, paintHook);
        }

        public boolean prePaintComponent(RenderingState state, Graphics2D g) {
            return paintHook != null
                    ? paintHook.prePaintComponent(state, g)
                    : true;
        }

        private void presentResult(BufferedImage surface, RenderingResult<ResultType> result) {
            if (result == null) {
                LOGGER.severe("Component renderer returned null as result.");
                return;
            }

            if (result.hasRendered()) {
                InternalResult<?> internalResult = paintHook != null
                        ? new InternalResult<>(result, paintHook)
                        : null;
                drawingConnector.presentNewImage(surface, internalResult);
                displayResult();
            }
            else {
                drawingConnector.offerBuffer(surface);
            }
        }

        public RenderingState render(final int bufferType) {
            DataRenderer<DataType> dataRenderer = new DataRenderer<DataType>() {
                @Override
                public boolean startRendering(CancellationToken cancelToken) {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.startRendering(cancelToken, surface);
                        return result.isSignificant();
                    } finally {
                        presentResult(surface, result);
                    }
                }

                @Override
                public boolean willDoSignificantRender(DataType data) {
                    return componentRenderer.willDoSignificantRender(data);
                }

                @Override
                public boolean render(CancellationToken cancelToken, DataType data) {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.render(cancelToken, data, surface);
                        return result.isSignificant();
                    } finally {
                        presentResult(surface, result);
                    }
                }

                @Override
                public void finishRendering(CancellationToken cancelToken, AsyncReport report) {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.finishRendering(cancelToken, report, surface);
                    } finally {
                        presentResult(surface, result);
                    }
                }
            };

            return asyncRenderer.render(Cancellation.UNCANCELABLE_TOKEN, dataLink, dataRenderer);
        }
    }

    private static class NoOpRenderingState implements RenderingState {
        private final long startTime;

        public NoOpRenderingState() {
            this.startTime = System.nanoTime();
        }

        @Override
        public boolean isRenderingFinished() {
            return true;
        }

        @Override
        public long getRenderingTime(TimeUnit unit) {
            return unit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }

        @Override
        public AsyncDataState getAsyncDataState() {
            return null;
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
