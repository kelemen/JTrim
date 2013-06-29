package org.jtrim.property.swing;

import javax.swing.AbstractButton;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.utils.ExceptionHelper;

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
