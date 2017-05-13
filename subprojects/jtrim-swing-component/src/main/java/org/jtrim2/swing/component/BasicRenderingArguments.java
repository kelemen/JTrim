package org.jtrim2.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 * Holds arguments usually required to render a <I>Swing</I> component. That
 * is, the background color, foreground color and the font to write texts.
 *
 * <h3>Thread safety</h3>
 * Instances of this class is immutable (assuming that {@code Color} and {@code Font}
 * instances are immutable) and as such are safe to be accessed from multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 */
public final class BasicRenderingArguments {
    private final Color backgroundColor;
    private final Color foregroundColor;
    private final Font font;

    /**
     * Initializes the {@code BasicRenderingArguments} with properties retrieved
     * from the passed {@code Component}.
     * <P>
     * That is, background color is retrieved by
     * {@code component.getBackground()}, foreground color is retrieved by
     * {@code component.getForeground()} and the font is retrieved by the
     * {@code component.getFont()} method call.
     *
     * @param component the {@code Component} from which the properties are to
     *   be retrieved. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified component is
     *   {@code null}
     */
    public BasicRenderingArguments(Component component) {
        this(component.getBackground(), component.getForeground(), component.getFont());
    }

    /**
     * Initializes the {@code BasicRenderingArguments} with the given
     * properties.
     *
     * @param backgroundColor the {@code Color} object to be returned by the
     *   {@link #getBackgroundColor() getBackgroundColor()} method call. This
     *   argument can be {@code null}.
     * @param foregroundColor the {@code Color} object to be returned by the
     *   {@link #getForegroundColor() getForegroundColor()} method call. This
     *   argument can be {@code null}.
     * @param font the {@code Font} object to be returned by the
     *   {@link #getFont() getFont()} method call. This argument can be
     *   {@code null}.
     */
    public BasicRenderingArguments(Color backgroundColor, Color foregroundColor, Font font) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.font = font;
    }

    /**
     * Returns the background color as specified at construction time.
     *
     * @return the background color as specified at construction time. This
     *   method may return {@code null}.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Returns the font as specified at construction time.
     *
     * @return the font as specified at construction time. This
     *   method may return {@code null}.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns the foreground color as specified at construction time.
     *
     * @return the foreground color as specified at construction time. This
     *   method may return {@code null}.
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }
}
