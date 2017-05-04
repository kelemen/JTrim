package org.jtrim2.image.transform;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import org.jtrim2.image.BufferedImages;
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
@SuppressWarnings("deprecation")
public class TransformedImageDataTest {
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
    public void testProperties() {
        for (TransformedImage image: Arrays.asList(null, new TransformedImage(null, null))) {
            List<org.jtrim2.image.ImageReceiveException> exceptions
                    = Arrays.asList(null, new org.jtrim2.image.ImageReceiveException());
            for (org.jtrim2.image.ImageReceiveException exception: exceptions) {
                TransformedImageData data = new TransformedImageData(image, exception);
                assertSame(image, data.getTransformedImage());
                assertSame(exception, data.getException());
            }
        }
    }

    @Test
    public void testGetImage1() {
        TransformedImageData data = new TransformedImageData(null, null);
        assertNull(data.getImage());
    }

    @Test
    public void testGetImage2() {
        BufferedImage image = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImagePointTransformer transformer = mock(ImagePointTransformer.class);
        TransformedImageData data = new TransformedImageData(new TransformedImage(image, transformer), null);
        assertSame(image, data.getImage());
    }

    @Test
    public void testGetPointTransformer1() {
        TransformedImageData data = new TransformedImageData(null, null);
        assertNull(data.getPointTransformer());
    }

    @Test
    public void testGetPointTransformer2() {
        ImagePointTransformer transformer = mock(ImagePointTransformer.class);
        TransformedImageData data = new TransformedImageData(new TransformedImage(null, transformer), null);
        assertSame(transformer, data.getPointTransformer());
    }

    @Test
    public void testGetApproxMemorySize1() {
        TransformedImageData data = new TransformedImageData(null, null);
        assertEquals(0L, data.getApproxMemorySize());
    }

    @Test
    public void testGetApproxMemorySize2() {
        BufferedImage image = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImagePointTransformer transformer = mock(ImagePointTransformer.class);
        TransformedImageData data = new TransformedImageData(new TransformedImage(image, transformer), null);
        assertEquals(BufferedImages.getApproxSize(image), data.getApproxMemorySize());
    }
}
