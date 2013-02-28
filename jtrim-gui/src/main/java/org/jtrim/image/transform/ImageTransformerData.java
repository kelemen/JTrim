package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import org.jtrim.image.ImageMetaData;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines the input of an {@link ImageTransformer image transformer}.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads. Although
 * individual properties are not immutable, they should be treated so. Users of
 * this class can assume that the properties of {@code ImageTransformerData} are
 * not modified and can be safely accessed by multiple concurrent threads as
 * well.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ImageTransformerData {
    private final BufferedImage sourceImage;
    private final int destWidth;
    private final int destHeight;
    private final ImageMetaData metaData;

    /**
     * Creates a new {@code ImageTransformerData} with the specified properties.
     * <P>
     * Although both the source image and its meta-data is allowed to be
     * {@code null}, it is recommended to provide at least one of them.
     *
     * @param sourceImage the image to be transformed. This argument can be
     *   {@code null} if the image is not available.
     * @param destWidth the required width for the result of the image
     *   transformation. That is, image transformers must create an image with
     *   {@code destWidth} width. This argument must be greater than or equal to
     *   zero.
     * @param destHeight the required height for the result of the image
     *   transformation. That is, image transformers must create an image with
     *   {@code destHeight} height. This argument must be greater than or equal
     *   to zero.
     * @param metaData the meta-data information of the original source image.
     *   This can be especially important to the image transformer when the
     *   source image is not available. This argument can be {@code null} if the
     *   meta-data of the image is not available. <B>Note</B> that the width
     *   and height specified in the meta-data might be different than the
     *   source image because the meta-data refers to the original source image.
     */
    public ImageTransformerData(
            BufferedImage sourceImage,
            int destWidth,
            int destHeight,
            ImageMetaData metaData) {

        ExceptionHelper.checkArgumentInRange(destWidth, 0, Integer.MAX_VALUE, "destWidth");
        ExceptionHelper.checkArgumentInRange(destHeight, 0, Integer.MAX_VALUE, "destHeight");

        this.sourceImage = sourceImage;
        this.destWidth = destWidth;
        this.destHeight = destHeight;
        this.metaData = metaData;
    }

    /**
     * Returns the width of the source image or -1 if it is not known.
     * <P>
     * If the {@link #getSourceImage() source image} is not {@code null}, this
     * method simply returns {@code getSourceImage().getWidth()}. If the
     * source image is {@code null} but the {@link #getMetaData() meta-data} is
     * not, then this method returns {@code getMetaData().getWidth()}. If none
     * of them is available -1 is returned.
     *
     * @return the width of the source image or -1 if it is not known
     */
    public int getImageWidth() {
        if (sourceImage != null) {
            return sourceImage.getWidth();
        }
        if (metaData != null) {
            return metaData.getWidth();
        }

        return -1;
    }

    /**
     * Returns the height of the source image or -1 if it is not known.
     * <P>
     * If the {@link #getSourceImage() source image} is not {@code null}, this
     * method simply returns {@code getSourceImage().getHeight()}. If the
     * source image is {@code null} but the {@link #getMetaData() meta-data} is
     * not, then this method returns {@code getMetaData().getHeight()}. If none
     * of them is available -1 is returned.
     *
     * @return the height of the source image or -1 if it is not known
     */
    public int getImageHeight() {
        if (sourceImage != null) {
            return sourceImage.getHeight();
        }
        if (metaData != null) {
            return metaData.getHeight();
        }

        return -1;
    }

    /**
     * Returns the width of the source image ignoring the meta-data, or 0 if the
     * source image is not available.
     *
     * @return the width of the source image ignoring the meta-data, or 0 if the
     *   source image is not available
     */
    public int getSrcWidth() {
        return sourceImage != null ? sourceImage.getWidth() : 0;
    }

    /**
     * Returns the height of the source image ignoring the meta-data, or 0 if
     * the source image is not available.
     *
     * @return the height of the source image ignoring the meta-data, or 0 if
     *   the source image is not available
     */
    public int getSrcHeight() {
        return sourceImage != null ? sourceImage.getHeight() : 0;
    }

    /**
     * Returns the height of the required result of the image transformation.
     * This method returns the same value as specified at construction time.
     *
     * @return the height of the required result of the image transformation
     */
    public int getDestHeight() {
        return destHeight;
    }

    /**
     * Returns the width of the required result of the image transformation.
     * This method returns the same value as specified at construction time.
     *
     * @return the width of the required result of the image transformation
     */
    public int getDestWidth() {
        return destWidth;
    }

    /**
     * Returns the meta-data information of the source image or {@code null} if
     * it is not available. This method returns the same object as specified at
     * construction time.
     * <P>
     * <B>Note</B>: The width and height of the meta-data might not be the same
     * as the image to be transformed because, the meta-data stores the width
     * and height of the original source image (and the current source image
     * might already be a transformed image).
     *
     * @return the meta-data information of the source image or {@code null} if
     *   it is not available
     */
    public ImageMetaData getMetaData() {
        return metaData;
    }

    /**
     * Returns the source image to be transformed or {@code null} if
     * it is not available. This method returns the same object as specified at
     * construction time.
     * <P>
     * Note: If the source image is not available, the image transformer might
     * be able to render something relying on the meta-data (if it is
     * available).
     *
     * @return the source image to be transformed or {@code null} if
     *   it is not available
     */
    public BufferedImage getSourceImage() {
        return sourceImage;
    }
}
