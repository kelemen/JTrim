package org.jtrim2.image.transform;

import java.util.Objects;
import org.jtrim2.image.ImageResult;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines the input of an {@link ImageTransformationStep}. That is, this is
 * an input for an image transformation step within a chain of image
 * transformations.
 * <P>
 * The input consists of both the output of the previous transformation step
 * and the original source image.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are allowed to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see ImageTransformationStep
 */
public final class TransformationStepInput {
    /**
     * Defines a quick comparison of two {@code TransformationStepInput}
     * instances. The comparison must be quick and should return {@code false},
     * if it cannot quickly determine the equality. That is, the comparison
     * should be a constant time operation which should little more than
     * comparing references.
     * <P>
     * Note that, the comparison can be relative to a particular
     * {@link ImageTransformationStep}. That is, it only needs to consider
     * if the associated {@code ImageTransformationStep} will yield the same
     * result for the two inputs or not.
     *
     * @see ImageTransformationStep
     * @see TransformationSteps#cachedStep(org.jtrim2.cache.ReferenceType, ImageTransformationStep, TransformationStepInput.Cmp) TransformationSteps.cachedStep
     */
    public interface Cmp {
        /**
         * Checks whether the two {@code TransformationStepInput} instances
         * should be considered the same for a particular
         * {@link ImageTransformationStep}.
         * <P>
         * The comparison should not do anything expensive. For example, it
         * should avoid checking the pixels of the input images. Rather, it
         * should compare the references of the needed input images. Also, if
         * the associated {@code ImageTransformationStep} ignores some property
         * of its input, this comparison should ignore them as well.
         * <P>
         * This method should be commutative. That is, return the same value
         * if its arguments are passed in the reverse order.
         *
         * @param input1 the first input object to be compared. This argument
         *   cannot be {@code null}.
         * @param input2 the second input object to be compared. This argument
         *   cannot be {@code null}.
         * @return {@code true} if the specified instances can be consider to
         *   be the same, {@code false} otherwise. This method may also return
         *   {@code false}, if it cannot quickly determine if the two inputs
         *   are equivalent or not.
         */
        public boolean isSameInput(TransformationStepInput input1, TransformationStepInput input2);
    }

    private final ImageResult source;
    private final int destinationWidth;
    private final int destinationHeight;
    private final TransformedImage inputImage;

    /**
     * Creates a new {@code TransformationStepInput} with the specified
     * properties.
     *
     * @param source the {@code ImageResult} to be returned by the
     *   {@link #getSource()} method. That is, the original source image of
     *   the transformation. This argument is allowed to be {@code null}, if the
     *   source image is not (yet) available.
     * @param destinationWidth the width of the area to where the image is
     *   to be displayed after the last applied transformation. This argument
     *   must be greater than or equal to zero.
     * @param destinationHeight the height of the area to where the image is
     *   to be displayed after the last applied transformation. This argument
     *   must be greater than or equal to zero.
     * @param inputImage the output of the previous image transformation step.
     *   This is the same as the source image for the first transformation to be
     *   applied. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the {@code inputImage} argument is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the {@code destinationWidth}
     *   or the {@code destinationHeight} is less than zero
     */
    public TransformationStepInput(
            ImageResult source,
            int destinationWidth,
            int destinationHeight,
            TransformedImage inputImage) {
        ExceptionHelper.checkArgumentInRange(destinationWidth, 0, Integer.MAX_VALUE, "destinationWidth");
        ExceptionHelper.checkArgumentInRange(destinationHeight, 0, Integer.MAX_VALUE, "destinationHeight");
        Objects.requireNonNull(inputImage, "inputImage");

        this.source = source;
        this.destinationWidth = destinationWidth;
        this.destinationHeight = destinationHeight;
        this.inputImage = inputImage;
    }

    /**
     * Returns the output of the previous transformation step which should
     * usually server as the input of the transformation applied after it.
     * <P>
     * Note the {@link TransformedImage#getPointTransformer() pointTransformer}
     * property of the returned {@code TransformedImage} is relative to the
     * {@link #getSource() original source image}.
     *
     * @return the output of the previous transformation step which should
     *   usually server as the input of the transformation applied after it.
     *   This method may never return {@code null}.
     */
    public TransformedImage getInputImage() {
        return inputImage;
    }

    /**
     * Returns the original source image of the whole image transformation
     * chain or {@code null} if the source image was not available.
     *
     * @return the original source image of the whole image transformation
     *   chain or {@code null} if the source image was not available
     */
    public ImageResult getSource() {
        return source;
    }

    /**
     * Returns the width of the final destination area of the whole image
     * transformation chain. For GUI components, this is the width of the
     * component.
     * <P>
     * <B>Warning</B>: This method may return zero, which is not accepted as a
     * width of a {@code BufferedImage}.
     *
     * @return the width of the final destination area of the whole image
     *   transformation chain. This method may never return a value less than
     *   zero.
     */
    public int getDestinationWidth() {
        return destinationWidth;
    }

    /**
     * Returns the height of the final destination area of the whole image
     * transformation chain. For GUI components, this is the height of the
     * component.
     * <P>
     * <B>Warning</B>: This method may return zero, which is not accepted as a
     * width of a {@code BufferedImage}.
     *
     * @return the height of the final destination area of the whole image
     *   transformation chain. This method may never return a value less than
     *   zero.
     */
    public int getDestinationHeight() {
        return destinationHeight;
    }
}
