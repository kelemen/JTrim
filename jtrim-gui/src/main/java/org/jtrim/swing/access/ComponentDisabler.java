package org.jtrim.swing.access;

import java.awt.Component;
import java.util.Collection;
import org.jtrim.access.AccessChangeAction;
import org.jtrim.access.AccessManager;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 *
 * @author Kelemen Attila
 */
public final class ComponentDisabler implements AccessChangeAction {
    private static final Component[] EMPTY_ARRAY = new Component[0];

    private final Component[] components;

    public ComponentDisabler(Component... components) {
        this.components = components.clone();
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    public ComponentDisabler(Collection<? extends Component> components) {
        this.components = components.toArray(EMPTY_ARRAY);
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    public Collection<Component> getComponents() {
        return ArraysEx.viewAsList(components);
    }

    @Override
    public void onChangeAccess(AccessManager<?, ?> accessManager, boolean available) {
        for (Component component: components) {
            component.setEnabled(available);
        }
    }
}
