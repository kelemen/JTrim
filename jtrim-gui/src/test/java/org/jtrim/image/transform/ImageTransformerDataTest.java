package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.jtrim.image.ImageMetaData;
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
@SuppressWarnings("deprecation")
public class ImageTransformerDataTest {
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

    private static ImageTransformerData create(
            BufferedImage sourceImage,
            int destWidth,
            int destHeight,
            ImageMetaData metaData) {
        return new ImageTransformerData(sourceImage, destWidth, destHeight, metaData);
    }

    @Test
    public void testSimpleProperties() {
        for (int destWidth: Arrays.asList(0, 1, 35)) {
            for (int destHeight: Arrays.asList(0, 1, 43)) {
                int srcWidth = 7;
                int srcHeight = 8;
                BufferedImage srcImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
                ImageMetaData metaData = new ImageMetaData(srcWidth, srcHeight, true);
                ImageTransformerData data = create(srcImage, destWidth, destHeight, metaData);

                assertSame(srcImage, data.getSourceImage());
                assertSame(metaData, data.getMetaData());
                assertEquals(destWidth, data.getDestWidth());
                assertEquals(destHeight, data.getDestHeight());
                assertEquals(srcWidth, data.getSrcWidth());
                assertEquals(srcHeight, data.getSrcHeight());
                assertEquals(srcWidth, data.getImageWidth());
                assertEquals(srcHeight, data.getImageHeight());
            }
        }
    }

    @Test
    public void testPropertiesNullSrc() {
        for (int destWidth: Arrays.asList(0, 1, 35)) {
            for (int destHeight: Arrays.asList(0, 1, 43)) {
                int srcWidth = 7;
                int srcHeight = 8;

                ImageMetaData metaData = new ImageMetaData(srcWidth, srcHeight, true);
                ImageTransformerData data = create(null, destWidth, destHeight, metaData);

                assertNull(data.getSourceImage());
                assertSame(metaData, data.getMetaData());
                assertEquals(destWidth, data.getDestWidth());
                assertEquals(destHeight, data.getDestHeight());
                assertEquals(0, data.getSrcWidth());
                assertEquals(0, data.getSrcHeight());
                assertEquals(srcWidth, data.getImageWidth());
                assertEquals(srcHeight, data.getImageHeight());
            }
        }
    }

    @Test
    public void testPropertiesNullSrcNullMetaData() {
        for (int destWidth: Arrays.asList(0, 1, 35)) {
            for (int destHeight: Arrays.asList(0, 1, 43)) {
                ImageTransformerData data = create(null, destWidth, destHeight, null);

                assertNull(data.getSourceImage());
                assertNull(data.getMetaData());
                assertEquals(destWidth, data.getDestWidth());
                assertEquals(destHeight, data.getDestHeight());
                assertEquals(0, data.getSrcWidth());
                assertEquals(0, data.getSrcHeight());
                assertEquals(-1, data.getImageWidth());
                assertEquals(-1, data.getImageHeight());
            }
        }
    }

    @Test
    public void testInconsistentMetaDataWidth() {
        BufferedImage srcImage = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImageMetaData metaData = new ImageMetaData(1, 8, true);

        ImageTransformerData data = create(srcImage, 1, 1, metaData);
        assertEquals(7, data.getSrcWidth());
        assertEquals(8, data.getSrcHeight());
        assertEquals(7, data.getImageWidth());
        assertEquals(8, data.getImageHeight());
    }

    @Test
    public void testInconsistentMetaDataHeight() {
        BufferedImage srcImage = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImageMetaData metaData = new ImageMetaData(7, 1, true);

        ImageTransformerData data = create(srcImage, 1, 1, metaData);
        assertEquals(7, data.getSrcWidth());
        assertEquals(8, data.getSrcHeight());
        assertEquals(7, data.getImageWidth());
        assertEquals(8, data.getImageHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalDestWidth() {
        BufferedImage srcImage = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImageMetaData metaData = new ImageMetaData(srcImage.getWidth(), srcImage.getHeight(), true);
        create(srcImage, -1, 1, metaData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalDestHeight() {
        BufferedImage srcImage = new BufferedImage(7, 8, BufferedImage.TYPE_BYTE_GRAY);
        ImageMetaData metaData = new ImageMetaData(srcImage.getWidth(), srcImage.getHeight(), true);
        create(srcImage, 1, -1, metaData);
    }
}
