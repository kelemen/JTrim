package org.jtrim2.image.async;

import java.awt.image.BufferedImage;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadUpdateListener;

/**
 * Defines a convenient base class for {@code IIOReadUpdateListener}
 * implementations when you only want to implement some of its methods.
 * <P>
 * All the implemented methods of {@code IIOReadUpdateAdapter} does nothing
 * but return immediately and subclasses may override them for their purpose.
 *
 * <h2>Thread safety</h2>
 * Since the methods of {@code IIOReadUpdateAdapter} does nothing, they are
 * safe to be accessed from multiple threads. Subclasses of
 * {@code IIOReadUpdateAdapter} are not required to be safe to be accessed
 * by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Since the methods of {@code IIOReadUpdateAdapter} does nothing, they are
 * <I>synchronization transparent</I>. As a listener interface, subclasses of
 * {@code IIOReadUpdateAdapter} are not required to be
 * <I>synchronization transparent</I> but should return reasonably fast.
 */
public class IIOReadUpdateAdapter implements IIOReadUpdateListener {
    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void passStarted(
            ImageReader source,
            BufferedImage theImage,
            int pass,
            int minPass,
            int maxPass,
            int minX,
            int minY,
            int periodX,
            int periodY,
            int[] bands) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void imageUpdate(
            ImageReader source,
            BufferedImage theImage,
            int minX,
            int minY,
            int width,
            int height,
            int periodX,
            int periodY,
            int[] bands) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void passComplete(ImageReader source, BufferedImage theImage) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailPassStarted(
            ImageReader source,
            BufferedImage theThumbnail,
            int pass,
            int minPass,
            int maxPass,
            int minX,
            int minY,
            int periodX,
            int periodY,
            int[] bands) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailUpdate(
            ImageReader source,
            BufferedImage theThumbnail,
            int minX,
            int minY,
            int width,
            int height,
            int periodX,
            int periodY,
            int[] bands) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailPassComplete(
            ImageReader source,
            BufferedImage theThumbnail) {
    }

}
