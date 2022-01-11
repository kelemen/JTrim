package org.jtrim2.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.jtrim2.cache.MemoryHeavyObject;
import org.jtrim2.image.BufferedImages;

/**
 * Defines the output of an image transformation.
 *
 * <h2>Thread safety</h2>
 * Methods of this class can be safely accessed by multiple threads. Although
 * individual properties are not immutable, they should be treated so. Users of
 * this class can assume that the properties of {@code ImageTransformerData} are
 * not modified and can be safely accessed by multiple concurrent threads as
 * well.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @see ImageTransformationStep
 */
public final class TransformedImage implements MemoryHeavyObject {
    /**
     * An immutable {@code TransformedImage} having {@code null} as an
     * {@link #getImage() image} and identity transformation as a
     * {@link #getPointTransformer() coordinate transformation}.
     */
    public static final TransformedImage NULL_IMAGE = new TransformedImage(null, null);

    private final BufferedImage image;
    private final long approxSize;
    private final ImagePointTransformer pointTransformer;

    /**
     * Creates a new {@code TransformedImage} from the specified properties.
     *
     * @param image the result of the image transformation. This argument can
     *   be {@code null} is the result is a {@code null} image (possibly because
     *   the input image was {@code null}).
     * @param pointTransformer the coordinate transformation which
     *   transforms the location of the pixels on the input image to the
     *   location of the pixels on the output image. This argument can be
     *   {@code null}, in which case an identity transformation is assumed.
     */
    public TransformedImage(BufferedImage image, ImagePointTransformer pointTransformer) {
        this.image = image;
        this.approxSize = BufferedImages.getApproxSize(image);
        this.pointTransformer = pointTransformer != null
                ? pointTransformer
                : AffineImagePointTransformer.IDENTITY;
    }

    /**
     * Returns the result of the image transformation. That is, this method
     * returns the image specified at construction time.
     *
     * @return the result of the image transformation. This method may return
     *   {@code null} if {@code null} was specified at construction time.
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns the coordinate transformation which transforms the location of
     * the pixels on the input image to the location of the pixels on the output
     * image.
     *
     * @return the coordinate transformation which transforms the location of
     *   the pixels on the input image to the location of the pixels on the output
     *   image. This method never returns {@code null}, if {@code null} was
     *   specified at construction time the result is an identity coordinate
     *   transformation.
     */
    public ImagePointTransformer getPointTransformer() {
        return pointTransformer;
    }

    /**
     * A convenience method to call
     * {@code getPointTransformer().transformSrcToDest(src, dest)}.
     *
     * @param src the point to be transformed to the coordinate system of the
     *   result image. This argument cannot be {@code null} and this method is
     *   not allowed to modify this argument.
     * @param dest the point object where the result of the transformation is
     *   to be stored. This method modifies this argument. This argument cannot
     *   be {@code null} but can be the same object as {@code src}.
     */
    public void transformSrcToDest(Point2D src, Point2D dest) {
        pointTransformer.transformSrcToDest(src, dest);
    }

    /**
     * A convenience method to call
     * {@code getPointTransformer().transformDestToSrc(dest, src)}.
     *
     * @param dest the point to be transformed to the coordinate system of the
     *   input image. This argument cannot be {@code null} and this method is
     *   not allowed to modify this argument.
     * @param src the point object where the result of the transformation is
     *   to be stored. This method modifies this argument. This argument cannot
     *   be {@code null} but can be the same object as {@code dest}.
     *
     * @throws NoninvertibleTransformException thrown if the transformation from
     *   the coordinate system of the input image cannot be inverted
     */
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        pointTransformer.transformDestToSrc(dest, src);
    }

    /**
     * Returns the approximate memory retention of the
     * {@link #getImage() resulting image}. This method returns the same value as
     * {@link BufferedImages#getApproxSize(BufferedImage) BufferedImages.getApproxSize(getImage())}
     * would.
     *
     * @return he approximate memory retention of the
     *   {@link #getImage() resulting image}. This method always returns a value
     *   greater than or equal to zero.
     *
     * @see BufferedImages#getApproxSize(BufferedImage)
     */
    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }
}
