package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
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
        ImagePointTransformer pointTransformer = transformedImage.getPointTransformer();

        assertNotNull(pointTransformer);

        Point2D origSrc = new Point2D.Double(26.0, 26.0);
        Point2D src1 = (Point2D)origSrc.clone();
        Point2D dest1 = new Point2D.Double();
        pointTransformer.transformSrcToDest(src1, dest1);
        assertEquals(origSrc, src1);
        assertEquals(origSrc, dest1);

        Point2D origDest = new Point2D.Double(26.0, 26.0);
        Point2D dest2 = (Point2D)origSrc.clone();
        Point2D src2 = new Point2D.Double();
        pointTransformer.transformDestToSrc(dest2, src2);
        assertEquals(origDest, src2);
        assertEquals(origDest, dest2);
    }
}