package org.jtrim2.image.transform;

import org.jtrim2.cache.ReferenceType;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransformationStepsTest {
    /**
     * Test of cachedStep method, of class TransformationSteps.
     */
    @Test
    public void testCachedStep() {
        ImageTransformationStep step = TransformationSteps.cachedStep(
                ReferenceType.NoRefType,
                mock(ImageTransformationStep.class),
                mock(TransformationStepInput.Cmp.class));
        assertTrue(step instanceof CachingImageTransformationStep);
    }
}
