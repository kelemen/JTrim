package org.jtrim2.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import org.jtrim2.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public class DelegateGraphics extends Graphics {
    protected final Graphics wrapped;

    public DelegateGraphics(Graphics wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        this.wrapped = wrapped;
    }

    @Override
    public Graphics create() {
        return wrapped.create();
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        return wrapped.create(x, y, width, height);
    }

    @Override
    public void translate(int x, int y) {
        wrapped.translate(x, y);
    }

    @Override
    public Color getColor() {
        return wrapped.getColor();
    }

    @Override
    public void setColor(Color c) {
        wrapped.setColor(c);
    }

    @Override
    public void setPaintMode() {
        wrapped.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        wrapped.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return wrapped.getFont();
    }

    @Override
    public void setFont(Font font) {
        wrapped.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return wrapped.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return wrapped.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return wrapped.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        wrapped.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        wrapped.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return wrapped.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        wrapped.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        wrapped.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        wrapped.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        wrapped.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        wrapped.drawRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        wrapped.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        wrapped.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        wrapped.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        wrapped.draw3DRect(x, y, width, height, raised);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        wrapped.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        wrapped.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        wrapped.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        wrapped.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        wrapped.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p) {
        wrapped.drawPolygon(p);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        wrapped.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Polygon p) {
        wrapped.fillPolygon(p);
    }

    @Override
    public void drawString(String str, int x, int y) {
        wrapped.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        wrapped.drawString(iterator, x, y);
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        wrapped.drawChars(data, offset, length, x, y);
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        wrapped.drawBytes(data, offset, length, x, y);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height,
            Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        return wrapped.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        wrapped.dispose();
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    @Override
    @Deprecated
    public Rectangle getClipRect() {
        return wrapped.getClipRect();
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        return wrapped.hitClip(x, y, width, height);
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return wrapped.getClipBounds(r);
    }
}
