/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.event;

import java.util.Set;
import org.jtrim.image.transform.ZoomToFitOption;

/**
 *
 * @author Kelemen Attila
 */
public interface TransformationListener {
    public void zoomChanged(double newZoomX, double newZoomY);
    public void offsetChanged(double newOffsetX, double newOffsetY);
    public void flipChanged(boolean hFlipped, boolean vFlipped);
    public void rotateChanged(double newRad);

    public void enterZoomToFitMode(Set<ZoomToFitOption> options);
    public void leaveZoomToFitMode();
}
