package org.jtrim2.image.transform;

import java.awt.image.BufferedImage;
import org.jtrim2.cache.MemoryHeavyObject;

/**
 * @deprecated This class is only used by deprecated classes. There is no
 *   replacement for this class.
 *
 * Defines the output of an {@link ImageTransformerLink}.
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
 * @see ImageTransformerLink
 * @see ImageTransformerQuery
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class TransformedImageData implements MemoryHeavyObject {
    private final TransformedImage transformedImage;
    private final org.jtrim2.image.ImageReceiveException exception;

    /**
     * Creates a new {@code TransformedImageData} with the given properties.
     *
     * @param transformedImage the result of the image transformation. This
     *   argument can be {@code null} if the transformation resulted in
     *   {@code null}.
     * @param exception the exception occurred when the image was retrieved.
     *   This argument can be {@code null} if there were no errors.
     */
    public TransformedImageData(
            TransformedImage transformedImage,
            org.jtrim2.image.ImageReceiveException exception) {
        this.transformedImage = transformedImage;
        this.exception = exception;
    }

    /**
     * Returns the exception occurred when the image was retrieved. This is the
     * same exception object as the one specified at construction time.
     *
     * @return the exception occurred when the image was retrieved. This method
     *   may return {@code null}.
     */
    public org.jtrim2.image.ImageReceiveException getException() {
        return exception;
    }

    /**
     * Returns the result of the image transformation. This is the same object
     * as the one one specified at construction time.
     *
     * @return the result of the image transformation. This method may return
     *   {@code null}.
     */
    public TransformedImage getTransformedImage() {
        return transformedImage;
    }

    /**
     * Returns the {@link TransformedImage#getPointTransformer() PointTransformer}
     * property of the {@link #getTransformedImage() result of the image transformation}
     * or {@code null} if the {@link #getTransformedImage() TransformedImage}
     * property is {@code null}.
     *
     * @return the {@link TransformedImage#getPointTransformer() PointTransformer}
     *   property of the {@link #getTransformedImage() result of the image transformation}
     *   or {@code null} if the {@link #getTransformedImage() TransformedImage}
     *   property is {@code null}
     */
    public ImagePointTransformer getPointTransformer() {
        return transformedImage != null ? transformedImage.getPointTransformer() : null;
    }

    /**
     * Returns the {@link TransformedImage#getImage() resulting image} of the
     * image transformation or {@code null} if the
     * {@link #getTransformedImage() TransformedImage} property is {@code null}.
     *
     * @return the {@link TransformedImage#getImage() resulting image} of the
     *   image transformation or {@code null} if the
     *   {@link #getTransformedImage() TransformedImage} property is {@code null}
     */
    public BufferedImage getImage() {
        return transformedImage != null ? transformedImage.getImage() : null;
    }

    /**
     * Returns the approximate memory retention in bytes of the resulting image
     * or 0 if the {@link #getTransformedImage() TransformedImage} property is
     * {@code null}.
     *
     * @return the approximate memory retention in bytes of the resulting image
     *   or 0 if the {@link #getTransformedImage() TransformedImage} property is
     *   {@code null}
     */
    @Override
    public long getApproxMemorySize() {
        if (transformedImage != null) {
            return transformedImage.getApproxMemorySize();
        }

        return 0;
    }
}
