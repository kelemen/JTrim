package org.jtrim.swing.component;

import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class RenderingResult<ResultType> {
    private static final RenderingResult<?> NO_RENDERING
            = new RenderingResult<>(RenderingType.NO_RENDERING, null);
    private static final RenderingResult<?> SIGNIFICANT_RENDERING
            = new RenderingResult<>(RenderingType.NO_RENDERING, null);

    private final RenderingType type;
    private final ResultType result;

    @SuppressWarnings("unchecked")
    public static <ResultType> RenderingResult<ResultType> noRendering() {
        return (RenderingResult<ResultType>)NO_RENDERING;
    }

    public static <ResultType> RenderingResult<ResultType> insignificant(ResultType result) {
        return new RenderingResult<>(RenderingType.INSIGNIFICANT_RENDERING, result);
    }

    @SuppressWarnings("unchecked")
    public static <ResultType> RenderingResult<ResultType> significant(ResultType result) {
        return result != null
                ? new RenderingResult<>(RenderingType.SIGNIFICANT_RENDERING, result)
                : (RenderingResult<ResultType>)SIGNIFICANT_RENDERING;
    }

    public RenderingResult(RenderingType type, ResultType result) {
        ExceptionHelper.checkNotNullArgument(type, "type");

        if (type == RenderingType.NO_RENDERING) {
            if (result != null) {
                throw new IllegalArgumentException("Result may not be attached to NO_RENDERING.");
            }
        }

        this.type = type;
        this.result = result;
    }

    public RenderingType getType() {
        return type;
    }

    public ResultType getResult() {
        return result;
    }

    public boolean hasRendered() {
        return type != RenderingType.NO_RENDERING;
    }

    public boolean isSignificant() {
        return type == RenderingType.SIGNIFICANT_RENDERING;
    }
}
