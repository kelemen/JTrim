package org.jtrim2.image.transform;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim2.image.BufferedImages;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TransformedImageTest {
    private static BufferedImage createTestImage() {
        return new BufferedImage(10, 15, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testNullImage() {
        assertNull(TransformedImage.NULL_IMAGE.getImage());
        assertSame(AffineImagePointTransformer.IDENTITY, TransformedImage.NULL_IMAGE.getPointTransformer());
    }

    @Test
    public void testProperties() throws Exception {
        for (BufferedImage image: Arrays.asList(null, createTestImage())) {
            ImagePointTransformer pointTransformer = mock(ImagePointTransformer.class);

            TransformedImage transformedImage = new TransformedImage(image, pointTransformer);
            assertSame(image, transformedImage.getImage());
            assertSame(pointTransformer, transformedImage.getPointTransformer());
            assertEquals(BufferedImages.getApproxSize(image), transformedImage.getApproxMemorySize());

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
