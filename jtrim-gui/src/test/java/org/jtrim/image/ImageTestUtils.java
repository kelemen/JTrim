package org.jtrim.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class ImageTestUtils {
    private static void fillPixels(int[] pixelArray) {
        for (int i = 0; i < pixelArray.length; i++) {
            int red = i % 256;
            int green = (i * 3) % 256;
            int blue = (i * 7) % 256;

            pixelArray[i] = blue | (green << 8) | (red << 16) | 0xFF00_0000;
        }
    }

    public static BufferedImage createTestCompatibleImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage createTestImageWithoutAlpha(int width, int height) {
        return createTestImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage createTestImage(int width, int height) {
        return createTestImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public static BufferedImage createTestImage(int width, int height, int bufferType) {
        BufferedImage result = new BufferedImage(width, height, bufferType);
        DataBuffer dataBuffer = result.getRaster().getDataBuffer();

        if (dataBuffer.getNumBanks() == 1 && dataBuffer instanceof DataBufferInt) {
            fillPixels(((DataBufferInt)(dataBuffer)).getData());
        }
        else {
            int[] pixels = new int[width * height];
            fillPixels(pixels);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result.setRGB(x, y, pixels[x + width * y]);
                }
            }
        }
        return result;
    }

    public static void equalImages(BufferedImage expected, BufferedImage actual) {
        int width = expected.getWidth();
        int height = expected.getHeight();

        assertEquals("Different width of images. ", width, actual.getWidth());
        assertEquals("Different height of images. ", height, actual.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int actualRgb = actual.getRGB(x, y);
                int expectedRgb = expected.getRGB(x, y);

                if (actualRgb != expectedRgb) {
                    fail("Pixel mismatch at (" + x + ", " + y + "). "
                            + "Actual: 0x" + Integer.toHexString(actualRgb) + ", "
                            + "Expected: 0x" + Integer.toHexString(expectedRgb));
                }
            }
        }
    }

    public static int getRgbOnImage(BufferedImage image, Color color) {
        int imageType = image.getType();
        if (imageType == BufferedImage.TYPE_4BYTE_ABGR
                || imageType == BufferedImage.TYPE_INT_ARGB) {
            return color.getRGB();
        }

        BufferedImage pixelImage = imageType != BufferedImage.TYPE_CUSTOM
                ? new BufferedImage(1, 1, imageType)
                : ImageData.createCompatibleBuffer(image, 1, 1);
        pixelImage.setRGB(0, 0, color.getRGB());
        return pixelImage.getRGB(0, 0);
    }

    public static void checkBlankImage(BufferedImage image, Color expectedColor) {
        int expectedRgb = getRgbOnImage(image, expectedColor);
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int actualRgb = image.getRGB(x, y);

                if (actualRgb != expectedRgb) {
                    fail("Expected all pixels to be: 0x" + Integer.toHexString(expectedRgb)
                            + " but found pixel at (" + x + ", " + y + "): 0x"
                            + Integer.toHexString(actualRgb));
                }
            }
        }
    }

    public static void checkNotBlankImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean blank = true;
        int lastColor = image.getRGB(0, 0);

        outerLoop:
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    if (rgb != lastColor) {
                        blank = false;
                        break outerLoop;
                    }
                    lastColor = rgb;
                }
            }

        if (blank) {
            fail("The image does not expected to be a blank image but it only "
                    + "contains the color 0x" + Integer.toHexString(lastColor));
        }
    }

    public static void checkTestImagePixels(BufferedImage image) {
        checkTestImagePixels("Incorrect test image pixels.", image);
    }

    public static void checkTestImagePixels(String errorMsg, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            DataBuffer dataBuffer = image.getRaster().getDataBuffer();
            if (dataBuffer.getNumBanks() == 1 && dataBuffer instanceof DataBufferInt) {
                int[] expected = new int[width * height];
                fillPixels(expected);
                assertArrayEquals(errorMsg, expected, ((DataBufferInt)(dataBuffer)).getData());
                return;
            }
        }

        int imageType = image.getType();
        BufferedImage expected = imageType != BufferedImage.TYPE_CUSTOM
                ? new BufferedImage(width, height, imageType)
                : ImageData.cloneImage(image);
        BufferedImage testImage = createTestImage(width, height);
        Graphics2D g2d = expected.createGraphics();
        try {
            g2d.drawImage(testImage, null, 0, 0);
        } finally {
            g2d.dispose();
        }

        equalImages(expected, image);
    }

    public static void fillImage(BufferedImage image, Color color) {
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(color);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    private ImageTestUtils() {
        throw new AssertionError();
    }
}
