package org.jtrim.swing.access;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JPanel;

/**
 * A {@link DecoratorPanelFactory} implementation creating invisible
 * {@code JPanel} instances blocking all user inputs if install as a glass pane
 * of a Swing component.
 * <P>
 * Note that this class is a singleton and its one and only instance can be
 * accessed by {@link InvisiblePanelFactory#INSTANCE}.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code createPanel} method of this class is not
 * <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public enum InvisiblePanelFactory implements DecoratorPanelFactory {
    /**
     * The one and only instance of {@code InvisiblePanelFactory}.
     */
    INSTANCE;

    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private static void registerConsumers(Component component) {
        component.addMouseListener(new MouseAdapter() { });
        component.addMouseMotionListener(new MouseMotionAdapter() { });
        component.addMouseWheelListener(new MouseAdapter() { });
        component.addKeyListener(new KeyAdapter() { });
    }

    /**
     * Creates and returns a {@code JPanel} which has a completely transparent
     * background and will block all user input from a component if installed
     * as its glass pane.
     *
     * @param decorated this argument is ignored by this method
     * @return a new {@code JPanel} which has a completely transparent
     *   background and will block all user input from a component if installed
     *   as its glass pane. This method never returns {@code null}.
     */
    @Override
    public JPanel createPanel(Component decorated) {
        JPanel result = new JPanel();
        registerConsumers(result);

        result.setOpaque(false);
        result.setBackground(TRANSPARENT_COLOR);
        return result;
    }
}
