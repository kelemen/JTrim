/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 *
 * @author Kelemen Attila
 */
public class BasicRenderingArguments {
    private final Color backgroundColor;
    private final Color foregroundColor;
    private final Font font;

    public BasicRenderingArguments(Component component) {
        this(component.getBackground(), component.getForeground(), component.getFont());
    }

    public BasicRenderingArguments(Color backgroundColor, Color foregroundColor, Font font) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.font = font;
    }

    public final Color getBackgroundColor() {
        return backgroundColor;
    }

    public final Font getFont() {
        return font;
    }

    public final Color getForegroundColor() {
        return foregroundColor;
    }
}
