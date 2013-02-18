package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import org.jtrim.swing.DelegateGraphics;
import org.jtrim.swing.DelegateGraphics2D;
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
public class Graphics2DComponentTest {
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
        int width = image.getWidth();
        int height = image.getHeight();

        int[] expected = new int[width * height];
        fillPixels(expected);

        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            DataBuffer dataBuffer = image.getRaster().getDataBuffer();
            if (dataBuffer.getNumBanks() == 1 && dataBuffer instanceof DataBufferInt) {
                assertArrayEquals(expected, ((DataBufferInt)(dataBuffer)).getData());
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

    @Test
    public void testPaintWithGraphics2D() {
        int width = 7;
        int height = 8;

        Graphics2DComponent component = new StaticGraphics2DComponent(width, height);

        BufferedImage resultImage = createTestCompatibleImage(width, height);
        Graphics2D g2d = resultImage.createGraphics();
        try {
            component.paintComponent(g2d);
        } finally {
            g2d.dispose();
        }

        checkTestImagePixels(resultImage);
    }

    @Test
    public void testPaintWithGraphics() {
        int width = 7;
        int height = 8;

        Graphics2DComponent component = new StaticGraphics2DComponent(width, height);

        BufferedImage resultImage = createTestCompatibleImage(width, height);
        Graphics g = new DelegateGraphics(resultImage.createGraphics());
        try {
            component.paintComponent(g);
        } finally {
            g.dispose();
        }

        checkTestImagePixels(resultImage);
    }

    @Test
    public void testPaintWithGraphicsErrorInPaint() {
        int width = 7;
        int height = 8;

        Graphics2DComponent component = new NoOpGraphics2DComponent(width, height) {
            private static final long serialVersionUID = -5977753446784477761L;

            @Override
            public void paintComponent2D(Graphics2D g) {
                throw new TestException();
            }
        };

        BufferedImage resultImage = createTestCompatibleImage(width, height);
        Graphics g = new DelegateGraphics(resultImage.createGraphics());
        try {
            try {
                component.paintComponent(g);
                fail("Expected: TestException.");
            } catch (TestException ex) {
            }
        } finally {
            g.dispose();
        }
    }

    @Test
    public void testPaintWithGraphicsChangeDimension() {
        int width = 7;
        int height = 8;

        Graphics2DComponent component = new StaticGraphics2DComponent(width, height);

        BufferedImage resultImage = createTestCompatibleImage(width, height);
        Graphics g = new DelegateGraphics(resultImage.createGraphics());
        try {
            component.setSize(width + 1, height);
            component.paintComponent(g);
            component.setSize(width, height + 1);
            component.paintComponent(g);
            component.setSize(width, height);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            component.paintComponent(g);
        } finally {
            g.dispose();
        }

        checkTestImagePixels(resultImage);
    }

    @Test
    public void testPaintWithGraphics2DCreatingGraphics() {
        int width = 7;
        int height = 8;

        Graphics2DComponent component = new StaticGraphics2DComponent(width, height);

        BufferedImage resultImage = createTestCompatibleImage(width, height);
        Graphics g = new DelegateGraphics2D(resultImage.createGraphics()) {
            @Override
            public Graphics create() {
                return new DelegateGraphics(wrapped.create());
            }

            @Override
            public Graphics create(int x, int y, int width, int height) {
                return new DelegateGraphics(wrapped.create(x, y, width, height));
            }
        };
        try {
            component.paintComponent(g);
        } finally {
            g.dispose();
        }

        checkTestImagePixels(resultImage);
    }

    @Test
    public void testZeroWidth() {
        Graphics2DComponent component = spy(new NoOpGraphics2DComponent(0, 10));

        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = new DelegateGraphics(image.createGraphics());
        try {
            component.paintComponent(g);
        } finally {
            g.dispose();
        }

        verify(component, never()).paintComponent2D(any(Graphics2D.class));
    }

    @Test
    public void testZeroHeight() {
        Graphics2DComponent component = spy(new NoOpGraphics2DComponent(10, 0));

        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics g = new DelegateGraphics(image.createGraphics());
        try {
            component.paintComponent(g);
        } finally {
            g.dispose();
        }

        verify(component, never()).paintComponent2D(any(Graphics2D.class));
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -1970810071945778607L;
    }

    @SuppressWarnings("serial")
    private static final class StaticGraphics2DComponent extends Graphics2DComponent {
        private final BufferedImage toDraw;

        public StaticGraphics2DComponent(int width, int height) {
            this.toDraw = createTestImage(width, height);
            setSize(width, height);
        }

        @Override
        public void paintComponent2D(Graphics2D g) {
            g.drawImage(toDraw, null, 0, 0);
        }
    }

    @SuppressWarnings("serial")
    private static class NoOpGraphics2DComponent extends Graphics2DComponent {
        public NoOpGraphics2DComponent(int width, int height) {
            setSize(width, height);
        }

        @Override
        public void paintComponent2D(Graphics2D g) {
        }
    }
}