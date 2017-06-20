package org.jtrim2.property.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.BoolProperties;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines static utility methods to allow automatically changing the state of
 * Swing components (e.g.: between enabled and disabled).
 * <P>
 * The following examples automatically disables a button, if a given
 * {@code JTextField} is empty or a {@code JCheckBox} is not selected, otherwise
 * the button will be automatically enabled.
 * <PRE>
 * JTextField textField = ...;
 * JCheckBox checkBox = ...;
 * JButton button = ...;
 *
 * PropertySource&lt;Boolean&gt; condition = and(
 *         not(equalsWithConst(textProperty(textField), "")),
 *         buttonSelected(checkBox));
 * addSwingStateListener(condition, componentDisabler(button));
 * </PRE>
 * <P>
 * Assuming the following static imports:
 * <PRE>
 * import static org.jtrim2.property.swing.AutoDisplayState.*;
 * import static org.jtrim2.property.swing.SwingProperties.*;
 * import static org.jtrim2.property.BoolProperties.*;
 * </PRE>
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently. Note however that this is not true for the
 * {@code GlassPaneFactory} instances.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I> unless
 * otherwise noted. However, they are always safe to be called from the AWT
 * Event Dispatch Thread.
 */
public final class AutoDisplayState {
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    /**
     * Adds a {@code BoolPropertyListener} to be notified on the
     * <I>Event Dispatch Thread</I> whenever the specified property changes.
     * This method does not guarantee that the listener is notified only if the
     * specified property changes. That is, it is possible that the specified
     * listener is notified subsequently with the same argument. However, this
     * should be rare and client code should not need to worry about too much
     * about unnecessary property change notifications.
     * <P>
     * This method will cause the {@code BoolPropertyListener} to be notified
     * at least once, even if the underlying property does not change. If the
     * calling thread is the <I>Event Dispatch Thread</I>, then it may even be
     * called synchronously by this method.
     * <P>
     * See the {@link AutoDisplayState class documentation} for example usages.
     *
     * @param property the property whose value is to be checked. This argument
     *   cannot be {@code null}.
     * @param stateListener the listener to be notified whenever the value of
     *   the specified property changes. This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     *
     * @throws NullPointerException thrown if any of the argument is
     *   {@code null}
     */
    public static ListenerRef addSwingStateListener(
            final PropertySource<Boolean> property,
            final BoolPropertyListener stateListener) {

        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(stateListener, "stateListener");

        final UpdateTaskExecutor executor = SwingExecutors.getSwingUpdateExecutor(false);
        ListenerRef result = BoolProperties.addBoolPropertyListener(property, stateListener, executor);

        executor.execute(() -> {
            stateListener.onChangeValue(Boolean.TRUE.equals(property.getValue()));
        });

        return result;
    }

    private static void invokeAllListeners(BoolPropertyListener[] listeners, boolean arg) {
        Throwable toThrow = null;
        for (BoolPropertyListener listener: listeners) {
            try {
                listener.onChangeValue(arg);
            } catch (Throwable ex) {
                if (toThrow == null) toThrow = ex;
                else toThrow.addSuppressed(ex);
            }
        }

        ExceptionHelper.rethrowIfNotNull(toThrow);
    }

    /**
     * Adds multiple {@code BoolPropertyListener} to be notified on the
     * <I>Event Dispatch Thread</I> whenever the specified property changes.
     * This method does not guarantee that the listener is notified only if the
     * specified property changes. That is, it is possible that the specified
     * listener is notified subsequently with the same argument. However, this
     * should be rare and client code should not need to worry about too much
     * about unnecessary property change notifications.
     * <P>
     * This method will cause added {@code BoolPropertyListener} to be notified
     * at least once, even if the underlying property does not change. If the
     * calling thread is the <I>Event Dispatch Thread</I>, then it may even be
     * called synchronously by this method.
     * <P>
     * See the {@link AutoDisplayState class documentation} for example usages.
     *
     * @param property the property whose value is to be checked. This argument
     *   cannot be {@code null}.
     * @param listener1 the first listener to be notified whenever the value of
     *   the specified property changes. This argument cannot be {@code null}.
     * @param listener2 the first listener to be notified whenever the value of
     *   the specified property changes. This argument cannot be {@code null}.
     * @param others other listeners to be notified whenever the value of
     *   the specified property changes. This argument cannot be {@code null}
     *   and none of the listeners can be {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listeners, so that they will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     *
     * @throws NullPointerException thrown if any of the argument is
     *   {@code null}
     */
    public static ListenerRef addSwingStateListener(
            PropertySource<Boolean> property,
            BoolPropertyListener listener1,
            BoolPropertyListener listener2,
            BoolPropertyListener... others) {

        final BoolPropertyListener[] listeners = new BoolPropertyListener[others.length + 2];
        listeners[0] = listener1;
        listeners[1] = listener2;
        System.arraycopy(others, 0, listeners, 2, others.length);
        ExceptionHelper.checkNotNullElements(listeners, "listeners");

        BoolPropertyListener mergedListeners = (boolean newValue) -> {
            invokeAllListeners(listeners, newValue);
        };

        return addSwingStateListener(property, mergedListeners);
    }

    /**
     * Returns a {@code BoolPropertyListener} which enables the specified
     * component if it is called with {@code true}, disables them if it is
     * called with {@code false}.
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param components the components to be enabled or disabled based on the
     *   argument passed to the returned listener. This argument cannot be
     *   {@code null} and none of its elements can be {@code null}.
     * @return a {@code BoolPropertyListener} which enables the specified
     *   component if it is called with {@code true}, disables them if it is
     *   called with {@code false}. This method may never return {@code null}.
     *
     * @throws NullPointerException thrown if the component array or any of
     *   the specified components is {@code null}
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     */
    public static BoolPropertyListener componentDisabler(Component... components) {
        return new ComponentDisabler(components);
    }

    /**
     * Returns a {@code BoolPropertyListener} which sets the {@code text}
     * property of the specified Swing button based on the argument passed to
     * the listener.
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param button the button whose {@code text} property is to be set. This
     *   argument cannot be {@code null}.
     * @param textWhenTrue the value to be set for the {@code text} property of
     *   the specified button when the listener is called with {@code true}.
     *   This argument cannot be {@code null}.
     * @param textWhenFalse the value to be set for the {@code text} property of
     *   the specified button when the listener is called with {@code false}.
     *   This argument cannot be {@code null}.
     * @return a {@code BoolPropertyListener} which sets the {@code text}
     *   property of the specified Swing button based on the argument passed to
     *   the listener. This method may never return {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     */
    public static BoolPropertyListener buttonCaptionSetter(
            AbstractButton button,
            String textWhenTrue,
            String textWhenFalse) {
        return new ButtonTextSwitcher(button, textWhenTrue, textWhenFalse);
    }

    /**
     * Returns a {@code BoolPropertyListener} which sets (replaces) the glass
     * pane of the specified window if the listener is called with
     * {@code false}. The glass pane is restored when the listener is called
     * with {@code true}. If you want to avoid flickering, if the listener is
     * only notified {@code false} for a short period of time before it is
     * notified again with {@code true}, then you might want to use the other
     * {@link #glassPaneSwitcher(RootPaneContainer, DelayedGlassPane) glassPaneSwitcher}
     * method.
     * <P>
     * The intended use of this method is that the listener is notified with
     * {@code false} when the window is "busy", and {@code true} if it is "free".
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param window the window whose glass pane is to be set. This argument
     *   cannot be {@code null} and must implement {@link java.awt.Component}.
     * @param glassPaneFactory the factory creating glass pane for the specified
     *   window when the listener is called with {@code false}. This argument
     *   cannot be {@code null}.
     * @return a {@code BoolPropertyListener} which sets (replaces) the glass
     *   pane of the specified window if the listener is called with
     *   {@code false}. This method may never returns {@code null}.
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     * @see #glassPaneSwitcher(RootPaneContainer, DelayedGlassPane)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            RootPaneContainer window,
            GlassPaneFactory glassPaneFactory) {
        return new GlassPaneSwitcher(window, glassPaneFactory);
    }

    /**
     * Returns a {@code BoolPropertyListener} which sets (replaces) the glass
     * pane of the specified window if the listener is called with
     * {@code false}. The glass pane is restored when the listener is called
     * with {@code true}. If you want to avoid flickering, if the listener is
     * only notified {@code false} for a short period of time before it is
     * notified again with {@code true}, then you might want to use the other
     * {@link #glassPaneSwitcher(RootPaneContainer, DelayedGlassPane) glassPaneSwitcher}
     * method.
     * <P>
     * This method allows to specify a glass pane which is applied immediately
     * to the component and another which applied after a given timeout if the
     * listener has still not been notified with {@code true}. The benefit of
     * this feature is that you can avoid flickering if the {@code false} state
     * for the associated property lasts for only a very short period of time.
     * In this case, you may specify an {@link #invisibleGlassPane() invisible panel}
     * for the glass pane to be set immediately and only set the real (visible)
     * glass pane if this timeout elapses. Using the invisible panel, you may
     * block user input for even that short period of time.
     * <P>
     * The intended use of this method is that the listener is notified with
     * {@code false} when the window is "busy", and {@code true} if it is "free".
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param window the window whose glass pane is to be set. This argument
     *   cannot be {@code null} and must implement {@link java.awt.Component}.
     * @param glassPanes the glass panes to replace the glass pane of the
     *   given window as specified in the documentation of this method. This
     *   argument cannot be {@code null}.
     * @return a {@code BoolPropertyListener} which sets (replaces) the glass
     *   pane of the specified window if the listener is called with
     *   {@code false}. This method may never returns {@code null}.
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     * @see #glassPaneSwitcher(RootPaneContainer, GlassPaneFactory)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            RootPaneContainer window,
            DelayedGlassPane glassPanes) {
        return new GlassPaneSwitcher(window, glassPanes);
    }

    /**
     * Returns a {@code BoolPropertyListener} which sets (replaces) the glass
     * pane of the specified {@code JLayer} if the listener is called with
     * {@code false}. The glass pane is restored when the listener is called
     * with {@code true}. If you want to avoid flickering, if the listener is
     * only notified {@code false} for a short period of time before it is
     * notified again with {@code true}, then you might want to use the other
     * {@link #glassPaneSwitcher(RootPaneContainer, DelayedGlassPane) glassPaneSwitcher}
     * method.
     * <P>
     * The intended use of this method is that the listener is notified with
     * {@code false} when the component is "busy", and {@code true} if it is
     * "free".
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param component the {@code JLayer} whose glass pane is to be set. This
     *   argument cannot be {@code null}.
     * @param glassPaneFactory the factory creating glass pane for the specified
     *   {@code JLayer} when the listener is called with {@code false}. This
     *   argument cannot be {@code null}.
     * @return a {@code BoolPropertyListener} which sets (replaces) the glass
     *   pane of the specified {@code JLayer} if the listener is called with
     *   {@code false}. This method may never returns {@code null}.
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     * @see #glassPaneSwitcher(JLayer, DelayedGlassPane)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            JLayer<?> component,
            GlassPaneFactory glassPaneFactory) {
        return new GlassPaneSwitcher(component, glassPaneFactory);
    }

    /**
     * Returns a {@code BoolPropertyListener} which sets (replaces) the glass
     * pane of the specified {@code JLayer} if the listener is called with
     * {@code false}. The glass pane is restored when the listener is called
     * with {@code true}. If you want to avoid flickering, if the listener is
     * only notified {@code false} for a short period of time before it is
     * notified again with {@code true}, then you might want to use the other
     * {@link #glassPaneSwitcher(RootPaneContainer, DelayedGlassPane) glassPaneSwitcher}
     * method.
     * <P>
     * This method allows to specify a glass pane which is applied immediately
     * to the component and another which applied after a given timeout if the
     * listener has still not been notified with {@code true}. The benefit of
     * this feature is that you can avoid flickering if the {@code false} state
     * for the associated property lasts for only a very short period of time.
     * In this case, you may specify an {@link #invisibleGlassPane() invisible panel}
     * for the glass pane to be set immediately and only set the real (visible)
     * glass pane if this timeout elapses. Using the invisible panel, you may
     * block user input for even that short period of time.
     * <P>
     * The intended use of this method is that the listener is notified with
     * {@code false} when the component is "busy", and {@code true} if it is
     * "free".
     * <P>
     * Note that this listener must only be notified on the Event Dispatch
     * Thread. The intended use is to use this listener with the
     * {@link #addSwingStateListener(PropertySource, BoolPropertyListener) addSwingStateListener}
     * method.
     *
     * @param component the {@code JLayer} whose glass pane is to be set. This
     *   argument cannot be {@code null}.
     * @param glassPanes the glass panes to replace the glass pane of the
     *   given {@code JLayer} as specified in the documentation of this method.
     *   This argument cannot be {@code null}.
     * @return a {@code BoolPropertyListener} which sets (replaces) the glass
     *   pane of the specified {@code JLayer} if the listener is called with
     *   {@code false}. This method may never returns {@code null}.
     *
     * @see #addSwingStateListener(PropertySource, BoolPropertyListener)
     * @see #glassPaneSwitcher(JLayer, GlassPaneFactory)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            JLayer<?> component,
            DelayedGlassPane glassPanes) {
        return new GlassPaneSwitcher(component, glassPanes);
    }

    /**
     * Returns a {@code GlassPaneFactory} which creates invisible {@code JPanel}
     * instances blocking all user inputs if install as a glass pane of a Swing
     * component.
     * <P>
     * Note that if the focus is on a particular component, it will still
     * receive user input regardless what is installed for its glass pane.
     * Therefore, if you truly want to block user inputs, you should take away
     * the focus from the component. For example, by requesting the focus for
     * the glass pane.
     *
     * @return a {@code GlassPaneFactory} which creates invisible {@code JPanel}
     *   instances blocking all user inputs if install as a glass pane of a
     *   Swing component. This method may never return {@code null}.
     */
    public static GlassPaneFactory invisibleGlassPane() {
        return AutoDisplayState::createInvisibleGlassPane;
    }

    private static JPanel createInvisibleGlassPane() {
        JPanel result = new JPanel();
        registerConsumers(result);

        result.setOpaque(false);
        result.setBackground(TRANSPARENT_COLOR);
        return result;
    }

    private static void registerConsumers(Component component) {
        component.addMouseListener(new MouseAdapter() { });
        component.addMouseMotionListener(new MouseMotionAdapter() { });
        component.addMouseWheelListener(new MouseAdapter() { });
        component.addKeyListener(new KeyAdapter() { });
    }

    private AutoDisplayState() {
        throw new AssertionError();
    }
}
