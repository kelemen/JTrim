package org.jtrim.swing.access;

import java.awt.Component;
import java.util.Collection;
import java.util.List;
import org.jtrim.collections.ArraysEx;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.swing.AutoDisplayState;

/**
 * @deprecated You should rely on
 * {@link org.jtrim.property.swing.AutoDisplayState} instead.
 * <P>
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
@Deprecated
public final class ComponentDisabler implements org.jtrim.access.AccessChangeAction {
    private static final Component[] EMPTY_ARRAY = new Component[0];

    private final BoolPropertyListener wrapped;
    private final List<Component> components;

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
        Component[] componentsCopy = components.clone();

        this.components = ArraysEx.viewAsList(componentsCopy);
        this.wrapped = AutoDisplayState.componentDisabler(componentsCopy);
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
        Component[] componentsArray = components.toArray(EMPTY_ARRAY);

        this.wrapped = AutoDisplayState.componentDisabler(componentsArray);
        this.components = ArraysEx.viewAsList(componentsArray.clone());
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
        return components;
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
        wrapped.onChangeValue(available);
    }
}
