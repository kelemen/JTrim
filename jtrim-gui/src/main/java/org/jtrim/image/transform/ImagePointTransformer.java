package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * Defines an interface for transforming 2D points (two dimensional vectors)
 * from one coordinate system to another.
 * <P>
 * Each instance of {@code ImagePointTransformer} must define the source and
 * the destination coordinate system because the methods use these terms.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to be called from
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface required to be <I>synchronization transparent</I>.
 *
 * @see AffineImagePointTransformer
 *
 * @author Kelemen Attila
 */
public interface ImagePointTransformer {
    /**
     * Transforms the given 2D point from the source coordinate system to the
     * destination coordinate system.
     * <P>
     * The source and destination point objects are allowed to be the same
     * object.
     *
     * @param src the point to be transformed to the destination coordinate
     *   system. This argument cannot be {@code null} and this method is not
     *   allowed to modify this argument.
     * @param dest the point object where the result of the transformation is
     *   to be stored. This method modifies this argument. This argument cannot
     *   be {@code null}.
     */
    public void transformSrcToDest(Point2D src, Point2D dest);

    /**
     * Transforms the given 2D point from the destination coordinate system to
     * the source coordinate system.
     * <P>
     * The source and destination point objects are allowed to be the same
     * object.
     * <P>
     * This method is the inverse transformation of
     * {@link #transformSrcToDest(Point2D, Point2D) transformSrcToDest}. Note
     * however, that due to rounding errors, applying this method to a point
     * created by {@code transformSrcToDest} may not yield exactly the same
     * source point.
     *
     * @param dest the point to be transformed to the source coordinate
     *   system. This argument cannot be {@code null} and this method is not
     *   allowed to modify this argument.
     * @param src the point object where the result of the transformation is
     *   to be stored. This method modifies this argument. This argument cannot
     *   be {@code null}.
     *
     * @throws NoninvertibleTransformException thrown if the transformation
     *   cannot be done because the source to destination transformation cannot
     *   be inverted
     */
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException;
}
