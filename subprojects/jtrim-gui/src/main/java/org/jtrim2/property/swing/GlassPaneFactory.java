package org.jtrim2.property.swing;

import javax.swing.JPanel;

/**
 * Defines an interface creating {@code JPanel} instances as a glass pane for
 * Swing components (those that have glass pane).
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface can only be used from the AWT event
 * dispatch thread (unless they allow otherwise).
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I> but due to being used on the AWT event
 * dispatch thread they must avoid expensive actions.
 *
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.JLayer, GlassPaneFactory)
 * @see AutoDisplayState#glassPaneSwitcher(javax.swing.RootPaneContainer, GlassPaneFactory)
 *
 * @author Kelemen Attila
 */
public interface GlassPaneFactory {
    /**
     * Creates a new {@code JPanel} instance which is to be set as a glass
     * pane of a Swing component.
     *
     * @return the new {@code JPanel} instance which is to be set as a glass
     *   pane of a Swing component. This method never returns {@code null}.
     */
    public JPanel createGlassPane();
}
