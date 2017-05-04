package org.jtrim2.image.transform;

/**
 * Defines types of interpolation to be used when scaling an image. In many
 * cases, implementations will use the requested interpolation type. However,
 * this is not required, when an implementation does not support the requested
 * interpolation type, it may choose another one (which is supported).
 * <P>
 * <B>Warning</B>: New interpolation types can be added at any time.
 *
 * @see AffineImageTransformer
 * @see org.jtrim2.swing.component.SimpleAsyncImageDisplay
 *
 * @author Kelemen Attila
 */
public enum InterpolationType {
    /**
     * Defines that the nearest-neighbor interpolation is to be used. This is
     * the most basic kind of interpolation which simply chooses the closest
     * pixel when resampling an image.
     * <P>
     * Usually this interpolation type is the most efficient in terms of speed.
     * All implementations should support nearest-neighbor interpolation.
     */
    NEAREST_NEIGHBOR,

    /**
     * Defines that the bilinear interpolation is to be used. This interpolation
     * calculates the pixel value from the 4 surrounding pixels. In most cases
     * this results in a far better quality image than image interpolated using
     * the {@link #NEAREST_NEIGHBOR nearest-neighbor interpolation}.
     * <P>
     * In most cases this interpolation type is a good trade-off between speed
     * and quality.
     */
    BILINEAR,

    /**
     * Defines that the bicubic interpolation is to be used. This interpolation
     * calculates the pixel value from the 16 surrounding pixels. This requires
     * considerably more computing power than
     * {@link #BILINEAR bilinear interpolation} but usually produces a more
     * smooth image.
     */
    BICUBIC
}
