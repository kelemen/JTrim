package org.jtrim2.swing.component;

import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines the result of a rendering done by an {@link ImageRenderer}. The
 * {@code ImageRender} must specify if it did a significant, insignificant or
 * no rendering at all and may also attach a user defined object to the
 * rendering it did.
 * <P>
 * To create instances of this class, you should use one of the factory methods
 * if possible: {@link #noRendering() noRendering()},
 * {@link #insignificant(Object) insignificant(ResultType)} and
 * {@link #significant(Object) significant(ResultType)}.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently. Also instances of {@code RenderingResult} are immutable
 * assuming that the {@link #getResult() attached object} is also immutable.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the
 *   {@link #getResult() object attached to the rendering}
 *
 * @see ImageRenderer
 * @see AsyncRenderingComponent
 *
 * @author Kelemen Attila
 */
public final class RenderingResult<ResultType> {
    private static final RenderingResult<?> NO_RENDERING
            = new RenderingResult<>(RenderingType.NO_RENDERING, null);
    private static final RenderingResult<?> INSIGNIFICANT_RENDERING
            = new RenderingResult<>(RenderingType.INSIGNIFICANT_RENDERING, null);
    private static final RenderingResult<?> SIGNIFICANT_RENDERING
            = new RenderingResult<>(RenderingType.SIGNIFICANT_RENDERING, null);

    private final RenderingType type;
    private final ResultType result;

    /**
     * Returns a {@code RenderingResult} which has no
     * {@link #getResult() attached object} and whose {@link #getType() getType()}
     * method returns {@link RenderingType#NO_RENDERING}.
     * <P>
     * The returned result means that no rendering was done that should be
     * displayed.
     *
     * @param <ResultType> the type of the attached result. Although no object
     *   is attached to the returned result, this type argument might be needed
     *   for type safety.
     *
     * @return a {@code RenderingResult} which means that no rendering was done
     *   that should be displayed. This method never returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <ResultType> RenderingResult<ResultType> noRendering() {
        return (RenderingResult<ResultType>)NO_RENDERING;
    }

    /**
     * Returns a {@code RenderingResult} with the specified
     * {@link #getResult() attached object} and whose {@link #getType() getType()}
     * method returns {@link RenderingType#INSIGNIFICANT_RENDERING}.
     * <P>
     * The returned result means that the rendering done should be displayed
     * but is not significant. That is, insignificant renderings are renderings
     * which in itself does not contain the main interest of the user looking at
     * the rendered image.
     *
     * @param <ResultType> the type of the attached object
     * @param result the object attached to the rendering. This argument is
     *   allowed to be {@code null}.
     * @return a {@code RenderingResult} signaling that an insignificant
     *   rendering was done. This method never returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <ResultType> RenderingResult<ResultType> insignificant(ResultType result) {
        return result != null
                ? new RenderingResult<>(RenderingType.INSIGNIFICANT_RENDERING, result)
                : (RenderingResult<ResultType>)INSIGNIFICANT_RENDERING;
    }

    /**
     * Returns a {@code RenderingResult} with the specified
     * {@link #getResult() attached object} and whose {@link #getType() getType()}
     * method returns {@link RenderingType#SIGNIFICANT_RENDERING}.
     * <P>
     * The returned result means that the rendering done should be displayed
     * and is not significant. That is, significant renderings are renderings
     * containing the main interest of the user.
     *
     * @param <ResultType> the type of the attached object
     * @param result the object attached to the rendering. This argument is
     *   allowed to be {@code null}.
     * @return a {@code RenderingResult} signaling that a significant
     *   rendering was done. This method never returns {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <ResultType> RenderingResult<ResultType> significant(ResultType result) {
        return result != null
                ? new RenderingResult<>(RenderingType.SIGNIFICANT_RENDERING, result)
                : (RenderingResult<ResultType>)SIGNIFICANT_RENDERING;
    }

    /**
     * Creates a new {@code RenderingResult} with the given properties.
     * <P>
     * Consider using one of the factory methods instead of calling this
     * constructor:
     * <ul>
     *  <li>{@link #noRendering() noRendering()} </li>
     *  <li>{@link #insignificant(Object) insignificant(ResultType)}</li>
     *  <li> {@link #significant(Object) significant(ResultType)}</li>
     * </ul>
     *
     * @param type the {@code RenderingType} which is to be returned by the
     *   {@link #getType() getType()} method. This argument cannot be
     *   {@code null}.
     * @param result the object to be returned by the
     *   {@link #getResult() getResult()} method. This argument is allowed to be
     *   {@code null} but when {@code type} is {@link RenderingType#NO_RENDERING},
     *   this argument must be {@code null} because you may only return a user
     *   defined object if there was any rendering.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code RenderingType} is {@code null}
     * @throws IllegalArgumentException thrown if {@code result} is not
     *   {@code null} but the {@code type} is {@link RenderingType#NO_RENDERING}
     */
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

    /**
     * Returns if there was any rendering done and if there was, it was
     * significant or not.
     * <P>
     * If this method returns {@link RenderingType#NO_RENDERING},
     * {@link #getResult() getResult()} will always return {@code null}.
     *
     * @return the {@code RenderingType} determining if there was any rendering
     *   done and if there was, it was significant or not. This method never
     *   returns {@code null}.
     */
    public RenderingType getType() {
        return type;
    }

    /**
     * Returns the object attached to the rendering. The meaning of this object
     * depends on the renderer.
     *
     * @return the object attached to the rendering. This method may return
     *   {@code null}, if {@code null} was specified at construction time.
     */
    public ResultType getResult() {
        return result;
    }

    /**
     * Returns {@code true} if the renderer did some rendering.
     * <P>
     * This method call is equivalent to:
     * {@code getType() != RenderingType.NO_RENDERING}.
     *
     * @return {@code true} if the renderer did some rendering, {@code false}
     *   otherwise
     */
    public boolean hasRendered() {
        return type != RenderingType.NO_RENDERING;
    }

    /**
     * Returns {@code true} if the renderer did some rendering and it was
     * significant.
     * <P>
     * This method call is equivalent to:
     * {@code getType() == RenderingType.SIGNIFICANT_RENDERING}.
     *
     * @return {@code true} if the renderer did some rendering and it was
     *   significant, {@code false} otherwise
     */
    public boolean isSignificant() {
        return type == RenderingType.SIGNIFICANT_RENDERING;
    }
}
