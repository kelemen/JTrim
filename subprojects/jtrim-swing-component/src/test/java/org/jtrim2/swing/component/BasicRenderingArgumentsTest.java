package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class BasicRenderingArgumentsTest {
    private static Color newColor() {
        return new Color(20, 30, 40, 50);
    }

    private static Font newFont() {
        return new Font("Arial", Font.ITALIC, 10);
    }

    @Test
    public void testExplicit() {
        for (Color bckg: Arrays.asList(null, newColor())) {
            for (Color foreground: Arrays.asList(null, newColor())) {
                for (Font font: Arrays.asList(null, newFont())) {
                    BasicRenderingArguments args = new BasicRenderingArguments(bckg, foreground, font);
                    assertSame(bckg, args.getBackgroundColor());
                    assertSame(foreground, args.getForegroundColor());
                    assertSame(font, args.getFont());
                }
            }
        }
    }

    private static Component createDummyComponent(Color bckg, Color foreground, Font font) {
        return new Component() {
            private static final long serialVersionUID = 1L;

            @Override
            public Color getForeground() {
                return foreground;
            }

            @Override
            public Color getBackground() {
                return bckg;
            }

            @Override
            public Font getFont() {
                return font;
            }
        };
    }

    @Test
    public void testFromComponent() {
        for (Color bckg: Arrays.asList(null, newColor())) {
            for (Color foreground: Arrays.asList(null, newColor())) {
                for (Font font: Arrays.asList(null, newFont())) {
                    Component component = createDummyComponent(bckg, foreground, font);

                    BasicRenderingArguments args = new BasicRenderingArguments(component);
                    assertSame(bckg, args.getBackgroundColor());
                    assertSame(foreground, args.getForegroundColor());
                    assertSame(font, args.getFont());
                }
            }
        }
    }
}
