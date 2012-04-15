/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.image.BufferedImage;

/**
 *
 * @author Kelemen Attila
 */
public interface ComponentRenderer {

    public int getRequiredDrawingSurfaceType(Object userDefRenderingParams, Object asyncData);

    public RenderingResult renderComponent(
            Object userDefRenderingParams,
            Object asyncData,
            BufferedImage drawingSurface);

    public void displayResult();
}
