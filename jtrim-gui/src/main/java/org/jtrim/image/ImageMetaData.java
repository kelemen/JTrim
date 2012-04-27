package org.jtrim.image;

import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a base class for the meta data of an image. The meta data must at
 * least contain the dimensions of the image and if the image is a complete or
 * partial image.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads. The
 * methods of the subclasses of this class must be safe to be accessed
 * concurrently as well. {@code ImageMetaData} instances are immutable and
 * subclasses should be implemented to be immutable as well.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I> and the
 * subclasses of this class must be <I>synchronization transparent</I> as well.
 *
 * @see ImageData
 *
 * @author Kelemen Attila
 */
public class ImageMetaData {
    private final int width;
    private final int height;
    private final boolean complete;

    /**
     * Initializes the {@code ImageMetaData} with the specified properties.
     *
     * @param width the width in number of pixels of the image whose meta data
     *   is the newly created {@code ImageMetaData}. This argument must be
     *   greater than or equal to zero.
     * @param height the height in number of pixels of the image whose meta data
     *   is the newly created {@code ImageMetaData}. This argument must be
     *   greater than or equal to zero.
     * @param complete a {@code boolean} indicating if the image whose meta data
     *   is the newly created {@code ImageMetaData} is a complete or partial
     *   image. If this argument is {@code true}, the image is completely
     *   loaded. If {@coda false}, the image is only partially available.
     *
     * @throws IllegalArgumentException thrown if the width or height argument
     *   is illegal
     */
    public ImageMetaData(int width, int height, boolean complete) {
        ExceptionHelper.checkArgumentInRange(width, 0, Integer.MAX_VALUE, "width");
        ExceptionHelper.checkArgumentInRange(height, 0, Integer.MAX_VALUE, "height");

        this.width = width;
        this.height = height;
        this.complete = complete;
    }

    /**
     * Returns {@code true} if the image whose meta data is this
     * {@code ImageMetaData} is completely available.
     *
     * @return {@code true} if the image whose meta data is this
     *   {@code ImageMetaData} is completely available, {@code false} if the
     *   image is only partially available
     */
    public final boolean isComplete() {
        return complete;
    }

    /**
     * Returns the height in number of pixels whose meta data is this
     * {@code ImageMetaData}.
     *
     * @return the height in number of pixels whose meta data is this
     *   {@code ImageMetaData}. This method always returns an integer greater
     *   tan or equal to zero.
     */
    public final int getHeight() {
        return height;
    }

    /**
     * Returns the width in number of pixels whose meta data is this
     * {@code ImageMetaData}.
     *
     * @return the width in number of pixels whose meta data is this
     *   {@code ImageMetaData}. This method always returns an integer greater
     *   tan or equal to zero.
     */
    public final int getWidth() {
        return width;
    }
}
