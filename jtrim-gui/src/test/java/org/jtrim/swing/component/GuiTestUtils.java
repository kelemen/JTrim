package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.image.ImageData;
import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class GuiTestUtils {
    private static final int MAX_EVENT_LOOP_COUNT = 100;
    private static final int MIN_EVENT_LOOP_COUNT = 1;

    private static void invokeAfterN(final Runnable task, final int invokeCount) {
        if (invokeCount <= 0) {
            task.run();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                invokeAfterN(task, invokeCount - 1);
            }
        });
    }

    public static void runAfterEvents(final Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }

        final AtomicInteger counter = new AtomicInteger(MAX_EVENT_LOOP_COUNT);
        final WaitableSignal doneSignal = new WaitableSignal();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        Runnable forwardTask = new Runnable() {
            public void executeOrDelay() {
                EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
                if (eventQueue.peekEvent() == null || counter.getAndDecrement() <= 0) {
                    try {
                        task.run();
                    } catch (Throwable ex) {
                        errorRef.set(ex);
                    } finally {
                        doneSignal.signal();
                    }
                }
                else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            executeOrDelay();
                        }
                    });
                }
            }

            @Override
            public void run() {
                executeOrDelay();
            }
        };

        invokeAfterN(forwardTask, MIN_EVENT_LOOP_COUNT);

        doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        Throwable toThrow = errorRef.get();
        if (toThrow != null) {
            ExceptionHelper.rethrow(toThrow);
        }
    }

    public static void runOnEDT(final Runnable task) {
        assert task != null;

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            final WaitableSignal doneSignal = new WaitableSignal();
            final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable ex) {
                        errorRef.set(ex);
                    } finally {
                        doneSignal.signal();
                    }
                }
            });
            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            Throwable error = errorRef.get();
            if (error != null) {
                ExceptionHelper.rethrow(error);
            }
        }
    }

    public static void checkImageContent(BufferedImage image, Color color) {
        int width = image.getWidth();
        int height = image.getHeight();

        int expectedRGB = color.getRGB() | 0xFF00_0000;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (rgb != expectedRGB) {
                    fail("The image is expected to be painted with 0x"
                            + Integer.toHexString(expectedRGB)
                            + " but found pixel: 0x"
                            + Integer.toHexString(rgb));
                }
            }
        }
    }

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

    public static BufferedImage createTestImage(int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    private GuiTestUtils() {
        throw new AssertionError();
    }
}
