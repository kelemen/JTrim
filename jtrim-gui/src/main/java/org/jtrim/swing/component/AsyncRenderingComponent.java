package org.jtrim.swing.component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncReport;
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

    public AsyncRenderingComponent() {
        this(null);
    }

    public AsyncRenderingComponent(AsyncRenderer asyncRenderer) {
        this.repaintRequester = new SwingUpdateTaskExecutor(true);
        this.renderingKey = new Object();
        this.asyncRenderer = asyncRenderer;
        this.bufferTypeModel = null;
        this.bufferType = BufferedImage.TYPE_INT_ARGB;
        this.drawingConnector = new SimpleDrawingConnector<>(1, 1);
        this.renderer = null;
        this.lastExecutedRenderer = null;
    }

    private int getRequiredDrawingSurfaceType() {
        ColorModel colorModel = getColorModel();
        if (bufferTypeModel != colorModel) {
            bufferType = ImageData.getCompatibleBufferType(getColorModel());
            bufferTypeModel = colorModel;
        }

        return bufferType;
    }

    public void setAsyncRenderer(AsyncRenderer asyncRenderer) {
        ExceptionHelper.checkNotNullArgument(asyncRenderer, "asyncRenderer");
        if (this.asyncRenderer != DEFAULT_RENDERER) {
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
        this.renderer = new Renderer<>(dataLink, componentRenderer, paintHook);
        repaint();
    }

    protected void paintDefault(Graphics2D g) {
        g.setBackground(getBackground());
        g.clearRect(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void paintComponent2D(Graphics2D g) {
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
                if (copyResult.isPainted()) {
                    InternalResult<?> internalResult = copyResult.getPaintResult();
                    if (internalResult != null) {
                        internalResult.postPaintComponent(state, g);
                    }
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
        private final ResultType result;
        private final PaintHook<ResultType> paintHook;

        public InternalResult(
                ResultType result,
                PaintHook<ResultType> paintHook) {
            assert paintHook != null;

            this.result = result;
            this.paintHook = paintHook;
        }

        public void postPaintComponent(RenderingState state, Graphics2D g) {
            paintHook.postPaintComponent(state, result, g);
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
                        ? new InternalResult<>(result.getResult(), paintHook)
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
}
