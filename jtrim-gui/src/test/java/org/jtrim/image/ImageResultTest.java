package org.jtrim.image;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class ImageResultTest {
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

    private static ImageResult create(BufferedImage image, ImageMetaData metaData) {
        return new ImageResult(image, metaData);
    }

    @Test
    public void testProperties() {
        int width = 8;
        int height = 9;
        for (BufferedImage image: Arrays.asList(null, createRgbImage(width, height))) {
            for (ImageMetaData metaData: Arrays.asList(null, new ImageMetaData(width, height, true))) {
                ImageResult data = create(image, metaData);
                assertSame(image, data.getImage());
                assertSame(metaData, data.getMetaData());
                assertEquals(BufferedImages.getApproxSize(image), data.getApproxMemorySize());

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

    @Test(expected = IllegalArgumentException.class)
    public void testInconsistentWidth() {
        create(createRgbImage(2, 3), new ImageMetaData(1, 3, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInconsistentHeight() {
        create(createRgbImage(2, 3), new ImageMetaData(2, 4, true));
    }
}
