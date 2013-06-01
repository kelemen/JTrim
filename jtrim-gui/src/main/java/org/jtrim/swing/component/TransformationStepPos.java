package org.jtrim.swing.component;

/**
 * Defines a reference which might be used to add a new transformation step
 * before or after a {@link TransformationStepDef}.
 * <P>
 * Instances of this interface are associated with a
 * {@code TransformationStepDef} and so may only be used while the associated
 * {@code TransformationStepDef} is not removed.
 * <P>
 * Since {@code TransformationStepPos} instances are associated with a
 * {@code TransformedImageDisplay} (because they are associated with a
 * {@code TransformationStepDef}, its method may only be called from the the AWT
 * Event Dispatch Thread.
 *
 * @see TransformationStepDef
 *
 * @author Kelemen Attila
 */
public interface TransformationStepPos {
    /**
     * Adds a new {@code TransformationStepDef} to apply before the
     * {@link TransformationStepDef} associated with this
     * {@code TransformationStepPos}.
     * <P>
     * This method may not be called after the associated
     * {@code TransformationStepDef} was removed.
     *
     * @return a new {@code TransformationStepDef} to apply before the
     *   {@link TransformationStepDef} associated with this
     *   {@code TransformationStepPos}. This method may never return
     *   {@code null}.
     */
    public TransformationStepDef addBefore();

    /**
     * Adds a new {@code TransformationStepDef} to apply after the
     * {@link TransformationStepDef} associated with this
     * {@code TransformationStepPos}.
     * <P>
     * This method may not be called after the associated
     * {@code TransformationStepDef} was removed.
     *
     * @return a new {@code TransformationStepDef} to apply after the
     *   {@link TransformationStepDef} associated with this
     *   {@code TransformationStepPos}. This method may never return
     *   {@code null}.
     */
    public TransformationStepDef addAfter();
}
