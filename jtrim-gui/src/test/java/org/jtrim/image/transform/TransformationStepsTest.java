package org.jtrim.image.transform;

import org.jtrim.cache.ReferenceType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TransformationStepsTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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
