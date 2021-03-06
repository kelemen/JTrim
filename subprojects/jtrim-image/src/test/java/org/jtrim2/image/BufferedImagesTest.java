package org.jtrim2.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Map;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.image.ImageTestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class BufferedImagesTest {
    private static final Map<Integer, Double> EXPECTED_PIXEL_SIZES;
    private static final int TEST_IMG_WIDTH = 8;
    private static final int TEST_IMG_HEIGHT = 9;
    private static final int[][] TEST_PIXELS;

    static {
        TEST_PIXELS = new int[TEST_IMG_HEIGHT][TEST_IMG_WIDTH];
        int pos = 0;
        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            @SuppressWarnings("MismatchedReadAndWriteOfArray")
            int[] line = TEST_PIXELS[y];

            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                int gray = (0xFF * pos) / (TEST_IMG_WIDTH * TEST_IMG_HEIGHT);
                line[x] = gray | (gray << 8) | (gray << 16) | 0xFF00_0000;
                pos++;
            }
        }

        EXPECTED_PIXEL_SIZES = CollectionsEx.newHashMap(32);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_3BYTE_BGR, 3.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_4BYTE_ABGR, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_4BYTE_ABGR_PRE, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_BYTE_BINARY, 1.0 / 8.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_BYTE_GRAY, 1.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_BYTE_INDEXED, 1.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_INT_ARGB, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_INT_ARGB_PRE, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_INT_BGR, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_INT_RGB, 4.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_USHORT_555_RGB, 2.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_USHORT_565_RGB, 2.0);
        EXPECTED_PIXEL_SIZES.put(BufferedImage.TYPE_USHORT_GRAY, 2.0);
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(BufferedImages.class);
    }

    @Test
    public void testGetStoredPixelSize() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            double size = BufferedImages.getStoredPixelSize(getColorModelForType(sizeEntry.getKey()));
            assertEquals("Size for buffer type: " + sizeEntry.getKey(),
                    sizeEntry.getValue(), size, 0.001);
        }
    }

    @Test
    public void testGetApproxSize() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            int width = 8;
            int height = 9;
            BufferedImage image = new BufferedImage(width, height, sizeEntry.getKey());

            long expectedSize = Math.round(sizeEntry.getValue() * (double) width * (double) height);
            long approxSize = BufferedImages.getApproxSize(image);
            assertEquals("Approximate size for buffer: " + sizeEntry.getKey(), expectedSize, approxSize);
        }
    }

    @Test
    public void testGetApproxSizeWithNull() {
        assertEquals(0L, BufferedImages.getApproxSize(null));
    }

    private static ColorModel getColorModelForType(int imageType) {
        BufferedImage image = new BufferedImage(1, 1, imageType);
        return image.getColorModel();
    }

    private static boolean isNoAlphaRgbType(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
                return true;
            default:
                return false;
        }
    }

    private static boolean isAlphaRgbType(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return true;
            default:
                return false;
        }
    }

    private boolean isRgbType(int imageType) {
        return isAlphaRgbType(imageType) || isNoAlphaRgbType(imageType);
    }

    private void testGetCompatibleBufferTypeExact(int imageType) {
        int receivedType = BufferedImages.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                imageType == receivedType || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    private void testGetCompatibleBufferTypeRgb(int imageType) {
        int receivedType = BufferedImages.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                isRgbType(receivedType) || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    private void testGetCompatibleBufferTypeAlphaRgb(int imageType) {
        int receivedType = BufferedImages.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                isAlphaRgbType(receivedType) || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    @Test
    public void testGetCompatibleBufferType() {
        testGetCompatibleBufferTypeRgb(BufferedImage.TYPE_3BYTE_BGR);
        testGetCompatibleBufferTypeAlphaRgb(BufferedImage.TYPE_4BYTE_ABGR);
        testGetCompatibleBufferTypeAlphaRgb(BufferedImage.TYPE_4BYTE_ABGR_PRE);
        testGetCompatibleBufferTypeAlphaRgb(BufferedImage.TYPE_INT_ARGB);
        testGetCompatibleBufferTypeAlphaRgb(BufferedImage.TYPE_INT_ARGB_PRE);
        testGetCompatibleBufferTypeRgb(BufferedImage.TYPE_INT_BGR);
        testGetCompatibleBufferTypeRgb(BufferedImage.TYPE_INT_RGB);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_BYTE_BINARY);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_BYTE_GRAY);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_BYTE_INDEXED);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_USHORT_555_RGB);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_USHORT_565_RGB);
        testGetCompatibleBufferTypeExact(BufferedImage.TYPE_USHORT_GRAY);
    }

    private void testCreateCompatibleBuffer(int imageType) {
        BufferedImage origImage = new BufferedImage(1, 1, imageType);

        int width = 8;
        int height = 9;
        BufferedImage compatibleBuffer = BufferedImages.createCompatibleBuffer(origImage, width, height);
        assertEquals(width, compatibleBuffer.getWidth());
        assertEquals(height, compatibleBuffer.getHeight());

        int compatibleType = compatibleBuffer.getType();
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + compatibleType,
                imageType == compatibleType);
    }

    @Test
    public void testCreateCompatibleBuffer() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            testCreateCompatibleBuffer(sizeEntry.getKey());
        }
    }

    @Test
    public void testCreateCompatibleBufferForCustom() {
        BufferedImage origImage = ImageTestUtils.createCustomImage(1, 1);

        int width = 8;
        int height = 9;
        BufferedImage compatibleBuffer = BufferedImages.createCompatibleBuffer(origImage, width, height);
        assertEquals(width, compatibleBuffer.getWidth());
        assertEquals(height, compatibleBuffer.getHeight());

        assertEquals(BufferedImage.TYPE_CUSTOM, compatibleBuffer.getType());
    }

    @Test
    public void testCreateCompatibleBufferWithNull() {
        assertNull(BufferedImages.createCompatibleBuffer(null, 100, 100));
    }

    private static BufferedImage createTestImage(int imageType) {
        BufferedImage bufferedImage = new BufferedImage(TEST_IMG_WIDTH, TEST_IMG_HEIGHT, imageType);
        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                bufferedImage.setRGB(x, y, TEST_PIXELS[y][x]);
            }
        }
        return bufferedImage;
    }

    private static boolean rgbEquals(int rgb1, int rgb2, int tolerancePerComponent) {
        for (int i = 0; i < 4; i++) {
            int bitOffset = 8 * i;
            int mask = 0xFF << bitOffset;
            int c1 = (rgb1 & mask) >>> bitOffset;
            int c2 = (rgb2 & mask) >>> bitOffset;
            if (Math.abs(c1 - c2) > tolerancePerComponent) {
                return false;
            }
        }
        return true;
    }

    private static void checkIfTestImage(BufferedImage image, int tolerancePerComponent) {
        assertEquals(TEST_IMG_WIDTH, image.getWidth());
        assertEquals(TEST_IMG_HEIGHT, image.getHeight());

        for (int y = 0; y < TEST_IMG_HEIGHT; y++) {
            for (int x = 0; x < TEST_IMG_WIDTH; x++) {
                int rgb = image.getRGB(x, y);
                int expected = TEST_PIXELS[y][x];
                assertTrue("Pixels must match. Expected: "
                        + Integer.toHexString(expected)
                        + ". Received: "
                        + Integer.toHexString(rgb),
                        rgbEquals(rgb, expected, tolerancePerComponent));
            }
        }
    }

    @Test
    public void testCloneImage() {
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(BufferedImages.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @Test
    public void testCloneImageWithNull() {
        assertNull(BufferedImages.cloneImage(null));
    }

    @Test
    public void testCreateNewAcceleratedBuffer() {
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(BufferedImages.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @Test
    public void testCreateNewAcceleratedBufferWithNull() {
        assertNull(BufferedImages.createNewAcceleratedBuffer(null));
    }

    @Test
    public void testCreateAcceleratedBuffer() {
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(BufferedImages.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @Test
    public void testCreateAcceleratedBufferWithNull() {
        assertNull(BufferedImages.createAcceleratedBuffer(null));
    }

    @Test
    public void testCreateNewOptimizedBuffer() {
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(BufferedImages.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @Test
    public void testCreateNewOptimizedBufferWithNull() {
        assertNull(BufferedImages.createNewOptimizedBuffer(null));
    }

    @Test
    public void testCreateOptimizedBuffer() {
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(BufferedImages.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @Test
    public void testCreateOptimizedBufferWithNull() {
        assertNull(BufferedImages.createOptimizedBuffer(null));
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
        BufferedImage image1 = ImageTestUtils.createCustomImage(20, 30);
        BufferedImage image2 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCustom2() {
        BufferedImage image1 = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        BufferedImage image2 = ImageTestUtils.createCustomImage(20, 30);
        assertFalse(BufferedImages.areCompatibleBuffers(image1, image2));
    }

    @Test
    public void testAreCompatibleBuffersCustomBoth() {
        BufferedImage image1 = ImageTestUtils.createCustomImage(20, 30);
        BufferedImage image2 = ImageTestUtils.createCustomImage(20, 30);
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
