package org.jtrim.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


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
}
