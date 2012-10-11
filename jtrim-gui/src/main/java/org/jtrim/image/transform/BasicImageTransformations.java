package org.jtrim.image.transform;

/**
 *
 * @author Kelemen Attila
 */
public final class BasicImageTransformations {
    private static final double ROTATE_0 = 0.0;
    private static final double ROTATE_90 = Math.PI / 2;
    private static final double ROTATE_180 = Math.PI;
    private static final double ROTATE_270 = 3 * Math.PI / 2;

    private static final double PI2 = 2.0 * Math.PI;
    private static final BasicImageTransformations IDENTITY
            = new BasicImageTransformations.Builder().create();

    public static BasicImageTransformations identityTransformation() {
        return IDENTITY;
    }

    public static BasicImageTransformations newZoomTransformation(
            double zoomX, double zoomY) {

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder();

        result.setZoomX(zoomX);
        result.setZoomY(zoomY);
        return result.create();
    }

    public static BasicImageTransformations newOffsetTransformation(
            double offsetX, double offsetY) {

        BasicImageTransformations.Builder result;
        result = new BasicImageTransformations.Builder();

        result.setOffset(offsetX, offsetY);
        return result.create();
    }

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
        double norm = Math.IEEEremainder(rad, PI2);
        if (norm < 0.0) {
            norm += PI2;
        }
        return norm;
    }

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

        public BasicImageTransformations create() {
            return new BasicImageTransformations(this);
        }

        public boolean isFlipHorizontal() {
            return flipHorizontal;
        }

        public void setFlipHorizontal(boolean flipHorizontal) {
            this.flipHorizontal = flipHorizontal;
        }

        public void flipHorizontal() {
            flipHorizontal = !flipHorizontal;
        }

        public boolean isFlipVertical() {
            return flipVertical;
        }

        public void setFlipVertical(boolean flipVertical) {
            this.flipVertical = flipVertical;
        }

        public void flipVertical() {
            flipVertical = !flipVertical;
        }

        public double getOffsetX() {
            return offsetX;
        }

        public double getOffsetY() {
            return offsetY;
        }

        public void setOffset(double offsetX, double offsetY) {
            this.offsetX = canonicalizeDouble(offsetX);
            this.offsetY = canonicalizeDouble(offsetY);
        }

        public double getRotateInRadians() {
            return rotateRad;
        }

        public void setRotateInRadians(double radians) {
            rotateRad = canonicalizeDouble(normalizeRadians(radians));
            rotateDeg = (int) Math.round(Math.toDegrees(rotateRad));

            // just in case of rounding errors
            rotateDeg = rotateDeg % 360;
            if (rotateDeg < 0) {
                rotateDeg += 360;
            }
        }

        public int getRotateInDegrees() {
            return rotateDeg;
        }

        public void setRotateInDegrees(int degrees) {
            // This works on negative numbers as well.
            rotateDeg = degrees % 360;
            if (rotateDeg < 0) {
                rotateDeg += 360;
            }

            // Be as precise as possible in these
            // important special cases.
            switch (rotateDeg) {
                case 0:
                    rotateRad = ROTATE_0;
                    break;
                case 90:
                    rotateRad = ROTATE_90;
                    break;
                case 180:
                    rotateRad = ROTATE_180;
                    break;
                case 270:
                    rotateRad = ROTATE_270;
                    break;
                default:
                    rotateRad = canonicalizeDouble(Math.toRadians(rotateDeg));
                    break;
            }
        }

        public double getZoomX() {
            return zoomX;
        }

        public void setZoomX(double zoomX) {
            this.zoomX = canonicalizeDouble(zoomX);
        }

        public double getZoomY() {
            return zoomY;
        }

        public void setZoomY(double zoomY) {
            this.zoomY = canonicalizeDouble(zoomY);
        }

        public void setZoom(double zoom) {
            zoom = canonicalizeDouble(zoom);
            this.zoomX = zoom;
            this.zoomY = zoom;
        }

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

    public boolean isFlipHorizontal() {
        return flipHorizontal;
    }

    public boolean isFlipVertical() {
        return flipVertical;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public int getRotateInDegrees() {
        return rotateDeg;
    }

    public double getRotateInRadians() {
        return rotateRad;
    }

    public double getZoomX() {
        return zoomX;
    }

    public double getZoomY() {
        return zoomY;
    }

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

    @Override
    public int hashCode() {
        int hash = cachedHash;

        // The "not yet computed" value must be the default value for int
        // values (i.e.: 0)
        if (hash == 0) {
            EffectiveValues effective = getEffectiveValues();

            hash = 7;
            hash = 83 * hash + (int)(Double.doubleToLongBits(effective.rotateRad) ^ (Double.doubleToLongBits(effective.rotateRad) >>> 32));
            hash = 83 * hash + (this.flipHorizontal ? 1 : 0);
            hash = 83 * hash + (this.flipVertical ? 1 : 0);
            hash = 83 * hash + (int)(Double.doubleToLongBits(effective.zoomX) ^ (Double.doubleToLongBits(effective.zoomX) >>> 32));
            hash = 83 * hash + (int)(Double.doubleToLongBits(effective.zoomY) ^ (Double.doubleToLongBits(effective.zoomY) >>> 32));
            hash = 83 * hash + (int)(Double.doubleToLongBits(this.offsetX) ^ (Double.doubleToLongBits(this.offsetX) >>> 32));
            hash = 83 * hash + (int)(Double.doubleToLongBits(this.offsetY) ^ (Double.doubleToLongBits(this.offsetY) >>> 32));
            // 0 hash is reserved for "not yet computed"
            if (hash == 0) hash = 1;

            cachedHash = hash;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
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

    @Override
    public String toString() {
        if (isIdentity()) {
            return "Identity transformation";
        }

        boolean hasPrev = false;
        StringBuilder result = new StringBuilder(256);
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

            if (rotateRad == ROTATE_90) {
                degrees = 90.0;
            }
            else if (rotateRad == ROTATE_180) {
                degrees = 180.0;
            }
            else if (rotateRad == ROTATE_270) {
                degrees = 270.0;
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
