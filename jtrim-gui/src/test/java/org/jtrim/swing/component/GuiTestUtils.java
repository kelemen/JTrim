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
import org.jtrim.utils.ExceptionHelper;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class GuiTestUtils {
    private static final int MAX_EVENT_LOOP_COUNT = 100;
    private static final int MIN_EVENT_LOOP_COUNT = 5;

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

    public static void checkTestImagePixels(BufferedImage image) {
        checkTestImagePixels("Incorrect test image pixels.", image);
    }

    public static void checkTestImagePixels(String errorMsg, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] expected = new int[width * height];
        fillPixels(expected);

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            DataBuffer dataBuffer = image.getRaster().getDataBuffer();
            if (dataBuffer.getNumBanks() == 1 && dataBuffer instanceof DataBufferInt) {
                assertArrayEquals(errorMsg, expected, ((DataBufferInt)(dataBuffer)).getData());
                return;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (rgb != expected[x + width * y]) {
                    fail("Pixel mismatch at (" + x + ", " + y + ")");
                }
            }
        }
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
