package org.jtrim2.property.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.concurrent.TimeUnit;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see AutoDisplayState#glassPaneSwitcher(JLayer, DelayedGlassPane)
 * @see AutoDisplayState#glassPaneSwitcher(JLayer, GlassPaneFactory)
 * @see AutoDisplayState#glassPaneSwitcher(RootPaneContainer, DelayedGlassPane)
 * @see AutoDisplayState#glassPaneSwitcher(RootPaneContainer, GlassPaneFactory)
 *
 * @author Kelemen Attila
 */
final class GlassPaneSwitcher implements BoolPropertyListener {
    private final Decorator decorator;

    public GlassPaneSwitcher(RootPaneContainer window, GlassPaneFactory glassPaneFactory) {
        this(new WindowWrapper(window), new DelayedGlassPane(glassPaneFactory, 0, TimeUnit.MILLISECONDS));
    }

    public GlassPaneSwitcher(RootPaneContainer window, DelayedGlassPane glassPanes) {
        this(new WindowWrapper(window), glassPanes);
    }

    public GlassPaneSwitcher(JLayer<?> component, GlassPaneFactory glassPaneFactory) {
        this(new JLayerWrapper(component), new DelayedGlassPane(glassPaneFactory, 0, TimeUnit.MILLISECONDS));
    }

    public GlassPaneSwitcher(JLayer<?> component, DelayedGlassPane glassPanes) {
        this(new JLayerWrapper(component), glassPanes);
    }

    private GlassPaneSwitcher(GlassPaneContainer container, DelayedGlassPane glassPanes) {
        this.decorator = new Decorator(container, glassPanes);
    }

    @Override
    public void onChangeValue(boolean newValue) {
        decorator.onChangeAccess(newValue);
    }

    private static boolean isFocused(Component component) {
        if (component == null) {
            return false;
        }
        if (component.isFocusOwner()) {
            return true;
        }
        if (component instanceof JLayer) {
            if (isFocused(((JLayer<?>)component).getView())) {
                return true;
            }
        }
        if (component instanceof Container) {
            Component[] subComponents;
            synchronized (component.getTreeLock()) {
                subComponents = ((Container)component).getComponents();
            }
            if (subComponents != null) {
                for (Component subComponent: subComponents) {
                    if (isFocused(subComponent)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static class Decorator {
        private final RestorableGlassPaneContainer component;
        private final DelayedGlassPane decorator;

        private ComponentState state;

        private javax.swing.Timer currentDecorateTimer;

        public Decorator(GlassPaneContainer component, DelayedGlassPane glassPanes) {
            ExceptionHelper.checkNotNullArgument(glassPanes, "glassPanes");

            this.component = new RestorableGlassPaneContainer(component);
            this.decorator = glassPanes;
            this.state = ComponentState.NOT_DECORDATED;
            this.currentDecorateTimer = null;
        }

        public void onChangeAccess(boolean available) {
            if (available) {
                stopCurrentDecorating();
            }
            else {
                if (state == ComponentState.NOT_DECORDATED) {
                    component.saveGlassPane();

                    int delayMillis = (int)Math.min(
                            decorator.getGlassPanePatience(TimeUnit.MILLISECONDS),
                            (long)Integer.MAX_VALUE);

                    if (delayMillis == 0) {
                        setDecoration();
                    }
                    else {
                        startDelayedDecoration(delayMillis);
                    }
                }
            }
        }

        private void setDecoration() {
            component.setGlassPane(decorator.getMainGlassPane().createGlassPane());
            state = ComponentState.DECORATED;
        }

        private void startDelayedDecoration(int delayMillis) {
            component.setGlassPane(decorator.getImmediateGlassPane().createGlassPane());
            state = ComponentState.WAIT_DECORATED;

            javax.swing.Timer timer = new javax.swing.Timer(delayMillis, (ActionEvent e) -> {
                if (currentDecorateTimer != e.getSource()) {
                    return;
                }

                currentDecorateTimer = null;
                if (state == ComponentState.WAIT_DECORATED) {
                    setDecoration();
                }
            });

            currentDecorateTimer = timer;
            timer.setRepeats(false);
            timer.start();
        }

        private void stopCurrentDecorating() {
            if (currentDecorateTimer != null) {
                currentDecorateTimer.stop();
                currentDecorateTimer = null;
            }
            removeDecoration();
        }

        private void removeDecoration() {
            component.restoreGlassPane();
            state = ComponentState.NOT_DECORDATED;
        }
    }

    private enum ComponentState {
        NOT_DECORDATED, WAIT_DECORATED, DECORATED
    }

    private static class RestorableGlassPaneContainer {
        private final GlassPaneContainer wrapped;

        private boolean hasSavedGlassPane;
        private Component savedGlassPane;
        private boolean savedGlassPaneVisible;

        public RestorableGlassPaneContainer(GlassPaneContainer wrapped) {
            this.wrapped = wrapped;
            this.hasSavedGlassPane = false;
            this.savedGlassPane = null;
            this.savedGlassPaneVisible = false;
        }

        public void saveGlassPane() {
            savedGlassPane = wrapped.getGlassPane();
            savedGlassPaneVisible = savedGlassPane != null
                    ? savedGlassPane.isVisible()
                    : false;
            hasSavedGlassPane = true;
        }

        public void restoreGlassPane() {
            if (hasSavedGlassPane) {
                wrapped.setGlassPane(savedGlassPane);
                if (savedGlassPane != null) {
                    savedGlassPane.setVisible(savedGlassPaneVisible);
                }

                savedGlassPane = null; // Allow it to be garbage collected
                hasSavedGlassPane = false;
            }
        }

        public void setGlassPane(Component glassPane) {
            wrapped.setGlassPane(glassPane);
            glassPane.setVisible(true);
            if (glassPane.isFocusable() && isFocused(wrapped.getComponent())) {
                glassPane.requestFocusInWindow();
            }
        }
    }

    private interface GlassPaneContainer {
        public Component getGlassPane();
        public void setGlassPane(Component glassPane);
        public Component getComponent();
    }

    private static class JLayerWrapper implements GlassPaneContainer {
        private final JLayer<?> component;

        public JLayerWrapper(JLayer<?> component) {
            ExceptionHelper.checkNotNullArgument(component, "component");
            this.component = component;
        }

        @Override
        public void setGlassPane(Component glassPane) {
            component.setGlassPane((JPanel)glassPane);
            component.revalidate();
        }

        @Override
        public Component getGlassPane() {
            return component.getGlassPane();
        }

        @Override
        public Component getComponent() {
            return component;
        }
    }

    private static class WindowWrapper implements GlassPaneContainer {
        private final RootPaneContainer asContainer;
        private final Component asComponent;

        public WindowWrapper(RootPaneContainer window) {
            ExceptionHelper.checkNotNullArgument(window, "window");
            this.asContainer = window;
            this.asComponent = (Component)window;
        }

        @Override
        public void setGlassPane(Component glassPane) {
            asContainer.setGlassPane(glassPane);
            asComponent.revalidate();
        }

        @Override
        public Component getGlassPane() {
            return asContainer.getGlassPane();
        }

        @Override
        public Component getComponent() {
            return asComponent;
        }
    }
}
