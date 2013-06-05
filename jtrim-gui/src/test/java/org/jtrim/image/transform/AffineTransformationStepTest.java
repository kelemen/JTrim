
package org.jtrim.image.transform;

import java.awt.geom.AffineTransform;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class AffineTransformationStepTest {
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

    @Test
    public void testCommonStaticMethods() {
        CommonAffineTransformationsTests.testCommonTransformations(new CommonAffineTransformations() {
            @Override
            public AffineTransform getTransformationMatrix(
                    BasicImageTransformations transformations) {
                return AffineTransformationStep.getTransformationMatrix(transformations);
            }

            @Override
            public AffineTransform getTransformationMatrix(
                    BasicImageTransformations transformations,
                    double srcWidth,
                    double srcHeight,
                    double destWidth,
                    double destHeight) {

                return AffineTransformationStep.getTransformationMatrix(
                        transformations, srcWidth, srcHeight, destWidth, destHeight);
            }

            @Override
            public boolean isSimpleTransformation(BasicImageTransformations transformation) {
                return AffineTransformationStep.isSimpleTransformation(transformation);
            }
        });
    }
}
