/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Kelemen Attila
 */
public interface DrawingConnector {
    public void setRequiredWidth(int width, int height);

    public boolean hasImage();
    public GraphicsCopyResult copyMostRecentGraphics(Graphics2D destination, int width, int height);

    public boolean offerBuffer(BufferedImage image);
    public void presentNewImage(BufferedImage image, Object paintResult);

    public BufferedImage getDrawingSurface(int bufferType);
}
