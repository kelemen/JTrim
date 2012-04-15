/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 *
 * @author Kelemen Attila
 */
public interface ImagePointTransformer {
    public void transformSrcToDest(Point2D src, Point2D dest);
    public void transformDestToSrc(Point2D dest, Point2D src) throws NoninvertibleTransformException;
}
