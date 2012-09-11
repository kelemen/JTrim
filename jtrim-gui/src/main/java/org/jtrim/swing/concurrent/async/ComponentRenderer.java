/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.image.BufferedImage;

/**
 * @deprecated Used only by deprecated classes.
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface ComponentRenderer {

    public int getRequiredDrawingSurfaceType(Object userDefRenderingParams, Object asyncData);

    public AsyncRenderingResult renderComponent(
            Object userDefRenderingParams,
            Object asyncData,
            BufferedImage drawingSurface);

    public void displayResult();
}
