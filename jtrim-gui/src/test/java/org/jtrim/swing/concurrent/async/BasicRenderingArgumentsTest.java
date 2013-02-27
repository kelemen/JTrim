package org.jtrim.swing.concurrent.async;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class BasicRenderingArgumentsTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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

    @Test
    public void testFromComponent() {
        for (Color bckg: Arrays.asList(null, newColor())) {
            for (Color foreground: Arrays.asList(null, newColor())) {
                for (Font font: Arrays.asList(null, newFont())) {
                    Component component = mock(Component.class);
                    stub(component.getBackground()).toReturn(bckg);
                    stub(component.getForeground()).toReturn(foreground);
                    stub(component.getFont()).toReturn(font);

                    BasicRenderingArguments args = new BasicRenderingArguments(component);
                    assertSame(bckg, args.getBackgroundColor());
                    assertSame(foreground, args.getForegroundColor());
                    assertSame(font, args.getFont());

                    verify(component).getBackground();
                    verify(component).getForeground();
                    verify(component).getFont();
                    verifyNoMoreInteractions(component);
                }
            }
        }
    }
}
