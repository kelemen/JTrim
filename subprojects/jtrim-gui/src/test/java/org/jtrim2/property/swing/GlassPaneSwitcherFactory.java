package org.jtrim2.property.swing;

import javax.swing.JLayer;
import javax.swing.RootPaneContainer;
import org.jtrim2.property.BoolPropertyListener;

/**
 *
 * @author Kelemen Attila
 */
public interface GlassPaneSwitcherFactory {
    public BoolPropertyListener create(RootPaneContainer window, GlassPaneFactory glassPaneFactory);
    public BoolPropertyListener create(RootPaneContainer window, DelayedGlassPane glassPanes);
    public BoolPropertyListener create(JLayer<?> component, GlassPaneFactory glassPaneFactory);
    public BoolPropertyListener create(JLayer<?> component, DelayedGlassPane glassPanes);
}
