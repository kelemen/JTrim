package org.jtrim.image;

import java.awt.image.BufferedImage;
import org.jtrim.cache.MemoryHeavyObject;

/**
 * Defines an image which was retrieved from an external source. Apart from the
 * image itself, the {@code ImageResult} contains the meta data information of
 * the image.
 * <P>
 * Although it is possible to mutate the properties of an {@code ImageResult}
 * instance, the intended use of {@code ImageResult} is to use it as an
 * effectively immutable object. That is, its properties should not be modified
 * (e.g.: The pixels of the {@code BufferedImage} should not be modified).
 * <P>
 * This class implements the {@link MemoryHeavyObject} interface and
 * approximates the size of an {@code ImageResult} instance by the approximate
 * size of the underlying {@code BufferedImage}.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads. Although
 * individual properties are not immutable, they should treated so. Users of
 * this class can assume that the properties of {@code ImageResult} are not
 * modified and can be safely accessed by multiple concurrent threads as well.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ImageResult implements MemoryHeavyObject {
    private final BufferedImage image;
    private final ImageMetaData metaData;
    private final long approxSize;

    /**
     * Initializes the {@code ImageResult} with the given image and its
     * meta data.
     *
     * @param image the retrieved image. This argument can be {@code null} if
     *   no image could be retrieved (maybe because it is not (yet) available.
     * @param metaData the meta data information about the specified image. If
     *   both the meta data and the image is specified (i.e.: not {@code null}),
     *   the meta data must define the same dimensions for the image as the
     *   image itself. This argument can be {@code null} if no meta data
     *   information is available.
     *
     * @throws IllegalArgumentException thrown if both the image and the meta
     *   data is specified and they define different dimensions for the image.
     */
    public ImageResult(BufferedImage image, ImageMetaData metaData) {
        if (image != null && metaData != null) {
            if (image.getWidth() != metaData.getWidth()
                    || image.getHeight() != metaData.getHeight()) {
                throw new IllegalArgumentException("The dimensions specified "
                        + "by the meta data and the image are inconsistent.");
            }
        }

        this.image = image;
        this.metaData = metaData;
        this.approxSize = ImageData.getApproxSize(image);
    }

    /**
     * Returns the retrieved image. The image might be incomplete or partial
     * if not completely available or in case of some failure.
     *
     * @return the retrieved image. This method may return {@code null}, if
     *   {@code null} was specified at construction time.
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns the meta data information of the image. In case both the meta
     * data and the image are specified, they define the same dimensions. That
     * is, both {@code getMetaData().getWidth() == getImage().getWidth()} and
     * {@code getMetaData().getHeight() == getImage().getHeight()} holds.
     *
     * @return the meta data information of the image. This method may return
     *   {@code null}, if {@code null} was specified at construction time.
     */
    public ImageMetaData getMetaData() {
        return metaData;
    }

    /**
     * Returns the width of the image or -1 if it is unknown.
     * <P>
     * If the meta data is available, the width is retrieved from the meta data,
     * if not and the image is available, it is retrieved from the image.
     *
     * @return the width of the image or -1 if it is unknown
     */
    public int getWidth() {
        return metaData != null
                ? metaData.getWidth()
                : (image != null ? image.getWidth() : -1);
    }

    /**
     * Returns the height of the image or -1 if it is unknown.
     * <P>
     * If the meta data is available, the height is retrieved from the meta
     * data, if not and the image is available, it is retrieved from the image.
     *
     * @return the height of the image or -1 if it is unknown
     */
    public int getHeight() {
        return metaData != null
                ? metaData.getHeight()
                : (image != null ? image.getHeight() : -1);
    }

    /**
     * Returns the approximate size of memory in bytes, the image of this
     * {@code ImageResult} retains. That is, this method returns the same value as
     * the {@code ImageData.getApproxSize(getImage())} invocation.
     *
     * @return the approximate size of memory in bytes, the image of this
     *   {@code ImageResult} retains. This method always returns a value greater
     *   than or equal to zero.
     */
    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }
}
