package org.jtrim.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class StaticImageDataMethodsTests {
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

    private final StaticImageDataMethods tested;

    public StaticImageDataMethodsTests(StaticImageDataMethods tested) {
        ExceptionHelper.checkNotNullArgument(tested, "tested");
        this.tested = tested;
    }

    public void doAllTests() {
        try {
            executeAllTests(InternalTest.class);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void executeAllTests(Class<? extends Annotation> annotation) throws ReflectiveOperationException {
        Throwable toThrow = null;

        int failureCount = 0;
        for (Method method: StaticImageDataMethodsTests.class.getMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                try {
                    method.invoke(this);
                } catch (InvocationTargetException ex) {
                    failureCount++;
                    if (toThrow == null) toThrow = ex.getCause();
                    else toThrow.addSuppressed(ex.getCause());
                }
            }
        }

        if (toThrow != null) {
            throw new RuntimeException(failureCount + " generic test(s) have failed.", toThrow);
        }
    }

    /**
     * Test of getStoredPixelSize method, of class tested.
     */
    @InternalTest
    public void testGetStoredPixelSize() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            double size = tested.getStoredPixelSize(getColorModelForType(sizeEntry.getKey()));
            assertEquals("Size for buffer type: " + sizeEntry.getKey(),
                    sizeEntry.getValue().doubleValue(), size, 0.001);
        }
    }

    /**
     * Test of getApproxSize method, of class tested.
     */
    @InternalTest
    public void testGetApproxSize() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            int width = 8;
            int height = 9;
            BufferedImage image = new BufferedImage(width, height, sizeEntry.getKey());

            long expectedSize = Math.round(sizeEntry.getValue().doubleValue() * (double)width * (double)height);
            long approxSize = tested.getApproxSize(image);
            assertEquals("Approximate size for buffer: " + sizeEntry.getKey(), expectedSize, approxSize);
        }
    }

    @InternalTest
    public void testGetApproxSizeWithNull() {
        assertEquals(0L, tested.getApproxSize(null));
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
        int receivedType = tested.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                imageType == receivedType || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    private void testGetCompatibleBufferTypeRgb(int imageType) {
        int receivedType = tested.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                isRgbType(receivedType) || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    private void testGetCompatibleBufferTypeAlphaRgb(int imageType) {
        int receivedType = tested.getCompatibleBufferType(getColorModelForType(imageType));
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + receivedType,
                isAlphaRgbType(receivedType) || receivedType == BufferedImage.TYPE_CUSTOM);
    }

    /**
     * Test of getCompatibleBufferType method, of class tested.
     */
    @InternalTest
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
        BufferedImage compatibleBuffer = tested.createCompatibleBuffer(origImage, width, height);
        assertEquals(width, compatibleBuffer.getWidth());
        assertEquals(height, compatibleBuffer.getHeight());

        int compatibleType = compatibleBuffer.getType();
        assertTrue("Compatible buffer for type: " + imageType + ", received type: " + compatibleType,
                imageType == compatibleType);
    }

    /**
     * Test of createCompatibleBuffer method, of class tested.
     */
    @InternalTest
    public void testCreateCompatibleBuffer() {
        for (Map.Entry<Integer, Double> sizeEntry: EXPECTED_PIXEL_SIZES.entrySet()) {
            testCreateCompatibleBuffer(sizeEntry.getKey());
        }
    }

    @InternalTest
    public void testCreateCompatibleBufferForCustom() {
        BufferedImage origImage = BufferedImagesTest.createCustomImage(1, 1);

        int width = 8;
        int height = 9;
        BufferedImage compatibleBuffer = tested.createCompatibleBuffer(origImage, width, height);
        assertEquals(width, compatibleBuffer.getWidth());
        assertEquals(height, compatibleBuffer.getHeight());

        assertEquals(BufferedImage.TYPE_CUSTOM, compatibleBuffer.getType());
    }

    @InternalTest
    public void testCreateCompatibleBufferWithNull() {
        assertNull(tested.createCompatibleBuffer(null, 100, 100));
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

    /**
     * Test of cloneImage method, of class tested.
     */
    @InternalTest
    public void testCloneImage() {
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(tested.cloneImage(createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @InternalTest
    public void testCloneImageWithNull() {
        assertNull(tested.cloneImage(null));
    }

    /**
     * Test of createNewAcceleratedBuffer method, of class tested.
     */
    @InternalTest
    public void testCreateNewAcceleratedBuffer() {
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(tested.createAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @InternalTest
    public void testCreateNewAcceleratedBufferWithNull() {
        assertNull(tested.createNewAcceleratedBuffer(null));
    }

    /**
     * Test of createAcceleratedBuffer method, of class tested.
     */
    @InternalTest
    public void testCreateAcceleratedBuffer() {
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(tested.createNewAcceleratedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @InternalTest
    public void testCreateAcceleratedBufferWithNull() {
        assertNull(tested.createAcceleratedBuffer(null));
    }

    /**
     * Test of createNewOptimizedBuffer method, of class tested.
     */
    @InternalTest
    public void testCreateNewOptimizedBuffer() {
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(tested.createNewOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @InternalTest
    public void testCreateNewOptimizedBufferWithNull() {
        assertNull(tested.createNewOptimizedBuffer(null));
    }

    /**
     * Test of createOptimizedBuffer method, of class tested.
     */
    @InternalTest
    public void testCreateOptimizedBuffer() {
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_3BYTE_BGR)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_4BYTE_ABGR_PRE)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_ARGB_PRE)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_BGR)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_INT_RGB)), 0);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_555_RGB)), 5);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_565_RGB)), 5);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_BYTE_GRAY)), 10);
        checkIfTestImage(tested.createOptimizedBuffer(
                createTestImage(BufferedImage.TYPE_USHORT_GRAY)), 10);
    }

    @InternalTest
    public void testCreateOptimizedBufferWithNull() {
        assertNull(tested.createOptimizedBuffer(null));
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = {ElementType.METHOD})
    public @interface InternalTest {
    }
}
