package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial")
public abstract class Graphics2DComponent extends JComponent {
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private BufferedImage fallbackImage;

    public Graphics2DComponent() {
        this.fallbackImage = null;
    }

    protected abstract void paintComponent2D(Graphics2D g);

    @Override
    protected final void paintComponent(Graphics g) {
        int currentWidth = getWidth();
        int currentHeight = getHeight();

        Graphics scratchGraphics = null;
        Graphics2D g2d = null;
        boolean useBufferedImage = false;

        try {
            if (g instanceof Graphics2D) {
                scratchGraphics = g.create();
                if (scratchGraphics instanceof Graphics2D) {
                    g2d = (Graphics2D)scratchGraphics;
                }
                else {
                    scratchGraphics.dispose();
                    scratchGraphics = null;
                }
            }

            if (g2d == null) {
                useBufferedImage = true;
                if (fallbackImage == null ||
                        fallbackImage.getWidth() != currentWidth ||
                        fallbackImage.getHeight() != currentHeight) {

                    fallbackImage = new BufferedImage(currentWidth, currentHeight,
                            BufferedImage.TYPE_INT_ARGB);
                }
                g2d = fallbackImage.createGraphics();
                scratchGraphics = g2d;
                g2d.setColor(TRANSPARENT_COLOR);
                g2d.fillRect(0, 0, currentWidth, currentHeight);
            }

            paintComponent2D(g2d);
        } finally {
            if (scratchGraphics != null) {
                scratchGraphics.dispose();
            }
            if (useBufferedImage) {
                g.drawImage(fallbackImage, 0, 0, null);
            }
        }
    }
}
