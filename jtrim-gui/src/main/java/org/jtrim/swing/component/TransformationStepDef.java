package org.jtrim.swing.component;

import org.jtrim.cache.ReferenceType;

/**
 *
 * @author Kelemen Attila
 */
public interface TransformationStepDef {
    public TransformationStepPos getPosition();
    public void setTransformation(ReferenceType cacheType, ImageTransformationStep transformation);
    public void removeStep();
}
