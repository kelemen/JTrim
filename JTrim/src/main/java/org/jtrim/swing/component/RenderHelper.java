/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.component;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;

/**
 *
 * @author Kelemen Attila
 */
final class RenderHelper {
    private RenderHelper() {
    }

    public static int drawOutlinedStringSimple(Graphics2D g, String str, int x, int y) {
        x++;

        Rectangle2D textArea = g.getFont().getStringBounds(str, g.getFontRenderContext());

        Color textColor = g.getColor();
        g.setColor(g.getBackground());

        g.drawString(str, x + 1, y + 1);
        g.drawString(str, x + 1, y - 1);
        g.drawString(str, x - 1, y + 1);
        g.drawString(str, x - 1, y - 1);

        g.drawString(str, x + 1, y);
        g.drawString(str, x - 1, y);
        g.drawString(str, x, y - 1);
        g.drawString(str, x, y + 1);

        g.setColor(textColor);
        g.drawString(str, x, y);

        return (int)textArea.getWidth() + 1;
    }

    public static void drawOutlinedString(Graphics2D g, String str, int x, int y, int extraSpace) {
        int currentX = x;

        for (int i = 0; i < str.length(); i++) {
            currentX += drawOutlinedStringSimple(g, str.substring(i, i + 1), currentX, y);
            currentX += extraSpace;
        }
    }

    public static void drawMessage(Graphics2D g, String message) {
        final int gap = 5;

        Rectangle2D textArea = g.getFont().getStringBounds(message, g.getFontRenderContext());

        int x = gap - (int)textArea.getMinX();
        int y = gap - (int)textArea.getMinY();

        StringTokenizer messageTokenizer = new StringTokenizer(message, "\n");

        while (messageTokenizer.hasMoreTokens()) {
            String currentLine = messageTokenizer.nextToken();
            drawOutlinedString(g, currentLine, x, y, 0);

            y = y + (int)textArea.getHeight();
        }
    }
}
