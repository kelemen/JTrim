package org.jtrim2.image.transform;

import java.awt.image.BufferedImage;
import java.util.Set;

public interface CommonZoomToFitTransformations {
    public BasicImageTransformations getBasicTransformations(
            int inputWidth,
            int inputHeight,
            int destWidth,
            int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase);

    public TransformedImage doTransform(
            BufferedImage srcImage,
            int destWidth,
            int destHeight,
            Set<ZoomToFitOption> options,
            BasicImageTransformations transBase,
            InterpolationType interpolationType);
}
