package org.jtrim2.property.swing;

import javax.swing.AbstractButton;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AutoDisplayState#buttonCaptionSetter(AbstractButton, String, String)
 *
 * @author Kelemen Attila
 */
final class ButtonTextSwitcher implements BoolPropertyListener {
    private final String textWhenTrue;
    private final String textWhenFalse;
    private final AbstractButton button;

    public ButtonTextSwitcher(
            AbstractButton button,
            String textWhenTrue,
            String textWhenFalse) {

        ExceptionHelper.checkNotNullArgument(button, "button");
        ExceptionHelper.checkNotNullArgument(textWhenTrue, "textWhenTrue");
        ExceptionHelper.checkNotNullArgument(textWhenFalse, "textWhenFalse");

        this.button = button;
        this.textWhenTrue = textWhenTrue;
        this.textWhenFalse = textWhenFalse;
    }

    @Override
    public void onChangeValue(boolean newValue) {
        button.setText(newValue ? textWhenTrue : textWhenFalse);
    }
}
