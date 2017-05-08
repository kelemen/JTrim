package org.jtrim2.image.async;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

/**
 * Defines a convenient base class for {@code IIOReadProgressListener}
 * implementations when you only want to implement some of its methods.
 * <P>
 * All the implemented methods of {@code IIOReadProgressAdapter} does nothing
 * but return immediately and subclasses may override them for their purpose.
 *
 * <h3>Thread safety</h3>
 * Since the methods of {@code IIOReadProgressAdapter} does nothing, they are
 * safe to be accessed from multiple threads. Subclasses of
 * {@code IIOReadProgressAdapter} are not required to be safe to be accessed
 * by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Since the methods of {@code IIOReadProgressAdapter} does nothing, they are
 * <I>synchronization transparent</I>. As a listener interface, subclasses of
 * {@code IIOReadProgressAdapter} are not required to be
 * <I>synchronization transparent</I> but should return reasonably fast.
 */
public class IIOReadProgressAdapter implements IIOReadProgressListener {
    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void sequenceStarted(ImageReader source, int minIndex) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void sequenceComplete(ImageReader source) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void imageStarted(ImageReader source, int imageIndex) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void imageProgress(ImageReader source, float percentageDone) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void imageComplete(ImageReader source) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailProgress(ImageReader source, float percentageDone) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void thumbnailComplete(ImageReader source) {
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method does nothing if not overridden
     * in subclasses.
     */
    @Override
    public void readAborted(ImageReader source) {
    }
}
