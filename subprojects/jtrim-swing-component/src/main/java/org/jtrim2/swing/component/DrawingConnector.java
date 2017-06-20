package org.jtrim2.swing.component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.utils.ExceptionHelper;

/**
 * A helper class for triple buffering when rendering to a {@code BufferedImage}.
 * <P>
 * This class might be useful in the following scenario: Assume that there is
 * a thread doing the actual rendering and it is rendering to a
 * {@code BufferedImage}. Once it finished rendering, it needs to publish the
 * result of the rendering some way. It may choose to simply submit the
 * {@code BufferedImage} to another thread to display the results and continue
 * the subsequent rendering task. However, to start the another rendering it
 * will need another {@code BufferedImage} to render to. If it creates a new
 * {@code BufferedImage}, it might cause a heavy load on the garbage collector
 * (since {@code BufferedImage} objects are heavy).
 * <P>
 * This class helps in the above mentioned scenario when the thread displaying
 * the image can cooperate with the thread doing the rendering. That is, this
 * class is intended to be used the following way:
 * <ol>
 *  <li>
 *   Once the renderer needs a {@code BufferedImage} to render to, it needs to
 *   call the {@link #getDrawingSurface(int) getDrawingSurface} method to
 *   retrieve a {@code BufferedImage}.
 *  </li>
 *  <li>
 *   After rendering completes, the renderer thread must call the
 *   {@link #presentNewImage(BufferedImage, Object) presentNewImage} method to
 *   publish the result of the rendering and then notify in some way the
 *   displaying thread that a new rendering result is available. Then, the
 *   rendering thread may continue rendering again.
 *  </li>
 *  <li>
 *   Once the displaying thread knows that a new rendering process has
 *   completed, it needs to call the {@link #copyMostRecentGraphics(Graphics2D, int, int) copyMostRecentGraphics}
 *   method to copy the result of the rendering to a {@code Graphics2D} object.
 *  </li>
 * </ol>
 * This class works by storing a reference to two {@code BufferedImage}
 * instances, one is the last result presented through the
 * {@code presentNewImage} and another one which is used to returned by the
 * {@code getDrawingSurface} method call. Notice that {@code presentNewImage}
 * method calls overwrite each other's result, so after the second invocation
 * the currently stored {@code BufferedImage} will be useless for the
 * {@code DrawingConnector}. This is the image which will be returned by the
 * {@code getDrawingSurface} method.
 * <P>
 * This class is allowed to be subclassed for the sole purpose of overriding
 * the {@code scaleToGraphics} method to provide a user defined implementation.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the object which might be attached to a
 *   rendered {@code BufferedImage}
 */
public class DrawingConnector<ResultType> {
    private int requiredWidth;
    private int requiredHeight;

    private ResultType mostRecentPaintResult;
    private BufferedImage mostRecent;

    private SoftReference<BufferedImage> cachedImageRef;

    private final Lock bufferLock;

    /**
     * Creates a new {@code DrawingConnector} with the specified initial
     * width and height of the image to return by the
     * {@link #getDrawingSurface(int) getDrawingSurface} method. This can be
     * later overwritten by a {@link #setRequiredWidth(int, int) setRequiredWidth}
     * method call.
     *
     * @param width the initial width of the image to return by the
     *   {@code getDrawingSurface} method. This argument must be greater than or
     *   equal to zero.
     * @param height the initial height of the image to return by the
     *   {@code getDrawingSurface} method. This argument must be greater than or
     *   equal to zero.
     *
     * @throws IllegalArgumentException thrown if the specified width or height
     *   is a negative integer
     */
    public DrawingConnector(int width, int height) {
        ExceptionHelper.checkArgumentInRange(width, 0, Integer.MAX_VALUE, "width");
        ExceptionHelper.checkArgumentInRange(height, 0, Integer.MAX_VALUE, "height");

        this.requiredWidth = width;
        this.requiredHeight = height;
        this.bufferLock = new ReentrantLock();

        this.mostRecent = null;
        this.cachedImageRef = null;
        this.mostRecentPaintResult = null;
    }

    /**
     * Sets the width and height of the image to be returned by subsequent
     * invocations of the {@link #getDrawingSurface(int) getDrawingSurface}
     * method.
     * <P>
     * Concurrent invocations of the {@code getDrawingSurface} may return this
     * or the previous width and height values but this method call can be
     * considered atomic (i.e., it can be assumed that both width and height
     * are set at the same time).
     *
     * @param width the width of the image to be returned by subsequent
     *   invocations of the {@code getDrawingSurface} method. This argument must
     *   be greater than or equal to zero.
     * @param height the height of the image to be returned by subsequent
     *   invocations of the {@code getDrawingSurface} method. This argument must
     *   be greater than or equal to zero.
     *
     * @throws IllegalArgumentException thrown if the specified width or height
     *   is a negative integer
     */
    public final void setRequiredWidth(int width, int height) {
        ExceptionHelper.checkArgumentInRange(width, 0, Integer.MAX_VALUE, "width");
        ExceptionHelper.checkArgumentInRange(height, 0, Integer.MAX_VALUE, "height");

        bufferLock.lock();
        try {
            this.requiredWidth = width;
            this.requiredHeight = height;
        } finally {
            bufferLock.unlock();
        }
    }

    /**
     * Checks if the {@link #presentNewImage(BufferedImage, Object) presentNewImage}
     * has ever been called or not. That is, if this method returns {@code true}
     * every subsequent invocations of this method will also return
     * {@code true}. Also, if this method returns {@code true}, subsequent
     * invocations of the {@link #copyMostRecentGraphics(Graphics2D, int, int) copyMostRecentGraphics}
     * method will actually draw image to the passed {@code Graphics2D} object.
     *
     * @return {@code true} if the {@code presentNewImage} has been called at
     *   least once, {@code false} otherwise
     */
    public final boolean hasImage() {
        boolean result;

        bufferLock.lock();
        try {
            result = mostRecent != null;
        } finally {
            bufferLock.unlock();
        }

        return result;
    }

    /**
     * Called by the {@link #copyMostRecentGraphics(Graphics2D, int, int) copyMostRecentGraphics}
     * method to draw the most recently published image to the specified
     * {@code Graphics2D} object.
     * <P>
     * Subclasses may override this method to provide a user defined behaviour.
     * The default implementation simply draws the source image to the upper
     * left corner of the {@code Graphics2D} object without any scaling.
     *
     * <h3>Synchronization transparency</h3>
     * This method is called while holding a lock and therefore it should
     * refrain from doing anything not transparent to synchronization. In
     * particular, it must not acquire locks which might be held while calling
     * any of the methods of this {@code DrawingConnector}. The intent of this
     * method is simply to scale the image to the provided graphics.
     *
     * @param destination the {@code Graphics2D} object to which the specified
     *   {@code BufferedImage} must be drawn to. This argument cannot be
     *   {@code null}.
     * @param destWidth the width (in number of pixels) of the destination
     *   graphics to draw to. This argument must be greater than or equal to
     *   zero.
     * @param destHeight the height (in number of pixels) of the destination
     *   graphics to draw to. This argument must be greater than or equal to
     *   zero.
     * @param src the {@code BufferedImage} the image which is to be drawn to
     *   the specified graphics object. This argument cannot be {@code null}.
     * @param paintResult the object associated with the passed image (i.e.: was
     *   provided to the {@code presentNewImage}) method. This argument can be
     *   {@code null} if the provider of the image may pass {@code null}
     *   objects.
     */
    protected void scaleToGraphics(Graphics2D destination,
            int destWidth,
            int destHeight,
            BufferedImage src,
            ResultType paintResult) {
        destination.drawImage(src, null, 0, 0);
    }

    /**
     * Copies the most recent image provided to the
     * {@link #presentNewImage(BufferedImage, Object) presentNewImage} method
     * to the specified {@code Graphics2D} object. This method is allowed to
     * be called prior calling {@code presentNewImage} but it will not draw
     * anything to the provided graphics in this case.
     * <P>
     * Note that this method relies on the
     * {@link #scaleToGraphics(Graphics2D, int, int, BufferedImage, Object) scaleToGraphics}
     * method to actually draw the image to the graphics.
     *
     * @param destination the {@code Graphics2D} to which the current image is
     *   to drawn to. This argument cannot be {@code null}.
     * @param width the width of the specified graphics. Any painting must be
     *   done within this limit. This argument must be greater than or equal to
     *   zero.
     * @param height the height of the specified graphics. Any painting must be
     *   done within this limit. This argument must be greater than or equal to
     *   zero.
     * @return a {@code GraphicsCopyResult} object which stores the object
     *   associated with the currently drawn image and a boolean which is
     *   {@code false} if this method could not copy anything to the specified
     *   graphics because there was no image provided yet. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified {@code Graphics2D}
     *   object is {@code null}
     * @throws IllegalArgumentException thrown if the specified width or height
     *   is a negative integer
     */
    public final GraphicsCopyResult<ResultType> copyMostRecentGraphics(Graphics2D destination, int width, int height) {
        Objects.requireNonNull(destination, "destination");
        ExceptionHelper.checkArgumentInRange(width, 0, Integer.MAX_VALUE, "width");
        ExceptionHelper.checkArgumentInRange(height, 0, Integer.MAX_VALUE, "height");

        ResultType paintResult;
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

    /**
     * Offers a {@code BufferedImage} which might be returned by the
     * {@link #getDrawingSurface(int) getDrawingSurface} method of this
     * {@code DrawingConnector}. The caller must promise that the provided image
     * will no longer be accessed by outside code. This method is not required
     * to actually use the passed image, it may discard it as it sees fit.
     *
     * @param image the {@code BufferedImage} which might be used by this
     *   {@code DrawingConnector} when it needs to create a new image. This
     *   argument can be {@code null}, in which case this method will simply
     *   return {@code false}.
     * @return {@code true} if this method accepted the passed image,
     *   {@code false} if this method discarded the passed image. In case this
     *   method returns {@code false}, the caller might access this image after
     *   this method returns.
     */
    public final boolean offerBuffer(BufferedImage image) {
        if (image == null || image.getType() == BufferedImage.TYPE_CUSTOM) {
            return false;
        }

        boolean result = false;

        bufferLock.lock();
        try {
            if (image.getWidth() == requiredWidth && image.getHeight() == requiredHeight) {
                if (cachedImageRef == null) {
                    cachedImageRef = new SoftReference<>(image);
                    result = true;
                } else {
                    BufferedImage cachedImage = cachedImageRef.get();

                    if (cachedImage == null
                            || cachedImage.getWidth() != requiredWidth
                            || cachedImage.getHeight() != requiredHeight) {

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

    /**
     * Specifies the most recent result of the rendering process. Until this
     * method is called again, subsequent calls to the
     * {@link #copyMostRecentGraphics(Graphics2D, int, int) copyMostRecentGraphics}
     * method will copy the image passed to this method.
     *
     * @param image the {@code BufferedImage} which is to be copied by
     *   subsequent {@code copyMostRecentGraphics} method calls. This argument
     *   cannot be {@code null}.
     * @param paintResult an arbitrary object attached to the rendered image.
     *   This object will be returned by the {@code copyMostRecentGraphics}
     *   method when it copies the currently passed image. This argument is
     *   allowed to be {@code null}.
     *
     * @throws NullPointerException thrown if the specified image is
     *   {@code null}
     */
    public final void presentNewImage(BufferedImage image, ResultType paintResult) {
        Objects.requireNonNull(image, "image");

        bufferLock.lock();
        try {
            BufferedImage cachedImage = mostRecent;
            mostRecent = image;
            mostRecentPaintResult = paintResult;

            // if the cached image does not have the required size,
            // we will make it reclaimable by the GC because
            // it is unlikely that we could use this buffer for
            // anything.
            if (cachedImage != null
                    && cachedImage.getWidth() == requiredWidth
                    && cachedImage.getHeight() == requiredHeight) {
                cachedImageRef = new SoftReference<>(cachedImage);
            } else {
                cachedImageRef = null;
            }
        } finally {
            bufferLock.unlock();
        }
    }

    /**
     * Returns a {@code BufferedImage} to render to. The size of the returned
     * image is the size last set by a previous
     * {@link #setRequiredWidth(int, int) setRequiredWidth} method call.
     * <P>
     * In case the required width is 0, then the width of the returned image
     * will be 1. Similarly, in case the required height is 0, then the height
     * of the returned image is 1.
     *
     * @param bufferType the type of the {@code BufferedImage}
     *   (i.e., {@code BufferedImage.getType()} to be returned by this method.
     *   This argument must be a valid type for a {@code BufferedImage}
     *   (e.g.: {@code BufferedImage.TYPE_INT_ARGB}).
     * @return a new {@code BufferedImage} with the specified type. This method
     *   never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified type is not
     *   a valid type for a {@code BufferedImage}
     */
    public final BufferedImage getDrawingSurface(int bufferType) {
        BufferedImage result = null;

        int width;
        int height;

        bufferLock.lock();
        try {
            width = requiredWidth;
            height = requiredHeight;

            BufferedImage cachedImage = cachedImageRef != null
                    ? cachedImageRef.get()
                    : null;

            if (cachedImage != null) {
                result = cachedImage;
                cachedImageRef = null;
            }
        } finally {
            bufferLock.unlock();
        }

        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        if (result == null) {
            result = new BufferedImage(width, height, bufferType);
        } else {
            if (result.getType() != bufferType
                    || result.getWidth() != width
                    || result.getHeight() != height) {

                result = new BufferedImage(width, height, bufferType);
            }
        }

        return result;
    }
}
