/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class AffineImagePointTransformer implements ImagePointTransformer {
    private final AffineTransform srcToDest;

    public AffineImagePointTransformer(AffineTransform srcToDest) {
        ExceptionHelper.checkNotNullArgument(srcToDest, "srcToDest");

        this.srcToDest = new AffineTransform(srcToDest);
    }

    @Override
    public void transformSrcToDest(Point2D src, Point2D dest) {
        ExceptionHelper.checkNotNullArgument(src, "src");
        ExceptionHelper.checkNotNullArgument(dest, "dest");

        srcToDest.transform(src, dest);
    }

    @Override
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException {
        ExceptionHelper.checkNotNullArgument(src, "src");
        ExceptionHelper.checkNotNullArgument(dest, "dest");

        srcToDest.inverseTransform(dest, src);
    }


}
