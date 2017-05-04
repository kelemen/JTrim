package org.jtrim2.property.swing;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see SwingProperties#buttonSelected(AbstractButton)
 *
 * @author Kelemen Attila
 */
final class ButtonSelectedPropertySource implements SwingPropertySource<Boolean, ItemListener> {
    private final AbstractButton button;

    private ButtonSelectedPropertySource(AbstractButton button) {
        ExceptionHelper.checkNotNullArgument(button, "button");
        this.button = button;
    }

    public static MutableProperty<Boolean> createProperty(final AbstractButton button) {
        PropertySource<Boolean> source = SwingProperties.fromSwingSource(
                new ButtonSelectedPropertySource(button),
                ButtonSelectedPropertySource::createForwarder);

        return new AbstractMutableProperty<Boolean>(source) {
            @Override
            public void setValue(Boolean value) {
                button.setSelected(value);
            }
        };
    }

    @Override
    public Boolean getValue() {
        return button.isSelected();
    }

    @Override
    public void addChangeListener(ItemListener listener) {
        button.addItemListener(listener);
    }

    @Override
    public void removeChangeListener(ItemListener listener) {
        button.removeItemListener(listener);
    }

    private static ItemListener createForwarder(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        return (ItemEvent e) -> listener.run();
    }
}
