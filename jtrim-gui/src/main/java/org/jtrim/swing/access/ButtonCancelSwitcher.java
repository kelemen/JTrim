/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.access;

import javax.swing.JButton;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessState;
import org.jtrim.access.AccessStateListener;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ButtonCancelSwitcher implements AccessStateListener<SwingRight> {
    private final String caption;
    private final String cancelCaption;
    private final JButton button;

    public ButtonCancelSwitcher(JButton button, String cancelCaption) {
        this(button, button.getText(), cancelCaption);
    }

    public ButtonCancelSwitcher(JButton button,
            String caption, String cancelCaption) {

        ExceptionHelper.checkNotNullArgument(button, "button");
        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(cancelCaption, "cancelCaption");

        this.button = button;
        this.caption = caption;
        this.cancelCaption = cancelCaption;
    }

    @Override
    public void onEnterState(AccessManager<?, SwingRight> accessManager,
            SwingRight right, AccessState state) {
        button.setText(state == AccessState.AVAILABLE
                ? caption
                : cancelCaption);
    }
}
