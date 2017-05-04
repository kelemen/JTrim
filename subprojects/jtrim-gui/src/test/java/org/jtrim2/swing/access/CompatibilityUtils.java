package org.jtrim2.swing.access;

import java.awt.Component;
import java.util.concurrent.TimeUnit;
import org.jtrim2.property.BoolPropertyListener;
import org.jtrim2.property.swing.DelayedGlassPane;
import org.jtrim2.property.swing.GlassPaneFactory;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("deprecation")
final class CompatibilityUtils {
    public static BoolPropertyListener toBoolPropertyListener(final org.jtrim2.access.AccessChangeAction listener) {
        return listener::onChangeAccess;
    }

    public static DecoratorPanelFactory toDecoratorFactory(final GlassPaneFactory factory) {
        return (Component decorated) -> factory.createGlassPane();
    }

    public static DelayedDecorator toDelayedDecorator(DelayedGlassPane glassPanes) {
        return new DelayedDecorator(
                toDecoratorFactory(glassPanes.getImmediateGlassPane()),
                toDecoratorFactory(glassPanes.getMainGlassPane()),
                glassPanes.getGlassPanePatience(TimeUnit.NANOSECONDS),
                TimeUnit.NANOSECONDS);
    }

    private CompatibilityUtils() {
        throw new AssertionError();
    }
}
