package org.jtrim2.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import org.jtrim2.cache.MemoryHeavyObject;

/**
 * @deprecated Use {@link ImageResult} instead.
 *
 * Defines an image which was retrieved from an external source. Apart from the
 * image itself, the {@code ImageData} contains the meta data information of
 * the image and an exception associated with the image retrieval.
 * <P>
 * Although it is possible to mutate the properties of an {@code ImageData}
 * instance, the intended use of {@code ImageData} is to use it as an
 * effectively immutable object. That is, its properties should not be modified
 * (e.g.: The pixels of the {@code BufferedImage} should not be modified).
 * <P>
 * This class implements the {@link MemoryHeavyObject} interface and
 * approximates the size of an {@code ImageData} instance by the approximate
 * size of the underlying {@code BufferedImage}.
 * <P>
 * Note that this class also provides several useful static helper methods,
 * to create {@code BufferedImage} instance better handled by the Java and
 * methods to calculate the approximate size of a {@code BufferedImage}.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads. Although
 * individual properties are not immutable, they should treated so. Users of
 * this class can assume that the properties of {@code ImageData} are not
 * modified and can be safely accessed by multiple concurrent threads as well.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class ImageData implements MemoryHeavyObject {
    private final BufferedImage image;
    private final long approxSize;
    private final ImageMetaData metaData;
    private final ImageReceiveException exception;

    /**
     * Initializes the {@code ImageData} with the given image, meta data and
     * exception describing the failure in retrieving the image.
     *
     * @param image the retrieved image. This argument can be {@code null} if
     *   no image could be retrieved (maybe because it is not (yet) available.
     * @param metaData the meta data information about the specified image. If
     *   both the meta data and the image is specified (i.e.: not {@code null}),
     *   the meta data must define the same dimensions for the image as the
     *   image itself. This argument can be {@code null} if no meta data
     *   information is available.
     * @param exception the error describing the failure of the image retrieval.
     *   Even in case of failure a partial or incomplete image might be
     *   available. This argument can be {@code null} if there was no failure in
     *   retrieving the image.
     *
     * @throws IllegalArgumentException thrown if both the image and the meta
     *   data is specified and they define different dimensions for the image.
     */
    public ImageData(BufferedImage image, ImageMetaData metaData,
            ImageReceiveException exception) {
        if (image != null && metaData != null) {
            if (image.getWidth() != metaData.getWidth()
                    || image.getHeight() != metaData.getHeight()) {
                throw new IllegalArgumentException("The dimensions specified "
                        + "by the meta data and the image are inconsistent.");
            }
        }

        this.image = image;
        this.approxSize = getApproxSize(image);
        this.metaData = metaData;
        this.exception = exception;
    }

    /**
     * Returns the exception describing the failure of the image retrieval. This
     * method returns the same exception as specified at construction time.
     * <P>
     * Note that even in case of failure, a partial or incomplete image might
     * still be available.
     *
     * @return the exception describing the failure of the image retrieval. This
     *   method may return {@code null}, if {@code null} was specified at
     *   construction time.
     */
    public ImageReceiveException getException() {
        return exception;
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
     * {@code ImageData} retains. That is, this method returns the same value as
     * the {@code ImageData.getApproxSize(getImage())} invocation.
     *
     * @return the approximate size of memory in bytes, the image of this
     *   {@code ImageData} retains. This method always returns a value greater
     *   than or equal to zero.
     */
    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }

    /**
     * Returns the average size in bytes of a single pixel in the given
     * {@code ColorModel}. The returned value might not be an integer because
     * some {@code ColorModel} encodes pixels in a fraction of a byte (or just
     * not in a multiple of eight bits).
     * <P>
     * Note that for some obscure {@code ColorModel} this method may return an
     * incorrect value but tries to return a good approximate in this cases as
     * well.
     *
     * @param cm the {@code ColorModel} of which pixel size in bytes is to be
     *   returned. This argument cannot be {@code null}.
     * @return the average size in bytes of a single pixel in the given
     *   {@code ColorModel}. This method always return a value greater than or
     *   equal to zero. A return value is zero is usually only theoretical since
     *   there is no such practical color model.
     *
     * @throws NullPointerException thrown if the specified color model is
     *   {@code null}
     */
    public static double getStoredPixelSize(ColorModel cm) {
        return BufferedImages.getStoredPixelSize(cm);
    }

    /**
     * Returns the approximate memory in bytes the specified
     * {@code BufferedImage} retains. The size is approximate by the size of all
     * the pixels in the {@code BufferedImage}.
     *
     * @param image the {@code BufferedImage} whose size is to be approximated.
     *   This argument can be {@code null}, in which case zero is returned.
     * @return the approximate memory in bytes the specified
     *   {@code BufferedImage} retains. This method always returns a value
     *   greater than or equal to zero.
     */
    public static long getApproxSize(BufferedImage image) {
        return BufferedImages.getApproxSize(image);
    }

    /**
     * Returns the type of the {@code BufferedImage} which most closely
     * approximates the specified {@code ColorModel}.
     * <P>
     * The returned integer can be used as an image type for a
     * {@code BufferedImage}. That is, this integer can be specified for the
     * {@code BufferedImage} at construction time and will be returned by the
     * {@code BufferedImage.getType()} method. Note however, that this method
     * may return {@code BufferedImage.TYPE_CUSTOM} for which the constructor
     * of {@code BufferedImage} will throw an exception but any other return
     * value will be accepted by {@code BufferedImage}.
     *
     * @param colorModel the {@code ColorModel} which is to be approximated with
     *   a {@code BufferedImage} image type. This argument cannot be
     *   {@code null}.
     * @return the {@code BufferedImage} image type which most closely
     *   approximates the specified {@code ColorModel}.
     */
    public static int getCompatibleBufferType(ColorModel colorModel) {
        return BufferedImages.getCompatibleBufferType(colorModel);
    }

    /**
     * Creates a returns a new {@code BufferedImage} which is compatible with
     * the specified {@code BufferedImage} with the given dimensions. The
     * returned {@code BufferedImage} will have the same {@code ColorModel} as
     * the one specified. The returned {@code BufferedImage} is compatible in
     * the sense that its pixels can be copied using simple array copies and
     * does not require any other transformations.
     *
     * @param image the image to which a compatible {@code BufferedImage} is to
     *   be returned. This argument can be {@code null}, in which case
     *   {@code null} is returned.
     * @param width the width of the returned {@code BufferedImage}. This
     *   argument must be greater than zero.
     * @param height the height of the returned {@code BufferedImage}. This
     *   argument must be greater than zero.
     * @return a new {@code BufferedImage} which is compatible with the
     *   specified {@code BufferedImage} with the given dimensions. This method
     *   returns {@code null} only if the specified image was also {@code null}.
     */
    public static BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height) {
        return BufferedImages.createCompatibleBuffer(image, width, height);
    }

    /**
     * Returns a new {@code BufferedImage} which is the copy of the specified
     * image. That is, returns an image which when modified does not affect the
     * specified image. The returned and the specified {@code BufferedImage}
     * have the same color model, dimensions and are otherwise compatible.
     *
     * @param image the image which is to be copied. This argument can be
     *   {@code null}, in which case {@code null} is returned.
     * @return a new {@code BufferedImage} which is the copy of the specified
     *   image. This method returns {@code null} only if the specified image was
     *   also {@code null}.
     */
    public static BufferedImage cloneImage(BufferedImage image) {
        return BufferedImages.cloneImage(image);
    }

    /**
     * Returns a new image which have the same pixels as the specified image but
     * is likely to be accelerated by the currently present graphics card (if
     * there is any).
     * <P>
     * This methods differs from {@link #createAcceleratedBuffer(BufferedImage) createAcceleratedBuffer}
     * by always returning a new image which is independent from the argument.
     * That is, modifications of the returned image have no effect on the
     * image passed as an argument.
     *
     * @param image the base image to which an image is to be returned which
     *   is more likely to be accelerated by the currently present graphics
     *   card. If this method cannot do better, it will return a copy of this
     *   image. This argument can be {@code null}, in which case {@code null}
     *   is returned.
     * @return the image having the same pixels as the specified image but
     *   is likely to be accelerated by the currently present graphics card.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}. Modifying the returned image has no effect on the image
     *   passed in the argument.
     *
     * @see #createAcceleratedBuffer(BufferedImage)
     * @see #createNewOptimizedBuffer(BufferedImage)
     */
    public static BufferedImage createNewAcceleratedBuffer(BufferedImage image) {
        return BufferedImages.createNewAcceleratedBuffer(image);
    }

    /**
     * Returns an image which have the same pixels as the specified image but
     * is likely to be accelerated by the currently present graphics card (if
     * there is any).
     * <P>
     * This method uses several undefined heuristics to determine the required
     * image type and may also return an image which requires somewhat more
     * memory to be stored.
     * <P>
     * This method may return the same {@code BufferedImage} image as the one
     * specified in the argument. If this is undesired, use the
     * {@link #createNewAcceleratedBuffer(BufferedImage) createNewAcceleratedBuffer}
     * method instead.
     *
     * @param image the base image to which an image is to be returned which
     *   is more likely to be accelerated by the currently present graphics
     *   card. This method may return this image if it cannot do better. This
     *   argument can be {@code null}, in which case {@code null} is returned.
     * @return the image having the same pixels as the specified image but
     *   is likely to be accelerated by the currently present graphics card.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createNewAcceleratedBuffer(BufferedImage)
     */
    public static BufferedImage createAcceleratedBuffer(BufferedImage image) {
        return BufferedImages.createAcceleratedBuffer(image);
    }

    /**
     * Returns a new image which have the same pixels as the specified image but
     * is likely to be more efficiently handled than the one specified.
     * <P>
     * This methods differs from {@link #createOptimizedBuffer(BufferedImage) createOptimizedBuffer}
     * by always returning a new image which is independent from the argument.
     * That is, modifications of the returned image have no effect on the
     * image passed as an argument.
     *
     * @param image the base image to which an image is to be returned which
     *   is likely to be more efficiently handled in general. If this method
     *   cannot do better, it will return a copy of this image. This argument
     *   can be {@code null}, in which case {@code null} is returned.
     * @return the image which have the same pixels as the specified image but
     *   is likely to be more efficiently handled than the one specified.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createNewAcceleratedBuffer(BufferedImage)
     * @see #createOptimizedBuffer(BufferedImage)
     */
    public static BufferedImage createNewOptimizedBuffer(BufferedImage image) {
        return BufferedImages.createNewOptimizedBuffer(image);
    }

    /**
     * Returns an image which have the same pixels as the specified image but
     * is likely to be more efficiently handled than the one specified.
     * <P>
     * Unlike {@link #createAcceleratedBuffer(BufferedImage) createAcceleratedBuffer},
     * this method does not consider graphic card acceleration. That is, this is
     * a general purpose method and only employs heuristics for converting the
     * image which are very likely to increase performance in general or at
     * least does not downgrade performance. This method may also return an
     * image which requires somewhat more memory to be stored.
     * <P>
     * This method may return the same {@code BufferedImage} image as the one
     * specified in the argument. If this is undesired, use the
     * {@link #createNewOptimizedBuffer(BufferedImage) createNewOptimizedBuffer}
     * method instead.
     *
     * @param image the base image to which an image is to be returned which
     *   is likely to be more efficiently handled in general. This method may
     *   return this image if it cannot do better. This argument can be
     *   {@code null}, in which case {@code null} is returned.
     * @return the image which have the same pixels as the specified image but
     *   is likely to be more efficiently handled than the one specified.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createAcceleratedBuffer(BufferedImage)
     * @see #createNewOptimizedBuffer(BufferedImage)
     */
    public static BufferedImage createOptimizedBuffer(BufferedImage image) {
        return BufferedImages.createOptimizedBuffer(image);
    }
}