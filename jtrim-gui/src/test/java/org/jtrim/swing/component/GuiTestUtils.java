package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.image.ImageTestUtils;
import org.jtrim.utils.ExceptionHelper;


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
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                invokeAfterN(task, invokeCount - 1);
            }
        });
    }

    public static void waitAllSwingEvents() {
        runAfterEvents(new Runnable() {
            @Override
            public void run() {
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

    public static BufferedImage createTestCompatibleImage(int width, int height) {
        return ImageTestUtils.createTestCompatibleImage(width, height);
    }

    public static BufferedImage createTestImageWithoutAlpha(int width, int height) {
        return ImageTestUtils.createTestImageWithoutAlpha(width, height);
    }

    public static BufferedImage createTestImage(int width, int height) {
        return ImageTestUtils.createTestImage(width, height);
    }

    public static void equalImages(BufferedImage expected, BufferedImage actual) {
        ImageTestUtils.equalImages(expected, actual);
    }

    public static int getRgbOnImage(BufferedImage image, Color color) {
        return ImageTestUtils.getRgbOnImage(image, color);
    }

    public static void checkBlankImage(BufferedImage image, Color expectedColor) {
        ImageTestUtils.checkBlankImage(image, expectedColor);
    }

    public static void checkNotBlankImage(BufferedImage image) {
        ImageTestUtils.checkNotBlankImage(image);
    }

    public static void checkTestImagePixels(BufferedImage image) {
        ImageTestUtils.checkTestImagePixels(image);
    }

    public static void checkTestImagePixels(String errorMsg, BufferedImage image) {
        ImageTestUtils.checkTestImagePixels(errorMsg, image);
    }

    public static void fillImage(BufferedImage image, Color color) {
        ImageTestUtils.fillImage(image, color);
    }

    private GuiTestUtils() {
        throw new AssertionError();
    }
}
