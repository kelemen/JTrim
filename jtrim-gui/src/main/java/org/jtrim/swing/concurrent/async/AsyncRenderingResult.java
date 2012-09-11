/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

/**
 * @deprecated  Used only by deprecated classes.
 *
 * @author Kelemen Attila
 */
@Deprecated
public final class AsyncRenderingResult {
    private static final AsyncRenderingResult SKIP_PAINT_RESULT = new AsyncRenderingResult(false, false, null);
    private static final AsyncRenderingResult DONE_PAINT_RESULT = new AsyncRenderingResult(false, false, null);
    private static final AsyncRenderingResult TERMINATE_PAINT_RESULT = new AsyncRenderingResult(true, true, null);

    private final boolean doPaint;
    private final boolean renderingFinished;
    private final Object paintResult;

    public static AsyncRenderingResult skipPaint() {
        return SKIP_PAINT_RESULT;
    }

    public static AsyncRenderingResult terminatePaint() {
        return TERMINATE_PAINT_RESULT;
    }

    public static AsyncRenderingResult terminatePaint(Object paintResult) {
        if (paintResult == null) {
            return TERMINATE_PAINT_RESULT;
        }
        else {
            return new AsyncRenderingResult(true, true, paintResult);
        }
    }

    public static AsyncRenderingResult done() {
        return DONE_PAINT_RESULT;
    }

    public static AsyncRenderingResult done(Object paintResult) {
        if (paintResult == null) {
            return DONE_PAINT_RESULT;
        }
        else {
            return new AsyncRenderingResult(true, false, paintResult);
        }
    }

    private AsyncRenderingResult(boolean doPaint, boolean renderingFinished, Object paintResult) {
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
