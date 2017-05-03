package org.jtrim2.image.transform;

import org.jtrim2.cache.ReferenceType;

/**
 * Contains static utility methods related to {@link ImageTransformationStep}.
 *
 * @author Kelemen Attila
 */
public final class TransformationSteps {
    /**
     * Returns an {@code ImageTransformationStep} which delegates its call
     * to the specified {@code ImageTransformationStep} but caches its result if
     * possible. Caching might be done with weak, soft or hard references.
     * <P>
     * Caching is done by retaining a reference to both the input and the output
     * of the passed {@code ImageTransformationStep} and whenever a new input
     * is available checks if it is the same as the one cached. If the inputs
     * are matching, then the cached result is returned.
     * <P>
     * <B>Warning</B>: The returned is {@code ImageTransformationStep} is not
     * safe to be accessed concurrently.
     *
     * @param refType the type of the reference used to retain both the input
     *   and the output of the cached {@code ImageTransformationStep}. For this
     *   argument {@link ReferenceType#UserRefType} is equivalent to
     *   {@link ReferenceType#NoRefType}. This argument cannot be {@code null}.
     * @param step the {@code ImageTransformationStep} to which calls are
     *   delegated to if the cached output is not available or out-of-date. This
     *   argument cannot be {@code null}.
     * @param cacheCmp the comparison which is able to tell if the specified
     *   {@code ImageTransformationStep} will yield the same output for two
     *   inputs or not. This comparison should be quick and may return
     *   {@code false} if the exact check would be too slow. That is, the
     *   comparison is expected to do little more work than comparing
     *   references. This argument cannot be {@code null}.
     * @return the {@code ImageTransformationStep} which delegates its call to
     *   the specified {@code ImageTransformationStep} but caches its result if
     *   possible. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static ImageTransformationStep cachedStep(
            ReferenceType refType,
            ImageTransformationStep step,
            TransformationStepInput.Cmp cacheCmp) {
        return new CachingImageTransformationStep(refType, step, cacheCmp);
    }

    private TransformationSteps() {
        throw new AssertionError();
    }
}
