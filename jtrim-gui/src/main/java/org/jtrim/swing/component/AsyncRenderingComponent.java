package org.jtrim.swing.component;

import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
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
import org.jtrim.swing.concurrent.async.DataRenderer;
import org.jtrim.swing.concurrent.async.DrawingConnector;
import org.jtrim.swing.concurrent.async.GenericAsyncRenderer;
import org.jtrim.swing.concurrent.async.GraphicsCopyResult;
import org.jtrim.swing.concurrent.async.RenderingState;
import org.jtrim.swing.concurrent.async.SimpleDrawingConnector;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial")
public abstract class AsyncRenderingComponent extends Graphics2DComponent {
    public static interface PaintHook<T> {
        public boolean prePaintComponent(RenderingState state, Graphics2D g);
        public void postPaintComponent(RenderingState state, T renderingResult, Graphics2D g);
    }

    private static final AsyncRenderer DEFAULT_RENDERER
            = new GenericAsyncRenderer(SyncTaskExecutor.getSimpleExecutor());

    private static final Logger LOGGER = Logger.getLogger(AsyncRenderingComponent.class.getName());

    private final SwingUpdateTaskExecutor repaintRequester;
    private final Object renderingKey;
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

    public AsyncRenderingComponent() {
        this(null);
    }

    public AsyncRenderingComponent(AsyncRenderer asyncRenderer) {
        this.prePaintEvents = new CopyOnTriggerListenerManager<>();
        this.repaintRequester = new SwingUpdateTaskExecutor(true);
        this.renderingKey = new Object();
        this.asyncRenderer = null;
        this.bufferTypeModel = null;
        this.bufferType = BufferedImage.TYPE_INT_ARGB;
        this.drawingConnector = new SimpleDrawingConnector<>(1, 1);
        this.renderer = null;
        this.lastExecutedRenderer = null;
        this.lastRenderingState = null;
        this.lastPaintedState = new NoOpRenderingState();
        this.lastSignificantPaintedState = this.lastPaintedState;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                renderAgain();
            }
        });
    }

    private int getRequiredDrawingSurfaceType() {
        ColorModel colorModel = getColorModel();
        if (bufferTypeModel != colorModel) {
            bufferType = ImageData.getCompatibleBufferType(getColorModel());
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

    public final ListenerRef addPrePaintListener(Runnable listener) {
        return prePaintEvents.registerListener(listener);
    }

    public final long getRenderingTime(TimeUnit unit) {
        return lastPaintedState.getRenderingTime(unit);
    }

    public final long getSignificantRenderingTime(TimeUnit unit) {
        return lastSignificantPaintedState.getRenderingTime(unit);
    }

    public final void setAsyncRenderer(AsyncRenderer asyncRenderer) {
        ExceptionHelper.checkNotNullArgument(asyncRenderer, "asyncRenderer");
        if (this.asyncRenderer != null) {
            throw new IllegalStateException("The AsyncRenderer for this component has already been set.");
        }

        this.asyncRenderer = asyncRenderer;
    }

    protected final <DataType, ResultType> void setRenderingArgs(
            ImageRenderer<? super DataType, ResultType> componentRenderer) {
        setRenderingArgs(null, componentRenderer, null);
    }

    protected final <DataType, ResultType> void setRenderingArgs(
            AsyncDataLink<DataType> dataLink,
            ImageRenderer<? super DataType, ResultType> componentRenderer) {
        setRenderingArgs(dataLink, componentRenderer, null);
    }

    protected final <DataType, ResultType> void setRenderingArgs(
            AsyncDataLink<DataType> dataLink,
            ImageRenderer<? super DataType, ResultType> componentRenderer,
            PaintHook<ResultType> paintHook) {
        setRenderingArgs(new Renderer<>(dataLink, componentRenderer, paintHook));
    }

    private <DataType, ResultType> void setRenderingArgs(Renderer<?, ?> renderer) {
        this.renderer = renderer;
        repaint();
    }

    protected final void renderAgain() {
        setRenderingArgs(renderer != null ? renderer.createCopy() : null);
    }

    protected void paintDefault(Graphics2D g) {
        g.setBackground(getBackground());
        g.clearRect(0, 0, getWidth(), getHeight());
    }

    @Override
    protected final void paintComponent2D(Graphics2D g) {
        prePaintEvents.onEvent(RunnableDispatcher.INSTANCE, null);

        if (asyncRenderer == null) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "No component painter was specified "
                        + "for this component.");
            }

            asyncRenderer = DEFAULT_RENDERER;
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
                public boolean startRendering() {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.startRendering(surface);
                        return result.isSignificant();
                    } finally {
                        presentResult(surface, result);
                    }
                }

                @Override
                public boolean render(DataType data) {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.render(data, surface);
                        return result.isSignificant();
                    } finally {
                        presentResult(surface, result);
                    }
                }

                @Override
                public void finishRendering(AsyncReport report) {
                    RenderingResult<ResultType> result = RenderingResult.noRendering();
                    BufferedImage surface = drawingConnector.getDrawingSurface(bufferType);
                    try {
                        result = componentRenderer.finishRendering(report, surface);
                    } finally {
                        presentResult(surface, result);
                    }
                }
            };

            return asyncRenderer.render(renderingKey, Cancellation.UNCANCELABLE_TOKEN, dataLink, dataRenderer);
        }
    }

    private static class NoOpRenderingState implements RenderingState {
        private final long startTime;

        public NoOpRenderingState() {
            this.startTime = System.nanoTime();
        }

        @Override
        public boolean isRenderingFinished() {
            return false;
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
