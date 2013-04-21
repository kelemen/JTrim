
package org.jtrim.swing.component;

import java.awt.image.BufferedImage;
import org.jtrim.cache.GenericReference;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.image.ImageResult;
import org.jtrim.image.transform.TransformedImage;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class CachingImageTransformationStep implements ImageTransformationStep {
    private final ReferenceType cacheType;
    private final ImageTransformationStep.InputCmp cacheCmp;
    private final ImageTransformationStep wrapped;
    private StepCache cache;

    public CachingImageTransformationStep(
            ReferenceType cacheType,
            ImageTransformationStep wrapped,
            ImageTransformationStep.InputCmp cacheCmp) {
        ExceptionHelper.checkNotNullArgument(cacheType, "cacheType");
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        ExceptionHelper.checkNotNullArgument(cacheCmp, "cacheCmp");

        this.cacheCmp = cacheCmp;
        this.cacheType = cacheType;
        this.wrapped = wrapped;
        this.cache = null;
    }

    @Override
    public TransformedImage render(
            CancellationToken cancelToken,
            TransformationStepInput input,
            BufferedImage offeredBuffer) {

        if (cache != null) {
            TransformedImage output = cache.tryGetOutput(input, wrapped);
            if (output != null) {
                return output;
            }
        }
        TransformedImage output = wrapped.render(cancelToken, input, offeredBuffer);
        cache = new StepCache(cacheType, cacheCmp, input, output);
        return output;
    }

    private static final class StepCache {
        private final ImageTransformationStep.InputCmp inputCmp;
        private final VolatileReference<ImageResult> srcImageRef;
        private final int destinationWidth;
        private final int destinationHeight;
        private final VolatileReference<TransformedImage> inputImageRef;
        private final VolatileReference<TransformedImage> outputRef;

        public StepCache(
                ReferenceType cacheType,
                ImageTransformationStep.InputCmp inputCmp,
                TransformationStepInput info,
                TransformedImage output) {

            this.inputCmp = inputCmp;
            ImageResult source = info.getSource();
            this.srcImageRef = source != null
                    ? GenericReference.createReference(source, cacheType)
                    : null;
            this.destinationWidth = info.getDestinationWidth();
            this.destinationHeight = info.getDestinationHeight();

            TransformedImage inputImage = info.getInputImage();
            this.inputImageRef = inputImage != null
                    ? GenericReference.createReference(inputImage, cacheType)
                    : null;
            this.outputRef = GenericReference.createReference(output, cacheType);
        }

        public TransformedImage tryGetOutput(TransformationStepInput input, ImageTransformationStep step) {
            ImageResult cachedSourceImage;
            if (srcImageRef == null) {
                cachedSourceImage = null;
            }
            else {
                cachedSourceImage = srcImageRef.get();
                if (cachedSourceImage == null) {
                    return null;
                }
            }

            TransformedImage cachedInputImage;
            if (inputImageRef == null) {
                cachedInputImage = null;
            }
            else {
                cachedInputImage = inputImageRef.get();
                if (cachedInputImage == null) {
                    return null;
                }
            }

            TransformationStepInput cachedInput = new TransformationStepInput(
                    cachedSourceImage,
                    destinationWidth,
                    destinationHeight,
                    cachedInputImage);
            if (!inputCmp.isSameInput(input, cachedInput)) {
                return null;
            }

            return outputRef.get();
        }
    }
}
