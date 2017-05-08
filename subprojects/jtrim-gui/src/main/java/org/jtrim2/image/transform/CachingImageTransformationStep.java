package org.jtrim2.image.transform;

import java.awt.image.BufferedImage;
import java.util.Objects;
import org.jtrim2.cache.GenericReference;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cache.VolatileReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.image.ImageResult;

final class CachingImageTransformationStep implements ImageTransformationStep {
    private final ReferenceType cacheType;
    private final TransformationStepInput.Cmp cacheCmp;
    private final ImageTransformationStep wrapped;
    private StepCache cache;

    public CachingImageTransformationStep(
            ReferenceType cacheType,
            ImageTransformationStep wrapped,
            TransformationStepInput.Cmp cacheCmp) {
        Objects.requireNonNull(cacheType, "cacheType");
        Objects.requireNonNull(wrapped, "wrapped");
        Objects.requireNonNull(cacheCmp, "cacheCmp");

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
        private final TransformationStepInput.Cmp inputCmp;
        private final VolatileReference<ImageResult> srcImageRef;
        private final int destinationWidth;
        private final int destinationHeight;
        private final VolatileReference<TransformedImage> inputImageRef;
        private final VolatileReference<TransformedImage> outputRef;

        public StepCache(
                ReferenceType cacheType,
                TransformationStepInput.Cmp inputCmp,
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
            this.inputImageRef = GenericReference.createReference(inputImage, cacheType);
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

            TransformedImage cachedInputImage = inputImageRef.get();
            if (cachedInputImage == null) {
                return null;
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
