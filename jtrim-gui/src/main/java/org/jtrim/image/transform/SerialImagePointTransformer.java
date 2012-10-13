package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SerialImagePointTransformer implements ImagePointTransformer {
    private final ImagePointTransformer[] transformers;

    public SerialImagePointTransformer(ImagePointTransformer... transformers) {
        this.transformers = transformers.clone();
        ExceptionHelper.checkNotNullElements(this.transformers, "transformers");
    }

    public SerialImagePointTransformer(List<? extends ImagePointTransformer> transformers) {
        this.transformers = new ImagePointTransformer[transformers.size()];
        int index = 0;
        for (ImagePointTransformer transformer: transformers) {
            this.transformers[index] = transformer;
            index++;
        }

        ExceptionHelper.checkNotNullElements(this.transformers, "transformers");
        if (index != this.transformers.length) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public void transformSrcToDest(Point2D src, Point2D dest) {
        dest.setLocation(src);
        for (ImagePointTransformer transformer: transformers) {
            transformer.transformSrcToDest(dest, dest);
        }
    }

    @Override
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        src.setLocation(dest);
        for (int i = transformers.length - 1; i >= 0; i--) {
            transformers[i].transformDestToSrc(src, src);
        }
    }
}
