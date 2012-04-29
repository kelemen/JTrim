package org.jtrim.swing.access;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JPanel;
import org.jtrim.access.AccessManager;

/**
 *
 * @author Kelemen Attila
 */
public enum InvisiblePanelFactory implements DecoratorPanelFactory {
    INSTANCE;

    private static Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private static void registerConsumers(Component component) {
        component.addMouseListener(new MouseAdapter() {});
        component.addMouseMotionListener(new MouseMotionAdapter() {});
        component.addMouseWheelListener(new MouseAdapter() {});
        component.addKeyListener(new KeyAdapter() {});
    }

    @Override
    public JPanel createPanel(Component decorated, AccessManager<?, ?> accessManager) {
        JPanel result = new JPanel();
        registerConsumers(result);

        result.setOpaque(false);
        result.setBackground(TRANSPARENT_COLOR);
        return result;
    }
}
