package org.jtrim.image.transform;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim.image.ImageData;
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
public class TransformedImageTest {
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

    private static BufferedImage createTestImage() {
        return new BufferedImage(10, 15, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testProperties() throws Exception {
        for (BufferedImage image: Arrays.asList(null, createTestImage())) {
            ImagePointTransformer pointTransformer = mock(ImagePointTransformer.class);

            TransformedImage transformedImage = new TransformedImage(image, pointTransformer);
            assertSame(image, transformedImage.getImage());
            assertSame(pointTransformer, transformedImage.getPointTransformer());
            assertEquals(ImageData.getApproxSize(image), transformedImage.getApproxMemorySize());

            Point2D arg1 = mock(Point2D.class);
            Point2D arg2 = mock(Point2D.class);

            transformedImage.transformDestToSrc(arg1, arg2);
            verify(pointTransformer).transformDestToSrc(same(arg1), same(arg2));

            transformedImage.transformSrcToDest(arg1, arg2);
            verify(pointTransformer).transformSrcToDest(same(arg1), same(arg2));
            verifyNoMoreInteractions(pointTransformer);
        }
    }

    @Test
    public void testGetPointTransformerIdentity() throws Exception {
        TransformedImage transformedImage = new TransformedImage(null, null);
        assertSame(AffineImagePointTransformer.IDENTITY, transformedImage.getPointTransformer());
    }
}
