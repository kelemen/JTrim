package org.jtrim.image.transform;

/**
 * Defines a subset of commonly used 2D affine transformations. The
 * transformation is defined by the following properties: rotate, flip, scale
 * and offset. The offset property is intended to be applied after other
 * transformations have been applied.
 * <P>
 * Note that this class is only a subset of the possible affine transformations
 * and cannot define an arbitrary affine transformation.
 * <P>
 * This class does not have a public constructor. You can create instances of
 * {@code BasicImageTransformations} through
 * {@link Builder BasicImageTransformations.Builder} or by one of the factory
 * methods. For example:
 * <pre>
 * BasicImageTransformations.Builder builder = new BasicImageTransformations.Builder();
 * builder.setRotateInDegrees(90);
 * builder.setZoom(2.0);
 * return builder.create();
 * </pre>
 * <P>
 * The above code returns a transformations which rotates by 90 degrees
 * clockwise and magnifies the image to twice the original size in both
 * dimensions.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are completely immutable and therefore safe to be
 * accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see Builder
 * @see #identityTransformation()
 * @see #newOffsetTransformation(double, double)
 * @see #newRotateTransformation(double)
 * @see #newZoomTransformation(double, double)
 * @see org.jtrim.swing.component.SimpleAsyncImageDisplay
 *
 * @author Kelemen Attila
 */
public final class BasicImageTransformations {
    private static final int EXPECTED_MAX_TO_STRING_LENGTH = 256;

    /**
     * Defines 0 degree in radians. When 0 degree is set for a
     * {@code BasicImageTransformations}, then the
     * {@link #getRotateInRadians() getRotateInRadians()} method will return
     * exactly this double value.
     */
    public static final double RAD_0 = 0.0;

    /**
     * Defines 90 degrees in radians. When 90 degrees is set for a
     * {@code BasicImageTransformations}, then the
     * {@link #getRotateInRadians() getRotateInRadians()} method will return
     * exactly this double value.
     */
    public static final double RAD_90 = Math.PI / 2.0;

    /**
     * Defines 180 degrees in radians. When 180 degrees is set for a
     * {@code BasicImageTransformations}, then the
     * {@link #getRotateInRadians() getRotateInRadians()} method will return
     * exactly this double value.
     */
    public static final double RAD_180 = Math.PI;

    /**
     * Defines 270 degrees in radians. When 270 degrees is set for a
     * {@code BasicImageTransformations}, then the
     * {@link #getRotateInRadians() getRotateInRadians()} method will return
     * exactly this double value.
     */
    public static final double RAD_270 = RAD_180 + RAD_90;

    private static final int DEGREES_0 = 0;
    private static final int DEGREES_90 = 90;
    private static final int DEGREES_180 = 180;
    private static final int DEGREES_270 = 270;
    private static final int DEGREES_360 = 360;

    private static final double PI2 = 2.0 * Math.PI;
    private static final BasicImageTransformations IDENTITY
            = new BasicImageTransformations.Builder().create();

    /**
     * Returns the identity transformation. That is, a transformation with the
     * following properties:
     * <ul>
     *  <li>
     *   {@link #getRotateInRadians() RotateInRadians}
     *   (and {@link #getRotateInDegrees() RotateInDegrees}) == 0
     *  </li>
     *  <li>{@link #isFlipHorizontal() FlipHorizontal} == false</li>
     *  <li>{@link #isFlipVertical() FlipVertical} == false</li>
     *  <li>{@link #getZoomX() ZoomX} == 1.0</li>
     *  <li>{@link #getZoomY() ZoomY} == 1.0</li>
     *  <li>{@link #getOffsetX() OffsetX} == 0.0</li>
     *  <li>{@link #getOffsetY() OffsetY} == 0.0</li>
     * </ul>
     *
     * @return the identity transformation. This method never returns
     *   {@code null}.
     */
    public static BasicImageTransformations identityTransformation() {
        return IDENTITY;
    }

    /**
     * Returns a transformation which is the same as the
     * {@link #identityTransformation() identity transformation} except for
     * the scaling properties. That is, a transformation with the following
     * properties:
     * <ul>
     *  <li>
     *   {@link #getRotateInRadians() RotateInRadians}
     *   (and {@link #getRotateInDegrees() RotateInDegrees}) == 0
     *  </li>
     *  <li>{@link #isFlipHorizontal() FlipHorizontal} == false</li>
     *  <li>{@link #isFlipVertical() FlipVertical} == false</li>
     *  <li>{@link #getZoomX() ZoomX} == {@code zoomX} (as requested)</li>
     *  <li>{@link #getZoomY() ZoomY} == {@code zoomY} (as requested)</li>
     *  <li>{@link #getOffsetX() OffsetX} == 0.0</li>
     *  <li>{@link #getOffsetY() OffsetY} == 0.0</li>
     * </ul>
     *
     * @param zoomX the multiplier applied to the width of the image
     * @param zoomY the multiplier applied to the height of the image
     * @return a transformation with the specified scaling properties. This
     *   method never returns {@code null}.
     */
    public static BasicImageTransformations newZoomTransformation(
            double zoomX, double zoomY) {

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder();

        result.setZoomX(zoomX);
        result.setZoomY(zoomY);
        return result.create();
    }

    /**
     * Returns a transformation which is the same as the
     * {@link #identityTransformation() identity transformation} except for
     * the offset properties. That is, a transformation with the following
     * properties:
     * <ul>
     *  <li>
     *   {@link #getRotateInRadians() RotateInRadians}
     *   (and {@link #getRotateInDegrees() RotateInDegrees}) == 0
     *  </li>
     *  <li>{@link #isFlipHorizontal() FlipHorizontal} == false</li>
     *  <li>{@link #isFlipVertical() FlipVertical} == false</li>
     *  <li>{@link #getZoomX() ZoomX} == 1.0</li>
     *  <li>{@link #getZoomY() ZoomY} == 1.0</li>
     *  <li>{@link #getOffsetX() OffsetX} == {@code offsetX} (as requested)</li>
     *  <li>{@link #getOffsetY() OffsetY} == {@code offsetY} (as requested)</li>
     * </ul>
     *
     * @param offsetX the value defining translating along the horizontal
     *   axis
     * @param offsetY the value defining translating along the vertical
     *   axis
     * @return a transformation with the specified offset properties. This
     *   method never returns {@code null}.
     */
    public static BasicImageTransformations newOffsetTransformation(
            double offsetX, double offsetY) {

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder();

        result.setOffset(offsetX, offsetY);
        return result.create();
    }

    /**
     * Returns a transformation which is the same as the
     * {@link #identityTransformation() identity transformation} except for
     * the rotate property. That is, a transformation with the following
     * properties:
     * <ul>
     *  <li>
     *   {@link #getRotateInRadians() RotateInRadians} == {@code rotateInRads}
     *   (as requested).
     *  </li>
     *  <li>{@link #isFlipHorizontal() FlipHorizontal} == false</li>
     *  <li>{@link #isFlipVertical() FlipVertical} == false</li>
     *  <li>{@link #getZoomX() ZoomX} == 1.0</li>
     *  <li>{@link #getZoomY() ZoomY} == 1.0</li>
     *  <li>{@link #getOffsetX() OffsetX} == 0.0</li>
     *  <li>{@link #getOffsetY() OffsetY} == 0.0</li>
     * </ul>
     *
     * @param rotateInRads the required rotation in radians
     * @return a transformation with the specified offset properties. This
     *   method never returns {@code null}.
     */
    public static BasicImageTransformations newRotateTransformation(
            double rotateInRads) {

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder();

        result.setRotateInRadians(rotateInRads);
        return result.create();
    }


    /**
     * We will keep every double in a canonical form, so hashCode()
     * will not surprise us.
     * The possible problems are: NaNs and 0.0 (it has a sign).
     * @param value the value to canonize
     * @return the canonical value of the argument
     */
    private static double canonicalizeDouble(double value) {
        if (value == 0.0) {
            return 0.0;
        }

        if (Double.isNaN(value)) {
            return Double.NaN;
        }

        return value;
    }

    private static double normalizeRadians(double rad) {
        if (Double.isInfinite(rad)) {
            return rad;
        }

        double norm = Math.IEEEremainder(rad, PI2);
        if (norm < 0.0) {
            norm += PI2;
        }
        return norm;
    }

    /**
     * This class is to be used to create {@link BasicImageTransformations}
     * instances.
     *
     * <h3>Thread safety</h3>
     * Instances of this class cannot be accessed from multiple threads
     * concurrently. Concurrent access to instances of this class must by
     * externally synchronized.
     *
     * <h4>Synchronization transparency</h4>
     * Methods of this class are <I>synchronization transparent</I>.
     */
    public static final class Builder {
        // These variables are kept synchronized so setting and getting rotate will
        // not cause rounding errors but using higher precision is still available
        // through using radians.
        private int rotateDeg;
        private double rotateRad;

        private boolean flipHorizontal;
        private boolean flipVertical;
        private double zoomX; // if < 1, image is shrinked. if > 1, magnify
        private double zoomY;
        private double offsetX; // scale is according to the original size
        private double offsetY;

        /**
         * Creates a new {@code BasicImageTransformations.Builder} initialized
         * to the identity transformation. That is, the initial properties are:
         * <ul>
         *  <li>
         *   {@link #getRotateInRadians() RotateInRadians}
         *   (and {@link #getRotateInDegrees() RotateInDegrees}) == 0
         *  </li>
         *  <li>{@link #isFlipHorizontal() FlipHorizontal} == false</li>
         *  <li>{@link #isFlipVertical() FlipVertical} == false</li>
         *  <li>{@link #getZoomX() ZoomX} == 1.0</li>
         *  <li>{@link #getZoomY() ZoomY} == 1.0</li>
         *  <li>{@link #getOffsetX() OffsetX} == 0.0</li>
         *  <li>{@link #getOffsetY() OffsetY} == 0.0</li>
         * </ul>
         */
        public Builder() {
            this.rotateDeg = 0;
            this.rotateRad = 0.0;
            this.flipHorizontal = false;
            this.flipVertical = false;
            this.zoomX = 1.0;
            this.zoomY = 1.0;
            this.offsetX = 0.0;
            this.offsetY = 0.0;
        }

        /**
         * Creates a new {@code BasicImageTransformations.Builder} initialized
         * to the same properties as the specified transformation.
         *
         * @param base the {@code BasicImageTransformations} the transformation
         *   containing the initial properties of the newly created instance.
         *   This argument cannot be {@code null}.
         *
         * @throws NullPointerException thrown if the specified argument is
         *   {@code null}
         */
        public Builder(BasicImageTransformations base) {
            this.rotateDeg = base.rotateDeg;
            this.rotateRad = base.rotateRad;
            this.flipHorizontal = base.flipHorizontal;
            this.flipVertical = base.flipVertical;
            this.zoomX = base.zoomX;
            this.zoomY = base.zoomY;
            this.offsetX = base.offsetX;
            this.offsetY = base.offsetY;
        }

        /**
         * Creates and returns a snapshot of the currently set properties.
         *
         * @return the snapshot of the currently set properties. This method
         *   never returns {@code null} and subsequent changes to this
         *   {@code BasicImageTransformations.Builder} has no effect on the
         *   returned {@code BasicImageTransformations}.
         */
        public BasicImageTransformations create() {
            return new BasicImageTransformations(this);
        }

        /**
         * Returns {@code true} if the image needs to be flipped horizontally.
         * That is, the left side of the image need to be the right side of the
         * image.
         *
         * @return {@code true} if the image needs to be flipped horizontally,
         *   {@code false} otherwise
         */
        public boolean isFlipHorizontal() {
            return flipHorizontal;
        }

        /**
         * Sets if the image should be flipped horizontally. That is, if the left
         * side of the image should become the right side of the image.
         *
         * @param flipHorizontal if {@code true} the image should be flipped
         *   horizontally, set it to {@code false} otherwise
         */
        public void setFlipHorizontal(boolean flipHorizontal) {
            this.flipHorizontal = flipHorizontal;
        }

        /**
         * Inverts the {@link #isFlipHorizontal() "FlipHorizontal" property}.
         * Calling this method is effectively equivalent to calling:
         * {@code setFlipHorizontal(!isFlipHorizontal())}.
         */
        public void flipHorizontal() {
            flipHorizontal = !flipHorizontal;
        }

        /**
         * Returns {@code true} if the image needs to be flipped vertically.
         * That is, the top side of the image need to be the bottom side of the
         * image.
         *
         * @return {@code true} if the image needs to be flipped vertically,
         *   {@code false} otherwise
         */
        public boolean isFlipVertical() {
            return flipVertical;
        }

        /**
         * Sets if the image should be flipped vertically. That is, if the top
         * side of the image should become the bottom side of the image.
         *
         * @param flipVertical if {@code true} the image should be flipped
         *   vertically, set it to {@code false} otherwise
         */
        public void setFlipVertical(boolean flipVertical) {
            this.flipVertical = flipVertical;
        }

        /**
         * Inverts the {@link #isFlipVertical() "FlipVertical" property}.
         * Calling this method is effectively equivalent to calling:
         * {@code setFlipVertical(!isFlipVertical())}.
         */
        public void flipVertical() {
            flipVertical = !flipVertical;
        }

        /**
         * Returns the offset along the horizontal axis. Note that the offset is
         * to be applied after other transformations have been applied.
         *
         * @return the offset along the horizontal axis
         */
        public double getOffsetX() {
            return offsetX;
        }

        /**
         * Returns the offset along the vertical axis. Note that the offset is
         * to be applied after other transformations have been applied.
         *
         * @return the offset along the vertical axis
         */
        public double getOffsetY() {
            return offsetY;
        }

        /**
         * Sets both the horizontal an vertical offset need to be applied on the
         * image.
         * <P>
         * Note that these offsets are to be applied after other transformations
         * have been applied.
         *
         * @param offsetX the offset along the horizontal axis. Subsequent
         *   {@link #getOffsetX() getOffsetX()} method calls will return this
         *   value.
         * @param offsetY the offset along the vertical axis. Subsequent
         *   {@link #getOffsetY() getOffsetY()} method calls will return this
         *   value.
         */
        public void setOffset(double offsetX, double offsetY) {
            this.offsetX = canonicalizeDouble(offsetX);
            this.offsetY = canonicalizeDouble(offsetY);
        }

        /**
         * Returns the angle meaning how much the image need to be rotated
         * around its center in radians. As the angle increases, the image need
         * to be rotated clockwise. The zero angle means that the image is not
         * rotated at all.
         *
         * @return the angle meaning how much the image need to be rotated
         *   around its center in radians. This method returns always returns a
         *   normalized angle. That is, a value between 0 and {@code 2*pi} or
         *   {@code NaN} if {@code NaN} was set.
         */
        public double getRotateInRadians() {
            return rotateRad;
        }

        /**
         * Sets the angle meaning how much the image need to be rotated around
         * its center in radians. As the angle increases, the image need to be
         * rotated clockwise. The zero angle means that the image is not rotated
         * at all.
         * <P>
         * Note: Settings this property also changes the value returned by the
         * {@link #getRotateInDegrees() getRotateInDegrees()} method.
         *
         * @param radians the angle meaning how much the image need to be
         *   rotated around its center in radians. Note that this property is
         *   stored in a normalized form. So this property will be set to a
         *   value between 0 and {@code 2*pi} or {@code NaN} or infinity.
         */
        public void setRotateInRadians(double radians) {
            rotateRad = canonicalizeDouble(normalizeRadians(radians));
            rotateDeg = (int)Math.round(Math.toDegrees(rotateRad));

            // just in case of rounding errors
            rotateDeg = rotateDeg % DEGREES_360;
            if (rotateDeg < 0) {
                rotateDeg += DEGREES_360;
            }
        }

        /**
         * Returns the angle meaning how much the image need to be rotated
         * around its center in degrees. As the angle increases, the image need
         * to be rotated clockwise. The zero angle means that the image is not
         * rotated at all.
         * <P>
         * Notice that this property is an {@code int}, if you need better
         * precision, use the {@link #getRotateInRadians() getRotateInRadians()}
         * method.
         * <P>
         * If the rotation amount was set by the
         * {@link #setRotateInRadians(double) setRotateInRadians} method, this
         * method will round the value to the closest integer (after converting
         * radians to degrees).
         *
         * @return the angle meaning how much the image need to be rotated
         *   around its center in radians. This method returns always returns a
         *   normalized angle. That is, a value which is greater than or equal
         *   to zero and less than (not equal) to 360 or {@code NaN} or
         *   infinity.
         */
        public int getRotateInDegrees() {
            return rotateDeg;
        }

        /**
         * Sets the angle meaning how much the image need to be rotated around
         * its center in degrees. As the angle increases, the image need to be
         * rotated clockwise. The zero angle means that the image is not rotated
         * at all.
         * <P>
         * Notice that this property is an {@code int}, if you need better
         * precision, use the {@link #setRotateInRadians(double) setRotateInRadians}
         * method.
         * <P>
         * Note: Settings this property also changes the value returned by the
         * {@link #getRotateInRadians() getRotateInRadians()} method.
         *
         * @param degrees the angle meaning how much the image need to be
         *   rotated around its center in degrees. Note that this property is
         *   stored in a normalized form. So this property will be set to a
         *   value which is greater than or equal to zero and less than
         *   (not equal) to 360.
         */
        public void setRotateInDegrees(int degrees) {
            // This works on negative numbers as well.
            rotateDeg = degrees % DEGREES_360;
            if (rotateDeg < 0) {
                rotateDeg += DEGREES_360;
            }

            // Be as precise as possible in these
            // important special cases.
            switch (rotateDeg) {
                case DEGREES_0:
                    rotateRad = RAD_0;
                    break;
                case DEGREES_90:
                    rotateRad = RAD_90;
                    break;
                case DEGREES_180:
                    rotateRad = RAD_180;
                    break;
                case DEGREES_270:
                    rotateRad = RAD_270;
                    break;
                default:
                    rotateRad = canonicalizeDouble(Math.toRadians(rotateDeg));
                    break;
            }
        }

        /**
         * Returns the scaling factor used to scale the image horizontally. For
         * example: If this method returns {@code 2.0}, the image must be scaled
         * to an image which has twice the width of the original image.
         * Respectively, a value of {@code 0.5} means that the resulting image
         * must be scaled to an image which has half the width of the original
         * image.
         * <P>
         * This method may return a negative value which should be interpreted
         * as {@link #flipHorizontal() flipping the image horizontally} and
         * scaling with the absolute value.
         * <P>
         * The client code should also expect that this property might be zero,
         * {@code NaN} or even infinity. How these special values are handled
         * are left to the client code.
         *
         * @return the scaling factor used to scale the image horizontally
         */
        public double getZoomX() {
            return zoomX;
        }

        /**
         * Sets the scaling factor used to scale the image horizontally. The
         * scaling must be interpreted in the coordinate system of the original
         * image. That is, {@code zoomX} adjusts the width of the original
         * image.
         *
         * @param zoomX the scaling factor used to scale the image horizontally
         */
        public void setZoomX(double zoomX) {
            this.zoomX = canonicalizeDouble(zoomX);
        }

        /**
         * Returns the scaling factor used to scale the image vertically. For
         * example: If this method returns {@code 2.0}, the image must be scaled
         * to an image which has twice the height of the original image.
         * Respectively, a value of {@code 0.5} means that the resulting image
         * must be scaled to an image which has half the height of the original
         * image.
         * <P>
         * This method may return a negative value which should be interpreted
         * as {@link #flipVertical() flipping the image vertically} and scaling
         * with the absolute value.
         * <P>
         * The client code should also expect that this property might be zero,
         * {@code NaN} or even infinity. How these special values are handled
         * are left to the client code.
         *
         * @return the scaling factor used to scale the image vertically
         */
        public double getZoomY() {
            return zoomY;
        }

        /**
         * Sets the scaling factor used to scale the image vertically. The
         * scaling must be interpreted in the coordinate system of the original
         * image. That is, {@code zoomY} adjusts the height of the original
         * image.
         *
         * @param zoomY the scaling factor used to scale the image vertically
         */
        public void setZoomY(double zoomY) {
            this.zoomY = canonicalizeDouble(zoomY);
        }

        /**
         * Sets the scaling factors used to scale the image horizontally and
         * vertically to the same value. The scaling must be interpreted in the
         * coordinate system of the original image. That is, {@code zoom}
         * adjusts both the width and the height of the original image.
         * <P>
         * This method is effectively equivalent to calling:
         * {@code setZoomX(zoom)} and {@code setZoomY(zoom)}.
         *
         * @param zoom the scaling factor used to scale the image both
         *   horizontally and vertically
         */
        public void setZoom(double zoom) {
            zoom = canonicalizeDouble(zoom);
            this.zoomX = zoom;
            this.zoomY = zoom;
        }

        /**
         * Returns the string representation of this transformation in no
         * particular format
         * <P>
         * This method is intended to be used for debugging only.
         *
         * @return the string representation of this object in no particular format.
         *   This method never returns {@code null}.
         */
        @Override
        public String toString() {
            return create().toString();
        }
    }

    private final int rotateDeg;
    private final double rotateRad;

    private final boolean flipHorizontal;
    private final boolean flipVertical;

    private final double zoomX; // if < 1, image is shrinked. if > 1, magnify
    private final double zoomY;

    private final double offsetX; // scale is according to the original size
    private final double offsetY;

    // Must not be initialized, or define it to be volatile
    private int cachedHash;

    // Does not need to be volatile because it is immutable
    private EffectiveValues effectiveValues;

    private BasicImageTransformations(Builder transformations) {
        rotateDeg = transformations.rotateDeg;
        rotateRad = transformations.rotateRad;

        flipHorizontal = transformations.flipHorizontal;
        flipVertical = transformations.flipVertical;

        zoomX = transformations.zoomX;
        zoomY = transformations.zoomY;

        offsetX = transformations.offsetX;
        offsetY = transformations.offsetY;
    }

    /**
     * Returns {@code true} if the image needs to be flipped horizontally.
     * That is, the left side of the image need to be the right side of the
     * image.
     *
     * @return {@code true} if the image needs to be flipped horizontally,
     *   {@code false} otherwise
     */
    public boolean isFlipHorizontal() {
        return flipHorizontal;
    }

    /**
     * Returns {@code true} if the image needs to be flipped vertically.
     * That is, the top side of the image need to be the bottom side of the
     * image.
     *
     * @return {@code true} if the image needs to be flipped vertically,
     *   {@code false} otherwise
     */
    public boolean isFlipVertical() {
        return flipVertical;
    }

    /**
     * Returns the offset along the horizontal axis. Note that the offset is to
     * be applied after zoom has been applied.
     *
     * @return the offset along the horizontal axis
     */
    public double getOffsetX() {
        return offsetX;
    }

    /**
     * Returns the offset along the vertical axis. Note that the offset is to
     * be applied after zoom has been applied.
     *
     * @return the offset along the vertical axis
     */
    public double getOffsetY() {
        return offsetY;
    }

    /**
     * Returns the angle meaning how much the image need to be rotated around
     * its center in radians. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     *
     * @return the angle meaning how much the image need to be rotated around
     *   its center in radians. This method returns always returns a normalized
     *   angle. That is, a value between 0 and {@code 2*pi} or {@code NaN} if
     *   {@code NaN} was set.
     */
    public int getRotateInDegrees() {
        return rotateDeg;
    }

    /**
     * Returns the angle meaning how much the image need to be rotated around
     * its center in degrees. As the angle increases, the image need to be
     * rotated clockwise. The zero angle means that the image is not rotated at
     * all.
     * <P>
     * Notice that this property is an {@code int}, if you need better
     * precision, use the {@link #getRotateInRadians() getRotateInRadians()}
     * method. This method will round the value to the closest integer.
     *
     * @return the angle meaning how much the image need to be rotated around
     *   its center in radians. This method returns always returns a normalized
     *   angle. That is, a value which is greater than or equal to zero and
     *   less than (not equal) to 360 or {@code NaN} or infinity.
     */
    public double getRotateInRadians() {
        return rotateRad;
    }

    /**
     * Returns the scaling factor used to scale the image horizontally. For
     * example: If this method returns {@code 2.0}, the image must be scaled
     * to an image which has twice the width of the original image.
     * Respectively, a value of {@code 0.5} means that the resulting image must
     * be scaled to an image which has half the width of the original image.
     * <P>
     * This method may return a negative value which should be interpreted as
     * {@link #isFlipHorizontal() flipping the image horizontally} and scaling
     * with the absolute value.
     * <P>
     * The client code should also expect that this property might be zero,
     * {@code NaN} or even infinity. How these special values are handled are
     * left to the client code.
     *
     * @return the scaling factor used to scale the image horizontally
     */
    public double getZoomX() {
        return zoomX;
    }

    /**
     * Returns the scaling factor used to scale the image vertically. For
     * example: If this method returns {@code 2.0}, the image must be scaled
     * to an image which has twice the height of the original image.
     * Respectively, a value of {@code 0.5} means that the resulting image must
     * be scaled to an image which has half the height of the original image.
     * <P>
     * This method may return a negative value which should be interpreted as
     * {@link #isFlipVertical() flipping the image vertically} and scaling
     * with the absolute value.
     * <P>
     * The client code should also expect that this property might be zero,
     * {@code NaN} or even infinity. How these special values are handled are
     * left to the client code.
     *
     * @return the scaling factor used to scale the image vertically
     */
    public double getZoomY() {
        return zoomY;
    }

    /**
     * Returns {@code true} if this transformation defines the identity
     * transformation. That is, the same value as
     * {@code equals(identityTransformation())}.
     * <P>
     * Notice that two distinct {@code BasicImageTransformations} may define
     * the same transformation with different properties (due to flipping). This
     * method takes this into account.
     *
     * @return {@code true} if this transformation defines the identity
     *   transformation, {@code false} otherwise
     */
    public boolean isIdentity() {
        return equals(IDENTITY);
    }

    private EffectiveValues getEffectiveValues() {
        EffectiveValues result = effectiveValues;
        if (result == null) {
            result = new EffectiveValues(this);
            effectiveValues = result;
        }
        return result;
    }

    private static int doubleHash(double value) {
        return (int)(Double.doubleToLongBits(value)
                ^ (Double.doubleToLongBits(value) >>> 32));
    }

    /**
     * Returns a hash code value compatible with the
     * {@link #equals(Object) equals} method, usable in hash tables.
     *
     * @return the hash code value of this object
     */
    @Override
    public int hashCode() {
        int hash = cachedHash;

        // The "not yet computed" value must be the default value for int
        // values (i.e.: 0)
        if (hash == 0) {
            EffectiveValues effective = getEffectiveValues();

            hash = 7;
            hash = 83 * hash + doubleHash(effective.rotateRad);
            hash = 83 * hash + doubleHash(effective.zoomX);
            hash = 83 * hash + doubleHash(effective.zoomY);
            hash = 83 * hash + doubleHash(this.offsetX);
            hash = 83 * hash + doubleHash(this.offsetY);
            // 0 hash is reserved for "not yet computed"
            if (hash == 0) hash = 1;

            cachedHash = hash;
        }
        return hash;
    }

    /**
     * Checks if the specified object is a {@code BasicImageTransformations}
     * and defines the same transformation as this
     * {@code BasicImageTransformations}.
     * <P>
     * Notice that two distinct {@code BasicImageTransformations} may define
     * the same transformation with different properties (due to flipping). This
     * method takes this into account.
     *
     * @param obj the object to which this {@code BasicImageTransformation} is
     *   to be compared to. This argument can be {@code null}, in which case the
     *   return value is {@code false}.
     * @return {@code true} if the specified object is a
     *   {@code BasicImageTransformations} and defines the same transformation
     *   as this {@code BasicImageTransformations}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BasicImageTransformations other = (BasicImageTransformations)obj;
        if (Double.doubleToLongBits(this.offsetX) != Double.doubleToLongBits(other.offsetX))
            return false;
        if (Double.doubleToLongBits(this.offsetY) != Double.doubleToLongBits(other.offsetY))
            return false;

        EffectiveValues effective1 = getEffectiveValues();
        EffectiveValues effective2 = other.getEffectiveValues();

        if (Double.doubleToLongBits(effective1.rotateRad) != Double.doubleToLongBits(effective2.rotateRad))
            return false;
        if (Double.doubleToLongBits(effective1.zoomX) != Double.doubleToLongBits(effective2.zoomX))
            return false;
        if (Double.doubleToLongBits(effective1.zoomY) != Double.doubleToLongBits(effective2.zoomY))
            return false;
        return true;
    }

    private static boolean appendSeparator(StringBuilder result, boolean hasPrev) {
        if (hasPrev) {
            result.append(", ");
        }

        return true;
    }

    /**
     * Returns the string representation of this transformation in no
     * particular format
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        if (isIdentity()) {
            return "Identity transformation";
        }

        boolean hasPrev = false;
        StringBuilder result = new StringBuilder(EXPECTED_MAX_TO_STRING_LENGTH);
        result.append("Transformation: {");

        if (offsetX != 0.0 || offsetY != 0.0) {
            hasPrev = true;
            result.append("Offset (x, y) = (");
            result.append(offsetX);
            result.append(", ");
            result.append(offsetY);
            result.append(")");
        }

        if (flipHorizontal) {
            hasPrev = appendSeparator(result, hasPrev);
            result.append("mirrored horizontaly");
        }

        if (flipVertical) {
            hasPrev = appendSeparator(result, hasPrev);
            result.append("mirrored verticaly");
        }

        if (zoomX == zoomY) {
            if (zoomX != 1.0) {
                hasPrev = appendSeparator(result, hasPrev);
                result.append("Zoom = ");
                result.append(zoomX);
            }
        }
        else {
            hasPrev = appendSeparator(result, hasPrev);
            result.append("ZoomX = ");
            result.append(zoomX);
            result.append(", ZoomY = ");
            result.append(zoomY);
        }


        if (rotateRad != 0.0) {
            double degrees;

            if (rotateRad == RAD_90) {
                degrees = DEGREES_90;
            }
            else if (rotateRad == RAD_180) {
                degrees = DEGREES_180;
            }
            else if (rotateRad == RAD_270) {
                degrees = DEGREES_270;
            }
            else {
                degrees = Math.toDegrees(rotateRad);
            }

            appendSeparator(result, hasPrev);
            result.append("Rotate (degrees) = ");
            result.append(degrees);
        }

        result.append('}');
        return result.toString();
    }

    private static final class EffectiveValues {
        public final double zoomX;
        public final double zoomY;
        public final double rotateRad;

        public EffectiveValues(BasicImageTransformations transf) {
            double effectiveZoomX = transf.flipHorizontal ? -transf.zoomX : transf.zoomX;
            double effectiveZoomY = transf.flipVertical ? -transf.zoomY : transf.zoomY;
            double effectiveRotate = transf.rotateRad;
            if (effectiveZoomX < 0.0 && effectiveZoomY < 0.0) {
                effectiveZoomX = -effectiveZoomX;
                effectiveZoomY = -effectiveZoomY;
                effectiveRotate = normalizeRadians(effectiveRotate + Math.PI);
            }

            this.zoomX = effectiveZoomX;
            this.zoomY = effectiveZoomY;
            this.rotateRad = effectiveRotate;
        }
    }
}
