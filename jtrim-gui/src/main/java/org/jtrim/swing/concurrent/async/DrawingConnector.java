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
public interface DrawingConnector<ResultType> {
    public void setRequiredWidth(int width, int height);

    public boolean hasImage();
    public GraphicsCopyResult<ResultType> copyMostRecentGraphics(Graphics2D destination, int width, int height);

    public boolean offerBuffer(BufferedImage image);
    public void presentNewImage(BufferedImage image, ResultType paintResult);

    public BufferedImage getDrawingSurface(int bufferType);
}
