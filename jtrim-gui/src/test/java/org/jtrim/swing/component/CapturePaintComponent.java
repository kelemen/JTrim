package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import org.jtrim.image.ImageData;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings(value = "serial")
public final class CapturePaintComponent extends JComponent {
    private BufferedImage image;
    private Component child;
    private final AtomicInteger numberOfPaints;

    public CapturePaintComponent() {
        super();
        image = null;
        numberOfPaints = new AtomicInteger(0);
        super.setLayout(new GridLayout(1, 1, 0, 0));
    }

    public BufferedImage getChildContent() {
        return image;
    }

    @Override
    public void setLayout(LayoutManager mgr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component add(Component comp) {
        throw new UnsupportedOperationException("Use setChild instead.");
    }

    @Override
    public Component add(String name, Component comp) {
        throw new UnsupportedOperationException("Use setChild instead.");
    }

    @Override
    public Component add(Component comp, int index) {
        throw new UnsupportedOperationException("Use setChild instead.");
    }

    @Override
    public void add(Component comp, Object constraints) {
        throw new UnsupportedOperationException("Use setChild instead.");
    }

    @Override
    public void add(Component comp, Object constraints, int index) {
        throw new UnsupportedOperationException("Use setChild instead.");
    }

    public void setChild(Component child) {
        assert child != null;
        this.child = child;
        removeAll();
        super.add(child);
    }

    public int getNumberOfPaints() {
        return numberOfPaints.get();
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    private void paintDefault(Graphics g) {
        Color prevColor = g.getColor();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(prevColor);
    }

    @Override
    public void paint(Graphics g) {
        if (child == null) {
            paintDefault(g);
            return;
        }
        int childWidth = child.getWidth();
        int childHeight = child.getHeight();
        if (childWidth <= 0 || childHeight <= 0) {
            paintDefault(g);
            return;
        }
        if (image == null || image.getWidth() != childWidth || image.getHeight() != childHeight) {
            int bufferType = ImageData.getCompatibleBufferType(child.getColorModel());
            if (bufferType == BufferedImage.TYPE_CUSTOM) {
                bufferType = BufferedImage.TYPE_INT_ARGB;
            }
            image = new BufferedImage(childWidth, childHeight, bufferType);
        }
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            numberOfPaints.incrementAndGet();
            child.paint(g2d);
        } finally {
            g2d.dispose();
        }
        g.drawImage(image, 0, 0, null);
    }

}
