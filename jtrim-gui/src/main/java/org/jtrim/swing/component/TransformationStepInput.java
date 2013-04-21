package org.jtrim.swing.component;

import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TransformationStepInput {
    private final ImageResult source;
    private final int destinationWidth;
    private final int destinationHeight;
    private final TransformedImage inputImage;

    public TransformationStepInput(
            ImageResult source,
            int destinationWidth,
            int destinationHeight,
            TransformedImage inputImage) {
        ExceptionHelper.checkArgumentInRange(destinationWidth, 0, Integer.MAX_VALUE, "destinationWidth");
        ExceptionHelper.checkArgumentInRange(destinationHeight, 0, Integer.MAX_VALUE, "destinationHeight");
        ExceptionHelper.checkNotNullArgument(inputImage, "inputImage");

        this.source = source;
        this.destinationWidth = destinationWidth;
        this.destinationHeight = destinationHeight;
        this.inputImage = inputImage;
    }

    /***/
    public TransformedImage getInputImage() {
        return inputImage;
    }

    /***/
    public ImageResult getSource() {
        return source;
    }

    /***/
    public int getDestinationWidth() {
        return destinationWidth;
    }

    /***/
    public int getDestinationHeight() {
        return destinationHeight;
    }
}
