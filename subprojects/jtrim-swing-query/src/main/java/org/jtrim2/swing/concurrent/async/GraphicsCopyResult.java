package org.jtrim2.swing.concurrent.async;

/**
 * Defines the result of a copying to a {@code Graphics} object. This object
 * is returned by the {@link DrawingConnector#copyMostRecentGraphics(Graphics2D, int, int)}
 * method.
 * <P>
 * The result of a copy is a user defined object and a {@code boolean}
 * determining if anything was copied to the {@code Graphics} object.
 * <P>
 * This class does not have a public constructor and can be instantiated by the
 * {@link #getInstance(boolean, Object) getInstance(boolean, ResultType)}
 * method.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently and also instances of this class are immutable assuming that
 * the associated user defined object is immutable as well.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the user defined object attached to the
 *   {@code GraphicsCopyResult}
 */
public final class GraphicsCopyResult<ResultType> {
    private static final GraphicsCopyResult<?> PAINTED_WITHOUT_RESULT = new GraphicsCopyResult<>(true, null);
    private static final GraphicsCopyResult<?> NOT_PAINTED_WITHOUT_RESULT = new GraphicsCopyResult<>(false, null);

    private final boolean painted;
    private final ResultType paintResult;

    /**
     * Returns an instance of {@code GraphicsCopyResult} based on the specified
     * arguments. This method may or may not return a new instance when called.
     *
     * @param <ResultType> the type of the user defined object
     * @param painted the {@code boolean} defining if a copy actually occurred
     *   or not. This value will be returned by the {@link #isPainted()} method
     *   of the returned object.
     * @param paintResult a user defined object associated with the copying.
     *   This argument is allowed to be {@code null}. This value will be
     *   returned by the {@link #getPaintResult()} method of the returned
     *   object.
     * @return an instance of {@code GraphicsCopyResult} based on the specified
     *   arguments. This method never returns {@code null}.
     */
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

    /**
     * Returns the user defined object associated with the copying.
     *
     * @return the user defined object associated with the copying. This method
     *   may return {@code null}.
     */
    public ResultType getPaintResult() {
        return paintResult;
    }

    /**
     * Returns {@code true} if anything was copied to the {@code Graphics}
     * object.
     *
     * @return {@code true} if anything was copied to the {@code Graphics}
     *   object, {@code false} otherwise
     */
    public boolean isPainted() {
        return painted;
    }
}
