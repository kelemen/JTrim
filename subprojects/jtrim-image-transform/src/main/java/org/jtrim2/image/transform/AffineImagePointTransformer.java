package org.jtrim2.image.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * Defines a coordinate transformation of 2D points (two dimensional vectors)
 * where the coordinate transformation can be described by an affine
 * transformation. That is, where the transformation can be represented by a
 * linear transformation and an offset.
 *
 * <h2>Thread safety</h2>
 * Instances of this class are safe to be called from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are <I>synchronization transparent</I>.
 */
public final class AffineImagePointTransformer implements ImagePointTransformer {
    /**
     * Defines an identity transformation. That is, a coordinate transformation
     * which transforms input coordinates to the same coordinates.
     */
    public static final ImagePointTransformer IDENTITY
            = new IdentityImageTransformation();

    private final AffineTransform srcToDest;

    /**
     * Creates a new {@code AffineImagePointTransformer} which is based on the
     * the given affine transformation. The source coordinate system is from
     * which the specified {@code AffineTransform.transform} method transforms
     * the 2D points.
     *
     * @param srcToDest the {@code AffineTransform} defining the transformation
     *   from the source coordinates to destination coordinates. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified transformation is
     *   {@code null}
     */
    public AffineImagePointTransformer(AffineTransform srcToDest) {
        Objects.requireNonNull(srcToDest, "srcToDest");

        this.srcToDest = new AffineTransform(srcToDest);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void transformSrcToDest(Point2D src, Point2D dest) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(dest, "dest");

        srcToDest.transform(src, dest);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: The transformation is not invertible if it
     * transforms points to a line or to a single point.
     */
    @Override
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(dest, "dest");

        srcToDest.inverseTransform(dest, src);
    }

    private static final class IdentityImageTransformation implements ImagePointTransformer {
        @Override
        public void transformSrcToDest(Point2D src, Point2D dest) {
            dest.setLocation(src);
        }

        @Override
        public void transformDestToSrc(Point2D dest, Point2D src) {
            src.setLocation(dest);
        }

        @Override
        public String toString() {
            return "Identity transformation";
        }
    }
}
