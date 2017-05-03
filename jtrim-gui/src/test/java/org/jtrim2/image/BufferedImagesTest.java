package org.jtrim2.image;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import org.jtrim2.gui.TestUtils;
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
public class BufferedImagesTest {
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
    public void testUtilityClass() {
        TestUtils.testUtilityClass(BufferedImages.class);
    }

    @Test
    public void testMethodsCommonWithImageData() {
        StaticImageDataMethodsTests tests = new StaticImageDataMethodsTests(new StaticImageDataMethods() {
            @Override
            public double getStoredPixelSize(ColorModel cm) {
                return BufferedImages.getStoredPixelSize(cm);
            }

            @Override
            public long getApproxSize(BufferedImage image) {
                return BufferedImages.getApproxSize(image);
            }

            @Override
            public int getCompatibleBufferType(ColorModel colorModel) {
                return BufferedImages.getCompatibleBufferType(colorModel);
            }

            @Override
            public BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height) {
                return BufferedImages.createCompatibleBuffer(image, width, height);
            }

            @Override
            public BufferedImage cloneImage(BufferedImage image) {
                return BufferedImages.cloneImage(image);
            }

            @Override
            public BufferedImage createNewAcceleratedBuffer(BufferedImage image) {
                return BufferedImages.createNewAcceleratedBuffer(image);
            }

            @Override
            public BufferedImage createAcceleratedBuffer(BufferedImage image) {
                return BufferedImages.createAcceleratedBuffer(image);
            }

            @Override
            public BufferedImage createNewOptimizedBuffer(BufferedImage image) {
                return BufferedImages.createNewOptimizedBuffer(image);
            }

            @Override
            public BufferedImage createOptimizedBuffer(BufferedImage image) {
                return BufferedImages.createOptimizedBuffer(image);
            }
        });
        tests.doAllTests();
    }

    public static BufferedImage createCustomImage(int width, int height) {
        ColorSpace colorSpace = mock(ColorSpace.class);
        ColorModel colorModel = new ComponentColorModel(
                colorSpace, true, true, ColorModel.TRANSLUCENT, DataBuffer.TYPE_DOUBLE);
        WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);

        return new BufferedImage(colorModel, raster, true, null);
    }

    @Test
    public void testAreCompatibleBuffersDifferentWidth() {
        BufferedImage image1 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(25, 30, BufferedImage.TYPE_INT_RGB);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersDifferentHeight() {
        BufferedImage image1 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(20, 35, BufferedImage.TYPE_INT_RGB);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersDifferentType() {
        BufferedImage image1 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = new BufferedImage(20, 30, BufferedImage.TYPE_BYTE_GRAY);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCustom1() {
        BufferedImage image1 = createCustomImage(20, 30);
        BufferedImage image2 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCustom2() {
        BufferedImage image1 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = createCustomImage(20, 30);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCustomBoth() {
        BufferedImage image1 = createCustomImage(20, 30);
        BufferedImage image2 = createCustomImage(20, 30);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCompatible() {
        int[] types = new int[]{
            BufferedImage.TYPE_3BYTE_BGR,
            BufferedImage.TYPE_4BYTE_ABGR,
            BufferedImage.TYPE_4BYTE_ABGR_PRE,
            BufferedImage.TYPE_BYTE_BINARY,
            BufferedImage.TYPE_BYTE_GRAY,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_INT_ARGB_PRE,
            BufferedImage.TYPE_INT_BGR,
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_USHORT_555_RGB,
            BufferedImage.TYPE_USHORT_565_RGB,
            BufferedImage.TYPE_USHORT_GRAY
        };

        for (int type: types) {
            BufferedImage image1 = new BufferedImage(20, 30, type);
            BufferedImage image2 = new BufferedImage(20, 30, type);
            assertTrue(BufferedImages.areCompatibleBuffers(image1, image2));
        }
    }
}
