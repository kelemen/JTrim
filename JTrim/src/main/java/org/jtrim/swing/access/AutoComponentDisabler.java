/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import java.awt.Component;
import org.jtrim.access.*;

/**
 *
 * @author Kelemen Attila
 */
public enum AutoComponentDisabler implements AccessStateListener<SwingRight> {
    INSTANCE;

    @Override
    public void onEnterState(SwingRight right, AccessState state) {
        Component component = right.getComponent();
        if (component != null) {
            if (state != AccessState.AVAILABLE) {
                component.setEnabled(false);
            }
            else if (right.getSubRight() == null) {
                component.setEnabled(true);
            }
        }
    }
}
