package org.jtrim2.property.swing;

import java.awt.Component;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AutoDisplayState#componentDisabler(Component[])
 */
final class ComponentDisabler implements BoolPropertyListener {
    private final Component[] components;

    public ComponentDisabler(Component... components) {
        this.components = components.clone();
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    @Override
    public void onChangeValue(boolean newValue) {
        for (Component component: components) {
            component.setEnabled(newValue);
        }
    }
}
