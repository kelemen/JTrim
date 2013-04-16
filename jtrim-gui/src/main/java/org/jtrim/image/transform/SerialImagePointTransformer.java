package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a coordinate transformation based on a list of other coordinate
 * transformations. That is, this coordinate transformation simply applies the
 * transformations in the order the transformations were specified at
 * construction time.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are not <I>synchronization transparent</I> and
 * calling them while holding a lock should be avoided.
 *
 * @author Kelemen Attila
 */
public final class SerialImagePointTransformer implements ImagePointTransformer {
    private final ImagePointTransformer[] transformers;

    private static ImagePointTransformer[] unfold(
            List<? extends ImagePointTransformer> transformers) {

        List<ImagePointTransformer> result = new LinkedList<>();
        for (ImagePointTransformer transformer: transformers) {
            if (transformer.getClass() == SerialImagePointTransformer.class) {
                result.addAll(Arrays.asList(((SerialImagePointTransformer)transformer).transformers));
            }
            else {
                result.add(transformer);
            }
        }
        return result.toArray(new ImagePointTransformer[result.size()]);
    }

    /**
     * Creates a new {@code SerialImagePointTransformer} from an array of
     * coordinate transformations.
     *
     * @param transformers the coordinate transformations in the order they
     *   need to be applied. This argument cannot be {@code null} and cannot
     *   contain {@code null} elements. If this array is empty, the newly
     *   created {@code SerialImagePointTransformer} will be equivalent to the
     *   identity transformation.
     *
     * @throws NullPointerException thrown if the coordinate transformation
     *   array or any of its element is {@code null}
     */
    public SerialImagePointTransformer(ImagePointTransformer... transformers) {
        this.transformers = unfold(Arrays.asList(transformers));
        ExceptionHelper.checkNotNullElements(this.transformers, "transformers");
    }

    /**
     * Creates a new {@code SerialImagePointTransformer} from a list of
     * coordinate transformations.
     *
     * @param transformers the coordinate transformations in the order they
     *   need to be applied. This argument cannot be {@code null} and cannot
     *   contain {@code null} elements. If this list is empty, the newly created
     *   {@code SerialImagePointTransformer} will be equivalent to the identity
     *   transformation.
     *
     * @throws NullPointerException thrown if the coordinate transformation
     *   list or any of its element is {@code null}
     */
    public SerialImagePointTransformer(List<? extends ImagePointTransformer> transformers) {
        this.transformers = unfold(transformers);
        ExceptionHelper.checkNotNullElements(this.transformers, "transformers");
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method will simply call the
     * {@link ImagePointTransformer#transformSrcToDest(Point2D, Point2D) transformSrcToDest}
     * method of the coordinate transformations in the order they were specified
     * at construction time.
     */
    @Override
    public void transformSrcToDest(Point2D src, Point2D dest) {
        dest.setLocation(src);
        for (ImagePointTransformer transformer: transformers) {
            transformer.transformSrcToDest(dest, dest);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method will simply call the
     * {@link ImagePointTransformer#transformDestToSrc(Point2D, Point2D) transformDestToSrc}
     * method of the coordinate transformations in the reverse order they were
     * specified at construction time.
     */
    @Override
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        src.setLocation(dest);
        for (int i = transformers.length - 1; i >= 0; i--) {
            transformers[i].transformDestToSrc(src, src);
        }
    }
}
