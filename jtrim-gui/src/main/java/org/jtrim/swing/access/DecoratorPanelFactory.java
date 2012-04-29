package org.jtrim.swing.access;

import java.awt.Component;
import javax.swing.JPanel;
import org.jtrim.access.AccessManager;

/**
 *
 * @author Kelemen Attila
 */
public interface DecoratorPanelFactory {
    public JPanel createPanel(Component decorated, AccessManager<?, ?> accessManager);
}
