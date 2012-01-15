/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
        this.pointTransformer = pointTransformer;
    }

    public BufferedImage getImage() {
        return image;
    }

    public ImagePointTransformer getPointTransformer() {
        return pointTransformer;
    }

    public void transformSrcToDest(Point2D src, Point2D dest) {
        if (pointTransformer != null) {
            pointTransformer.transformSrcToDest(src, dest);
        }
        else {
            dest.setLocation(src);
        }
    }

    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        if (pointTransformer != null) {
            pointTransformer.transformDestToSrc(dest, src);
        }
        else {
            src.setLocation(dest);
        }
    }

    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }
}
