/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class RenderingResult {
    private static final RenderingResult SKIP_PAINT_RESULT = new RenderingResult(false, false, null);
    private static final RenderingResult DONE_PAINT_RESULT = new RenderingResult(false, false, null);
    private static final RenderingResult TERMINATE_PAINT_RESULT = new RenderingResult(true, true, null);

    private final boolean doPaint;
    private final boolean renderingFinished;
    private final Object paintResult;

    public static RenderingResult skipPaint() {
        return SKIP_PAINT_RESULT;
    }

    public static RenderingResult terminatePaint() {
        return TERMINATE_PAINT_RESULT;
    }

    public static RenderingResult terminatePaint(Object paintResult) {
        if (paintResult == null) {
            return TERMINATE_PAINT_RESULT;
        }
        else {
            return new RenderingResult(true, true, paintResult);
        }
    }

    public static RenderingResult done() {
        return DONE_PAINT_RESULT;
    }

    public static RenderingResult done(Object paintResult) {
        if (paintResult == null) {
            return DONE_PAINT_RESULT;
        }
        else {
            return new RenderingResult(true, false, paintResult);
        }
    }

    private RenderingResult(boolean doPaint, boolean renderingFinished, Object paintResult) {
        this.doPaint = doPaint;
        this.renderingFinished = renderingFinished;
        this.paintResult = paintResult;
    }

    public Object getPaintResult() {
        return paintResult;
    }

    public boolean needPaint() {
        return doPaint;
    }

    public boolean isRenderingFinished() {
        return renderingFinished;
    }
}
