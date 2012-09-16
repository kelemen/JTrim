/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;

import java.util.Set;
import org.jtrim.image.transform.ZoomToFitOption;

/**
 *
 * @author Kelemen Attila
 */
public interface TransformationListener {
    public void zoomChanged();
    public void offsetChanged();
    public void flipChanged();
    public void rotateChanged();

    public void enterZoomToFitMode(Set<ZoomToFitOption> options);
    public void leaveZoomToFitMode();
}
