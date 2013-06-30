package org.jtrim.property.swing;

import java.awt.Component;
import javax.swing.AbstractButton;
import javax.swing.JLayer;
import javax.swing.RootPaneContainer;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.BoolProperties;
import org.jtrim.property.BoolPropertyListener;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AutoDisplayState {
    /***/
    public static ListenerRef addSwingStateListener(
            final PropertySource<Boolean> property,
            final BoolPropertyListener stateListener) {

        ExceptionHelper.checkNotNullArgument(property, "property");
        ExceptionHelper.checkNotNullArgument(stateListener, "stateListener");

        final UpdateTaskExecutor executor = new SwingUpdateTaskExecutor(false);
        ListenerRef result = BoolProperties.addBoolPropertyListener(property, stateListener, executor);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                stateListener.onChangeValue(Boolean.TRUE.equals(property.getValue()));
            }
        });

        return result;
    }

    /***/
    public static BoolPropertyListener componentDisabler(Component... components) {
        return new ComponentDisabler(components);
    }

    /***/
    public static BoolPropertyListener buttonCaptionSetter(
            AbstractButton button,
            String textWhenTrue,
            String textWhenFalse) {
        return new ButtonTextSwitcher(button, textWhenTrue, textWhenFalse);
    }

    /**
     * @see AutoDisplayState#glassPaneSwitcher(RootPaneContainer, DelayedGlassPane)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            RootPaneContainer window,
            GlassPaneFactory glassPaneFactory) {
        return new GlassPaneSwitcher(window, glassPaneFactory);
    }

    /**
     *
     * @see AutoDisplayState#glassPaneSwitcher(RootPaneContainer, GlassPaneFactory)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            RootPaneContainer window,
            DelayedGlassPane glassPanes) {
        return new GlassPaneSwitcher(window, glassPanes);
    }

    /**
     *
     * @see AutoDisplayState#glassPaneSwitcher(JLayer, DelayedGlassPane)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            JLayer<?> component,
            GlassPaneFactory decorator) {
        return new GlassPaneSwitcher(component, decorator);
    }

    /**
     * @see AutoDisplayState#glassPaneSwitcher(JLayer, GlassPaneFactory)
     */
    public static BoolPropertyListener glassPaneSwitcher(
            JLayer<?> component,
            DelayedGlassPane glassPanes) {
        return new GlassPaneSwitcher(component, glassPanes);
    }

    /***/
    public static GlassPaneFactory invisibleGlassPane() {
        return InvisibleGlassPaneFactory.INSTANCE;
    }

    private AutoDisplayState() {
        throw new AssertionError();
    }
}
