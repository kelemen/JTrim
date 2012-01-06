/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.*;

/**
 *
 * @author Kelemen Attila
 */
public final class SimpleDrawingConnector implements DrawingConnector {
    private int requiredWidth;
    private int requiredHeight;

    private Object mostRecentPaintResult;
    private BufferedImage mostRecent;

    private SoftReference<BufferedImage> cachedImageRef;

    private final Lock bufferLock;

    public SimpleDrawingConnector(int width, int height) {
        this.requiredWidth = width;
        this.requiredHeight = height;
        this.bufferLock = new ReentrantLock();

        this.mostRecent = null;
        this.cachedImageRef = null;
        this.mostRecentPaintResult = null;
    }

    @Override
    public void setRequiredWidth(int width, int height) {
        bufferLock.lock();
        try {
            this.requiredWidth = width;
            this.requiredHeight = height;
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public boolean hasImage() {
        boolean result;

        bufferLock.lock();
        try {
            result = mostRecent != null;
        } finally {
            bufferLock.unlock();
        }

        return result;
    }

    protected void scaleToGraphics(Graphics2D destination, int destWidth, int destHeight, BufferedImage src, Object paintResult) {
        destination.drawImage(src, null, 0, 0);
    }

    @Override
    public GraphicsCopyResult copyMostRecentGraphics(Graphics2D destination, int width, int height) {
        Object paintResult;
        boolean hasPainted = false;

        bufferLock.lock();
        try {
            BufferedImage image = mostRecent;
            paintResult = mostRecentPaintResult;

            if (image != null) {
                scaleToGraphics(destination, width, height, image, paintResult);
                hasPainted = true;
            }
        } finally {
            bufferLock.unlock();
        }

        return GraphicsCopyResult.getInstance(hasPainted, paintResult);
    }

    @Override
    public boolean offerBuffer(BufferedImage image) {
        if (image == null) return false;

        boolean result = false;

        bufferLock.lock();
        try {
            if (image.getWidth() == requiredWidth && image.getHeight() == requiredHeight) {
                if (cachedImageRef == null) {
                    cachedImageRef = new SoftReference<>(image);
                    result = true;
                }
                else {
                    BufferedImage cachedImage = cachedImageRef.get();

                    if (cachedImage == null ||
                            cachedImage.getWidth() != requiredWidth ||
                            cachedImage.getHeight() != requiredHeight) {

                        cachedImageRef = new SoftReference<>(image);
                        result = true;
                    }
                }
            }
        } finally {
            bufferLock.unlock();
        }

        return result;
    }

    @Override
    public void presentNewImage(BufferedImage image, Object paintResult) {
        bufferLock.lock();
        try {
            BufferedImage cachedImage = mostRecent;
            mostRecent = image;
            mostRecentPaintResult = paintResult;

            // if the cached image does not have the required size,
            // we will make it reclaimable by the GC because
            // it is unlikely that we could use this buffer for
            // anything.
            if (cachedImage != null &&
                    cachedImage.getWidth() == requiredWidth &&
                    cachedImage.getHeight() == requiredHeight) {
                cachedImageRef = new SoftReference<>(cachedImage);
            }
            else {
                cachedImageRef = null;
            }
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public BufferedImage getDrawingSurface(int bufferType) {
        BufferedImage result = null;

        int width;
        int height;

        bufferLock.lock();
        try {
            width = requiredWidth;
            height = requiredHeight;

            BufferedImage cachedImage = cachedImageRef != null ? cachedImageRef.get() : null;

            if (cachedImage != null) {
                result = cachedImage;
                cachedImageRef = null;
            }
        } finally {
            bufferLock.unlock();
        }

        if (width <= 0 || height <= 0) return null;

        if (result == null) {
            result = new BufferedImage(width, height, bufferType);
        }
        else {
            if (result.getType() != bufferType ||
                    result.getWidth() != width ||
                    result.getHeight() != height) {

                result = new BufferedImage(width, height, bufferType);
            }
        }

        return result;
    }
}
