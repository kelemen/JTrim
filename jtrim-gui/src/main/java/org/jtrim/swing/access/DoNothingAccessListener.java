/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessState;
import org.jtrim.access.AccessStateListener;

/**
 *
 * @author Kelemen Attila
 */
enum DoNothingAccessListener implements AccessStateListener<SwingRight> {
    INSTANCE;

    @Override
    public void onEnterState(AccessManager<?, SwingRight> accessManager,
            SwingRight right, AccessState state) {
    }
}
