package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim.image.ImageData;
import org.jtrim.image.ImageReceiveException;
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
            for (ImageReceiveException exception: Arrays.asList(null, new ImageReceiveException())) {
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
        assertEquals(ImageData.getApproxSize(image), data.getApproxMemorySize());
    }
}
