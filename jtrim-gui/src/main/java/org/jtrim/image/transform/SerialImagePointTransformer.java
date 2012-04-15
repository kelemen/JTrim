/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.List;

/**
 *
 * @author Kelemen Attila
 */
public final class SerialImagePointTransformer implements ImagePointTransformer {
    private final ImagePointTransformer[] transformers;

    public SerialImagePointTransformer(ImagePointTransformer... transformers) {
        if (transformers != null) {
            this.transformers = transformers.clone();
        }
        else {
            this.transformers = null;
        }
    }

    public SerialImagePointTransformer(List<? extends ImagePointTransformer> transformers) {
        if (transformers != null) {
            this.transformers = new ImagePointTransformer[transformers.size()];
            int index = 0;
            for (ImagePointTransformer transformer: transformers) {
                this.transformers[index] = transformer;
                index++;
            }
        }
        else {
            this.transformers = null;
        }
    }

    @Override
    public void transformSrcToDest(Point2D src, Point2D dest) {
        if (transformers != null) {
            dest.setLocation(src);
            for (ImagePointTransformer transformer: transformers) {
                if (transformer != null) {
                    transformer.transformSrcToDest(dest, dest);
                }
            }
        }
        else {
            dest.setLocation(src);
        }
    }

    @Override
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        if (transformers != null) {
            src.setLocation(dest);
            for (int i = transformers.length - 1; i >= 0; i--) {
                ImagePointTransformer transformer = transformers[i];
                if (transformer != null) {
                    transformer.transformDestToSrc(src, src);
                }
            }
        }
        else {
            src.setLocation(dest);
        }
    }
}
