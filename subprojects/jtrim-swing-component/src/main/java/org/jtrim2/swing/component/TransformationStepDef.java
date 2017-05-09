package org.jtrim2.swing.component;

import org.jtrim2.image.transform.ImageTransformationStep;

/**
 * Defines a single transformation step of {@link TransformedImageDisplay}.
 * Instances of this interface are meant to be a logical definition of a
 * transformation step. The actual transformation is applied by the
 * {@link ImageTransformationStep} set for the {@code TransformationStepDef}.
 * <P>
 * You may define additional transformations to be applied before or after
 * this transformation by calling {@code getPosition().addBefore()} or
 * {@code getPosition().addAfter()}. Also, you may permanently remove this
 * transformation step by calling {@link #removeStep()}.
 * <P>
 * Since {@code TransformationStepDef} instances are associated with a
 * {@code TransformedImageDisplay}, its method may only be called from the
 * the AWT Event Dispatch Thread.
 *
 * @see TransformedImageDisplay
 */
public interface TransformationStepDef {
    /**
     * Returns a {@code TransformationStepPos} which might be used to add a
     * transformation to be applied before or after this transformation.
     * Note that it is not possible to add transformations through the returned
     * reference after this {@code TransformationStepDef} has been
     * {@link #removeStep() removed}.
     *
     * @return a {@code TransformationStepPos} which might be used to add a
     *   transformation to be applied before or after this transformation. This
     *   method never returns {@code null}.
     */
    public TransformationStepPos getPosition();

    /**
     * Replaces the transformation to be actually applied by this transformation
     * step. Setting {@code null} will effectively cause this transformation to
     * be skipped by the renderer of {@link TransformedImageDisplay}.
     *
     * @param transformation the transformation to be actually applied by this
     *   transformation step. This argument can be {@code null} if this step
     *   should apply an identity transformation.
     *
     * @see org.jtrim2.image.transform.TransformationSteps#cachedStep(ReferenceType, ImageTransformationStep, TransformationStepInput.Cmp) TransformationSteps.cachedStep
     */
    public void setTransformation(ImageTransformationStep transformation);

    /**
     * Removes this transformation step permanently. After this
     * {@code TransformationStepDef} has been removed, it will no longer apply
     * its transformation set by the
     * {@link #setTransformation(ImageTransformationStep) setTransformation}
     * method.
     * <P>
     * Note: Attempting to add a transformation before or after this
     * {@code TransformationStepDef} will fail after {@code removeStep()} has
     * been called.
     */
    public void removeStep();
}
