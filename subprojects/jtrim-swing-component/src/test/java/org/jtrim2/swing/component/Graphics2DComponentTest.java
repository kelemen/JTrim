package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.jtrim2.testutils.swing.DelegateGraphics;
import org.jtrim2.testutils.swing.DelegateGraphics2D;
import org.junit.Test;

import static org.jtrim2.testutils.swing.component.GuiTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Graphics2DComponentTest {
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
