/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;


import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.image.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.swing.*;
import org.jtrim.image.*;
import org.jtrim.swing.concurrent.*;
import org.jtrim.swing.concurrent.async.*;

/**
 *
 * @author Kelemen Attila
 */
public abstract class SlowDrawingComponent
extends
        JPanel
implements
        ComponentRenderer {

    private static final long serialVersionUID = 1092076813185568380L;
    private static final int RENDERING_STATE_POLL_TIME_MS = 100;


    private final SwingUpdateTaskExecutor repaintRequester;
    private AsyncComponentRenderer componentRenderer;
    private DrawingConnector drawingConnector;
    private Object lastPaintResult;
    private RenderingFuture lastRenderingFuture;
    private int lastWidth;
    private int lastHeight;
    private boolean dirty;
    private int drawingPriority;

    private ColorModel bufferTypeModel;
    private int bufferType;

    private long remainingRenderingTimeNanos;
    private boolean needLongRendering;
    private long renderingPatienceNanos;

    private ActionListener renderingListenerAction;
    private BufferedImage fallbackImage;

    public SlowDrawingComponent() {
        this(null, AsyncComponentRenderer.DEFAULT_PRIORITY);
    }

    public SlowDrawingComponent(AsyncComponentRenderer componentRenderer, int drawingPriority) {
        super();

        this.repaintRequester = new SwingUpdateTaskExecutor();
        this.fallbackImage = null;
        this.renderingListenerAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkRendering();
            }
        };

        this.needLongRendering = false;
        this.renderingPatienceNanos = TimeUnit.MILLISECONDS.toNanos(100);
        this.remainingRenderingTimeNanos = 0;

        this.lastWidth = 0;
        this.lastHeight = 0;
        this.lastPaintResult = null;
        this.drawingPriority = drawingPriority;
        this.dirty = true;
        this.componentRenderer = componentRenderer;
        this.bufferTypeModel = null;
        this.bufferType = BufferedImage.TYPE_INT_RGB;
        this.drawingConnector = new SimpleDrawingConnector(getWidth(), getHeight());
    }

    public void setComponentRenderer(AsyncComponentRenderer componentRenderer) {
        if (this.componentRenderer != null) {
            throw new IllegalStateException("The component renderer was"
                    + " already set for this component.");
        }
        this.componentRenderer = componentRenderer;
    }

    public abstract RenderingParameters getCurrentRenderingParams();

    public void clearComponent() {
        drawingConnector = new SimpleDrawingConnector(getWidth(), getHeight());
        setDirty();
    }

    protected Object getLastPaintResult() {
        return lastPaintResult;
    }

    public int getDrawingPriority() {
        return drawingPriority;
    }

    public void setDrawingPriority(int drawingPriority) {
        this.drawingPriority = drawingPriority;
    }

    public boolean isDirty() {
        return dirty || getHeight() != lastHeight || getWidth() != lastWidth;
    }

    public void setDirty() {
        this.dirty = true;
        repaint();
    }

    public void setInfLongRenderTimeout() {
        needLongRendering = false;
    }

    public void setLongRenderingTimeout(long time, TimeUnit timeunit) {
        this.renderingPatienceNanos = timeunit.toNanos(time);
        this.needLongRendering = true;
    }

    protected void postLongRendering(Graphics2D g,
            RenderingFuture renderingFuture, GraphicsCopyResult copyResult) {
    }

    protected void postShortRendering(Graphics2D g,
            RenderingFuture renderingFuture, GraphicsCopyResult copyResult) {
    }

    protected void postRendering(Graphics2D g,
            RenderingFuture renderingFuture, GraphicsCopyResult copyResult) {
    }

    @Override
    protected void paintComponent(Graphics g) {
        int currentWidth = getWidth();
        int currentHeight = getHeight();

        Graphics2D g2d;
        boolean useBufferedImage;

        if (g instanceof Graphics2D) {
            useBufferedImage = false;
            g2d = (Graphics2D)g;
        }
        else {
            useBufferedImage = true;
            if (fallbackImage == null ||
                    fallbackImage.getWidth() != currentWidth ||
                    fallbackImage.getHeight() != currentHeight) {

                fallbackImage = new BufferedImage(currentWidth, currentHeight,
                        BufferedImage.TYPE_INT_RGB);
            }

            g2d = fallbackImage.createGraphics();
        }

        try {
            g2d.setFont(getFont());
            g2d.setColor(getForeground());
            g2d.setBackground(getBackground());

            if (isOpaque()) {
                g2d.clearRect(0, 0, currentWidth, currentHeight);
            }

            RenderingFuture longRenderingFuture = null;

            if (isDirty()) {
                lastWidth = currentWidth;
                lastHeight = currentHeight;
                drawingConnector.setRequiredWidth(currentWidth, currentHeight);
                parallelPaintComponent(getCurrentRenderingParams());
                dirty = false;
            }

            if (needLongRendering && isLongRendering()) {
                longRenderingFuture = lastRenderingFuture;
            }

            GraphicsCopyResult copyResult;
            copyResult = drawingConnector.copyMostRecentGraphics(g2d,
                    currentWidth, currentHeight);

            lastPaintResult = copyResult != null
                    ? copyResult.getPaintResult()
                    : null;

            postRendering(g2d, lastRenderingFuture, copyResult);

            if (longRenderingFuture != null) {
                postLongRendering(g2d, longRenderingFuture, copyResult);
            }
            else {
                postShortRendering(g2d, lastRenderingFuture, copyResult);
            }

        } finally {
            if (useBufferedImage) {
                g2d.dispose();
                g.drawImage(fallbackImage, 0, 0, null);
            }
        }
    }

    private void parallelPaintComponent(RenderingParameters renderingParams) {
        if (lastRenderingFuture != null && !lastRenderingFuture.isRenderingDone()) {
            remainingRenderingTimeNanos += lastRenderingFuture.getRenderingTime();
        }
        else {
            remainingRenderingTimeNanos = 0;
        }

        if (componentRenderer == null) {
            Logger logger = Logger.getLogger(getClass().getName());
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, "No component painter was specified "
                        + "for this component.");
            }

            componentRenderer = new SimpleAsyncComponentRenderer();
        }

        lastRenderingFuture = componentRenderer.renderComponent(
                getDrawingPriority(), this, this, renderingParams,
                drawingConnector);

        startRenderingListener();
    }

    private boolean isLongRendering() {
        if (lastRenderingFuture == null) return false;
        if (lastRenderingFuture.isRenderingDone()) return false;

        return lastRenderingFuture.getRenderingTime()
                + remainingRenderingTimeNanos >= renderingPatienceNanos;
    }

    private void checkRendering() {
        if (lastRenderingFuture != null) {
            if (!lastRenderingFuture.isRenderingDone()) {
                try {
                    if (isLongRendering()) {
                        repaint();
                    }
                } finally {
                    startRenderingListener();
                }
            }
            else {
                lastRenderingFuture = null;
            }
        }
    }

    private void startRenderingListener() {
        if (!needLongRendering) {
            return;
        }

        javax.swing.Timer timer;
        timer = new javax.swing.Timer(RENDERING_STATE_POLL_TIME_MS, renderingListenerAction);
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public int getRequiredDrawingSurfaceType(Object renderingParams, Object blockedData) {
        ColorModel colorModel = getColorModel();
        if (bufferTypeModel != colorModel) {
            bufferType = ImageData.getCompatibleBufferType(getColorModel());
            bufferTypeModel = colorModel;
        }

        return bufferType;
    }

    @Override
    public void displayResult() {
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
}
