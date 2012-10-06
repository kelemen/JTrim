package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import org.jtrim.cache.MemoryHeavyObject;
import org.jtrim.image.ImageData;

/**
 *
 * @author Kelemen Attila
 */
public final class TransformedImage implements MemoryHeavyObject {
    private final BufferedImage image;
    private final long approxSize;
    private final ImagePointTransformer pointTransformer;

    public TransformedImage(BufferedImage image, ImagePointTransformer pointTransformer) {
        this.image = image;
        this.approxSize = ImageData.getApproxSize(image);
        this.pointTransformer = pointTransformer != null
                ? pointTransformer
                : IdentityImageTransformation.INSTANCE;
    }

    public BufferedImage getImage() {
        return image;
    }

    public ImagePointTransformer getPointTransformer() {
        return pointTransformer;
    }

    public void transformSrcToDest(Point2D src, Point2D dest) {
        pointTransformer.transformSrcToDest(src, dest);
    }

    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        pointTransformer.transformDestToSrc(dest, src);
    }

    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }

    private enum IdentityImageTransformation implements ImagePointTransformer {
        INSTANCE;

        @Override
        public void transformSrcToDest(Point2D src, Point2D dest) {
            dest.setLocation(src);
        }

        @Override
        public void transformDestToSrc(Point2D dest, Point2D src) {
            src.setLocation(dest);
        }
    }
}
