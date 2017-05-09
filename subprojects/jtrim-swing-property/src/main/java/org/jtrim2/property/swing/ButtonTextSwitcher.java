package org.jtrim2.property.swing;

import java.util.Objects;
import javax.swing.AbstractButton;
import org.jtrim2.property.BoolPropertyListener;

/**
 * @see AutoDisplayState#buttonCaptionSetter(AbstractButton, String, String)
 */
final class ButtonTextSwitcher implements BoolPropertyListener {
    private final String textWhenTrue;
    private final String textWhenFalse;
    private final AbstractButton button;

    public ButtonTextSwitcher(
            AbstractButton button,
            String textWhenTrue,
            String textWhenFalse) {

        Objects.requireNonNull(button, "button");
        Objects.requireNonNull(textWhenTrue, "textWhenTrue");
        Objects.requireNonNull(textWhenFalse, "textWhenFalse");

        this.button = button;
        this.textWhenTrue = textWhenTrue;
        this.textWhenFalse = textWhenFalse;
    }

    @Override
    public void onChangeValue(boolean newValue) {
        button.setText(newValue ? textWhenTrue : textWhenFalse);
    }
}
