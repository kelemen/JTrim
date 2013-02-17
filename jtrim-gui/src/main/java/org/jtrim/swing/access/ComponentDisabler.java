package org.jtrim.swing.access;

import java.awt.Component;
import java.util.Collection;
import org.jtrim.access.AccessChangeAction;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@link AccessChangeAction} which disables or enables the AWT
 * components specified at construction time according to the availability of
 * the associated group of rights.
 * <P>
 * Note that {@code ComponentDisabler} does call the {@code setEnabled} method
 * of the components in the {@link #onChangeAccess(boolean) onChangeAccess}
 * method, so the {@link org.jtrim.access.AccessManager} governing the rights
 * must be set to use an executor which submits tasks to the AWT event dispatch
 * thread (or wrap the {@code ComponentDisabler} in an
 * {@code AccessChangeAction} which makes sure that the {@code onChangeAccess}
 * method does not get called on an inappropriate thread).
 *
 * <h3>Thread safety</h3>
 * The {@link #onChangeAccess(boolean) onChangeAccess} may only
 * be called from the AWT event dispatch thread but other methods are safe to
 * be accessed from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class ComponentDisabler implements AccessChangeAction {
    private static final Component[] EMPTY_ARRAY = new Component[0];

    private final Component[] components;

    /**
     * Creates a new {@code ComponentDisabler} managing the enabled state of
     * the given AWT components.
     *
     * @param components the AWT components whose enabled state must be managed
     *   by this {@code ComponentDisabler}. This argument and its elements
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the components array or one of its
     *   elements is {@code null}
     */
    public ComponentDisabler(Component... components) {
        this.components = components.clone();
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    /**
     * Creates a new {@code ComponentDisabler} managing the enabled state of
     * the given AWT components.
     *
     * @param components the AWT components whose enabled state must be managed
     *   by this {@code ComponentDisabler}. This argument and its elements
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the components collection or one
     *   of its elements is {@code null}
     */
    public ComponentDisabler(Collection<? extends Component> components) {
        this.components = components.toArray(EMPTY_ARRAY);
        ExceptionHelper.checkNotNullElements(this.components, "components");
    }

    /**
     * Returns the AWT components whose enabled state are managed by this
     * {@code ComponentDisabler}. That is, the components specified at
     * construction time.
     *
     * @return the AWT components whose enabled state are managed by this
     *   {@code ComponentDisabler}. This method never returns {@code null} and
     *   the returned collection may not be modified.
     */
    public Collection<Component> getComponents() {
        return ArraysEx.viewAsList(components);
    }

    /**
     * Sets the enabled property of the AWT components specified at construction
     * time to the value of the {@code available} argument.
     *
     * @param available the value to which the enabled property of the AWT
     *   components is to be set
     */
    @Override
    public void onChangeAccess(boolean available) {
        for (Component component: components) {
            component.setEnabled(available);
        }
    }
}
