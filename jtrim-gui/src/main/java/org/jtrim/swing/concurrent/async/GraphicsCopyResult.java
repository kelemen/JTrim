/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public final class GraphicsCopyResult<ResultType> {
    private static final GraphicsCopyResult<?> PAINTED_WITHOUT_RESULT = new GraphicsCopyResult<>(true, null);
    private static final GraphicsCopyResult<?> NOT_PAINTED_WITHOUT_RESULT = new GraphicsCopyResult<>(false, null);

    private final boolean painted;
    private final ResultType paintResult;

    @SuppressWarnings("unchecked")
    public static <ResultType> GraphicsCopyResult<ResultType> getInstance(boolean painted, ResultType paintResult) {
        if (paintResult == null) {
            return painted
                    ? (GraphicsCopyResult<ResultType>)PAINTED_WITHOUT_RESULT
                    : (GraphicsCopyResult<ResultType>)NOT_PAINTED_WITHOUT_RESULT;
        }
        else {
            return new GraphicsCopyResult<>(painted, paintResult);
        }
    }

    private GraphicsCopyResult(boolean painted, ResultType paintResult) {
        this.painted = painted;
        this.paintResult = paintResult;
    }

    public ResultType getPaintResult() {
        return paintResult;
    }

    public boolean isPainted() {
        return painted;
    }
}
