package org.jtrim.property.swing;

import java.awt.Component;
import java.util.Collection;
import org.jtrim.collections.ArraysEx;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see AutoDisplayState#componentDisabler(Component[])
 *
 * @author Kelemen Attila
 */
final class ComponentDisabler implements BoolPropertyListener {
    private final Component[] components;

    public ComponentDisabler(Component... components) {
        this.components = components.clone();
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    public Collection<Component> getComponents() {
        return ArraysEx.viewAsList(components);
    }

    @Override
    public void onChangeValue(boolean newValue) {
        for (Component component: components) {
            component.setEnabled(newValue);
        }
    }
}
