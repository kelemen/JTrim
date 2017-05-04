package org.jtrim2.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;
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
public class ImageDataTest {
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

    private static BufferedImage createRgbImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testStaticMethods() {
        StaticImageDataMethodsTests tests = new StaticImageDataMethodsTests(new StaticImageDataMethods() {
            @Override
            public double getStoredPixelSize(ColorModel cm) {
                return ImageData.getStoredPixelSize(cm);
            }

            @Override
            public long getApproxSize(BufferedImage image) {
                return ImageData.getApproxSize(image);
            }

            @Override
            public int getCompatibleBufferType(ColorModel colorModel) {
                return ImageData.getCompatibleBufferType(colorModel);
            }

            @Override
            public BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height) {
                return ImageData.createCompatibleBuffer(image, width, height);
            }

            @Override
            public BufferedImage cloneImage(BufferedImage image) {
                return ImageData.cloneImage(image);
            }

            @Override
            public BufferedImage createNewAcceleratedBuffer(BufferedImage image) {
                return ImageData.createNewAcceleratedBuffer(image);
            }

            @Override
            public BufferedImage createAcceleratedBuffer(BufferedImage image) {
                return ImageData.createAcceleratedBuffer(image);
            }

            @Override
            public BufferedImage createNewOptimizedBuffer(BufferedImage image) {
                return ImageData.createNewOptimizedBuffer(image);
            }

            @Override
            public BufferedImage createOptimizedBuffer(BufferedImage image) {
                return ImageData.createOptimizedBuffer(image);
            }
        });
        tests.doAllTests();
    }

    /**
     * Test of properties of a newly created ImageData.
     */
    @Test
    public void testProperties() {
        int width = 8;
        int height = 9;
        for (BufferedImage image: Arrays.asList(null, createRgbImage(width, height))) {
            for (ImageMetaData metaData: Arrays.asList(null, new ImageMetaData(width, height, true))) {
                for (ImageReceiveException exception: Arrays.asList(null, mock(ImageReceiveException.class))) {
                    ImageData data = new ImageData(image, metaData, exception);
                    assertSame(image, data.getImage());
                    assertSame(metaData, data.getMetaData());
                    assertSame(exception, data.getException());
                    assertEquals(ImageData.getApproxSize(image), data.getApproxMemorySize());

                    if (image != null || metaData != null) {
                        assertEquals(width, data.getWidth());
                        assertEquals(height, data.getHeight());
                    }
                    else {
                        assertEquals(-1, data.getWidth());
                        assertEquals(-1, data.getHeight());
                    }
                }
            }
        }
    }
}
