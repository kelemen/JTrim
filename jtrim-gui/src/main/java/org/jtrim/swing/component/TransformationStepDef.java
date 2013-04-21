package org.jtrim.swing.component;

/**
 *
 * @author Kelemen Attila
 */
public interface TransformationStepDef {
    public TransformationStepPos getPosition();
    public void setTransformation(ImageTransformationStep transformation);
    public void removeStep();
}
