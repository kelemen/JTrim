package org.jtrim2.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.jtrim2.utils.ExceptionHelper;

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

    /**
     * Returns a transformation equivalent to applying all the specified
     * transformations in the given order.
     * <P>
     * This method does not necessarily returns an instance of
     * {@code SerialImagePointTransformer}. For example, if you specify only
     * a single transformation, then that transformation is returned.
     *
     * @param transformers the coordinate transformations to be applied in the
     *   order they need to be applied. This argument cannot be {@code null}
     *   and cannot contain {@code null} elements. This argument can be an
     *   empty array, in which case an identity transformation is returned.
     * @return a transformation equivalent to applying all the specified
     *   transformations in the given order. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified array or any of its
     *   element is {@code null}
     */
    public static ImagePointTransformer combine(ImagePointTransformer... transformers) {
        return combine(Arrays.asList(transformers));
    }

    /**
     * Returns a transformation equivalent to applying all the specified
     * transformations in the given order.
     * <P>
     * This method does not necessarily returns an instance of
     * {@code SerialImagePointTransformer}. For example, if you specify only
     * a single transformation, then that transformation is returned.
     *
     * @param transformers the coordinate transformations to be applied in the
     *   order they need to be applied. This argument cannot be {@code null}
     *   and cannot contain {@code null} elements. This argument can be an
     *   empty list, in which case an identity transformation is returned.
     * @return a transformation equivalent to applying all the specified
     *   transformations in the given order. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified list or any of its
     *   element is {@code null}
     */
    public static ImagePointTransformer combine(List<? extends ImagePointTransformer> transformers) {
        ImagePointTransformer[] filtered = unfold(transformers);

        switch (filtered.length) {
            case 0:
                return AffineImagePointTransformer.IDENTITY;
            case 1:
                return filtered[0];
            default:
                return new SerialImagePointTransformer(filtered);
        }
    }

    private static ImagePointTransformer[] unfold(
            List<? extends ImagePointTransformer> transformers) {
        int transformerCount = transformers.size();
        if (transformerCount <= 1) {
            ImagePointTransformer result = transformerCount == 0
                    ? AffineImagePointTransformer.IDENTITY
                    : transformers.get(0);
            Objects.requireNonNull(result, "transformers[0]");
            return new ImagePointTransformer[]{result};
        }

        List<ImagePointTransformer> result = new LinkedList<>();
        for (ImagePointTransformer transformer: transformers) {
            if (transformer.getClass() == SerialImagePointTransformer.class) {
                result.addAll(Arrays.asList(((SerialImagePointTransformer)transformer).transformers));
            }
            else if (transformer != AffineImagePointTransformer.IDENTITY) {
                result.add(transformer);
            }
        }
        return result.toArray(new ImagePointTransformer[result.size()]);
    }

    private SerialImagePointTransformer(ImagePointTransformer... transformers) {
        this.transformers = transformers.clone();
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
