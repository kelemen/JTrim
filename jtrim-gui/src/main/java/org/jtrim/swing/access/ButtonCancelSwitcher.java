package org.jtrim.swing.access;

import javax.swing.JButton;
import org.jtrim.access.AccessChangeAction;
import org.jtrim.access.AccessManager;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ButtonCancelSwitcher implements AccessChangeAction {
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
    public void onChangeAccess(AccessManager<?, ?> accessManager, boolean available) {
        button.setText(available ? caption : cancelCaption);
    }
}
