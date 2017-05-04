package org.jtrim2.image.transform;

import java.awt.geom.AffineTransform;

/**
 *
 * @author Kelemen Attila
 */
interface CommonAffineTransformations {
    public AffineTransform getTransformationMatrix(BasicImageTransformations transformations);

    public AffineTransform getTransformationMatrix(
            BasicImageTransformations transformations,
            double srcWidth, double srcHeight,
            double destWidth, double destHeight);

    public boolean isSimpleTransformation(BasicImageTransformations transformation);
}
