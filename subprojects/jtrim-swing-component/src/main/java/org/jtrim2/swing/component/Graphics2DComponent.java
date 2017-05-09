package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * Defines a convenient base class for user drawn <I>Swing</I> components. This
 * class defines an abstract {@link #paintComponent2D(Graphics2D) paintComponent2D}
 * in place of the inherited {@link #paintComponent(Graphics) paintComponent}
 * method. The main difference between these methods is that
 * {@code paintComponent2D} takes a {@code Graphics2D} object rather than a
 * {@code Graphics}.
 * <P>
 * The thread-safety property of this component is the same as with any other
 * <I>Swing</I> components. That is, instances of this class can be accessed
 * only from the AWT Event Dispatch Thread after made displayable.
 */
@SuppressWarnings("serial")
public abstract class Graphics2DComponent extends JComponent {
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private BufferedImage fallbackImage;

    /**
     * Initializes this Graphics2DComponent.
     */
    public Graphics2DComponent() {
        this.fallbackImage = null;
    }

    /**
     * Subclasses must override this method to paint this component.
     * Implementations must honor the {@code opaque} property of this component
     * but does not need to preserve the context of the passed
     * {@code Graphics2D} object (unlike with {@code paintComponent}).
     *
     * @param g the {@code Graphics2D} to paint to. This argument cannot be
     *   {@code null}.
     */
    protected abstract void paintComponent2D(Graphics2D g);

    /**
     * Invokes {@link #paintComponent2D(Graphics2D) paintComponent2D} with a
     * {@code Graphics2D} object to draw on.
     * <P>
     * In case a {@code Graphics2D} object is passed to this method a copy of
     * the context (via {@code Graphics2D.create()}) will be created and passed
     * to {@code paintComponent2D}, otherwise a {@code BufferedImage} of the
     * type {@code BufferedImage.TYPE_INT_ARGB} will be created to draw on and
     * its {@code Graphics2D} will be passed to {@code paintComponent2D}.
     * <P>
     * This method cannot be overridden, override {@code paintComponent2D}
     * instead.
     *
     * @param g the {@code Graphics} to paint to. This argument cannot be
     *   {@code null}.
     */
    @Override
    protected final void paintComponent(Graphics g) {
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
                int currentWidth = getWidth();
                int currentHeight = getHeight();
                if (currentWidth <= 0 || currentHeight <= 0) {
                    return;
                }

                useBufferedImage = true;
                if (fallbackImage == null
                        || fallbackImage.getWidth() != currentWidth
                        || fallbackImage.getHeight() != currentHeight) {

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
