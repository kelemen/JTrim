/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class GraphicsCopyResult {
    public static final GraphicsCopyResult PAINTED_WITHOUT_RESULT = new GraphicsCopyResult(true, null);
    public static final GraphicsCopyResult NOT_PAINTED_WITHOUT_RESULT = new GraphicsCopyResult(false, null);

    private final boolean painted;
    private final Object paintResult;

    public static GraphicsCopyResult getInstance(boolean painted, Object paintResult) {
        if (paintResult == null) {
            return painted ? PAINTED_WITHOUT_RESULT : NOT_PAINTED_WITHOUT_RESULT;
        }
        else {
            return new GraphicsCopyResult(painted, paintResult);
        }
    }

    private GraphicsCopyResult(boolean painted, Object paintResult) {
        this.painted = painted;
        this.paintResult = paintResult;
    }

    public Object getPaintResult() {
        return paintResult;
    }

    public boolean isPainted() {
        return painted;
    }
}
