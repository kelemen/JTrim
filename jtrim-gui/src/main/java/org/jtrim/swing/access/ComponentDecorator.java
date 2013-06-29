package org.jtrim.swing.access;

import java.awt.Component;
import java.util.concurrent.TimeUnit;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import org.jtrim.access.AccessChangeAction;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.swing.AutoDisplayState;
import org.jtrim.property.swing.DelayedGlassPane;
import org.jtrim.property.swing.GlassPaneFactory;

/**
 * Defines an {@code AccessChangeAction} implementation which decorates a
 * Swing component if the associated group of right becomes unavailable. The
 * component is required to be a {@link JLayer JLayer} or top level window
 * having a {@link javax.swing.JRootPane root pane}.
 * <P>
 * The {@code ComponentDecorator} decorates the component using its glass pane.
 * That is, when the associated group of rights becomes unavailable, it will
 * replace the glass pane of the components with the one provided to the
 * {@code ComponentDecorator} at construction time (by a factory class).
 * <P>
 * When you expect that usually the group of right is only unavailable for a
 * very short period of time, it is possible to define a two kinds of
 * decorations {@code ComponentDecorator}. One to apply immediately after the
 * group of rights becomes unavailable and one after a specified time elapses
 * and the group of rights is still unavailable. This is useful to prevent
 * flickering if the group of rights becomes available within the specified
 * time (that is, if the glass pane set up immediately does not have a visual
 * effect).
 * <P>
 * Note that if the glass pane which is to be set by the
 * {@code ComponentDecorator} can have the focus (as defined by the method
 * {@link Component#isFocusable()}) and the component decorated by the
 * {@code ComponentDecorator} has the focus (or one of its subcomponents), the
 * focus will be moved to the newly set glass pane (if possible).
 *
 * <h3>Thread safety</h3>
 * The {@link #onChangeAccess(boolean) onChangeAccess} may only
 * be called from the AWT event dispatch thread. Therefore, the
 * {@link org.jtrim.access.AccessManager} governing the rights must be set to
 * use an executor which submits tasks to the AWT event dispatch thread (or wrap
 * the {@code ComponentDecorator} in an {@code AccessChangeAction} which makes
 * sure that the {@code onChangeAccess} method does not get called on an
 * inappropriate thread).
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>.
 *
 * @see org.jtrim.access.AccessAvailabilityNotifier
 *
 * @author Kelemen Attila
 */
public final class ComponentDecorator implements AccessChangeAction {
    private final BoolPropertyListener wrapped;

    /**
     * Creates a new {@code ComponentDecorator} decorating a window. The passed
     * component must inherit from (directly or indirectly)
     * {@link java.awt.Component Component}.
     * <P>
     * Using this constructor, whenever the checked group of rights becomes
     * unavailable, the glass pane for the specified window will be set to the
     * {@code JPanel} created by the {@code decorator} without any delay.
     *
     * @param window the window to be decorated. This argument cannot be
     *   {@code null} and must subclass {@link java.awt.Component Component}.
     * @param decorator the {@code DecoratorPanelFactory} which defines the
     *   panel to be used as a glass pane for the window. This argument cannot
     *   be {@code null}.
     *
     * @throws ClassCastException if the passed argument is not an instance of
     *   {@link java.awt.Component Component}
     * @throws NullPointerException if any of the passed argument is
     *   {@code null}
     */
    public ComponentDecorator(RootPaneContainer window, DecoratorPanelFactory decorator) {
        this(window, new DelayedDecorator(decorator, 0, TimeUnit.MILLISECONDS));
    }

    /**
     * Creates a new {@code ComponentDecorator} decorating a window. The passed
     * component must inherit from (directly or indirectly)
     * {@link java.awt.Component Component}.
     *
     * @param window the window to be decorated. This argument cannot be
     *   {@code null} and must subclass {@link java.awt.Component Component}.
     * @param decorator the {@code DelayedDecorator} which defines the panels
     *   to be used as a glass pane for the window. This argument cannot be
     *   {@code null}.
     *
     * @throws ClassCastException if the passed argument is not an instance of
     *   {@link java.awt.Component Component}
     * @throws NullPointerException if any of the passed argument is
     *   {@code null}
     */
    public ComponentDecorator(RootPaneContainer window, DelayedDecorator decorator) {
        this.wrapped = AutoDisplayState.glassPaneSwitcher(
                window, toDelayedGlassPane((Component)window, decorator));
    }

    /**
     * Creates a new {@code ComponentDecorator} decorating a specific component.
     * The passed {@link JLayer JLayer} must contain the component to be
     * decorated.
     * <P>
     * Using this constructor, whenever the checked group of rights becomes
     * unavailable, the glass pane for the specified {@code JLayer} will be
     * set to the {@code JPanel} created by the {@code decorator} without any
     * delay.
     *
     * @param component the component to be decorated. This argument cannot be
     *   {@code null}.
     * @param decorator the {@code DecoratorPanelFactory} which defines the
     *   panel to be used as a glass pane for the {@code JLayer} component. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException if any of the passed argument is
     *   {@code null}
     */
    public ComponentDecorator(JLayer<?> component, DecoratorPanelFactory decorator) {
        this(component, new DelayedDecorator(decorator, 0, TimeUnit.MILLISECONDS));
    }

    /**
     * Creates a new {@code ComponentDecorator} decorating a specific component.
     * The passed {@link JLayer JLayer} must contain the component to be
     * decorated.
     *
     * @param component the component to be decorated. This argument cannot be
     *   {@code null}.
     * @param decorator the {@code DelayedDecorator} which defines the panels
     *   to be used as a glass pane for the {@code JLayer} component. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException if any of the passed argument is
     *   {@code null}
     */
    public ComponentDecorator(JLayer<?> component, DelayedDecorator decorator) {
        this.wrapped = AutoDisplayState.glassPaneSwitcher(
                component, toDelayedGlassPane(component, decorator));
    }

    /**
     * Sets or restores the glass pane of the Swing component specified at
     * construction time as required by the availability of the associated group
     * of rights.
     *
     * @param available the {@code boolean} value defining if the glass pane of
     *   the Swing component specified at construction time must be set or
     *   restored
     */
    @Override
    public void onChangeAccess(boolean available) {
        wrapped.onChangeValue(available);
    }

    private static GlassPaneFactory toGlassPaneFactory(
            final Component component,
            final DecoratorPanelFactory factory) {
        return new GlassPaneFactory() {
            @Override
            public JPanel createGlassPane() {
                return factory.createPanel(component);
            }
        };
    }

    private static DelayedGlassPane toDelayedGlassPane(Component component, DelayedDecorator decorator) {
        return new DelayedGlassPane(
                toGlassPaneFactory(component, decorator.getImmediateDecorator()),
                toGlassPaneFactory(component, decorator.getMainDecorator()),
                decorator.getDecoratorPatience(TimeUnit.NANOSECONDS),
                TimeUnit.NANOSECONDS);
    }
}
