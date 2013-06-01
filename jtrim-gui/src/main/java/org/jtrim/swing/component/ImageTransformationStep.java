package org.jtrim.swing.component;

import java.awt.image.BufferedImage;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.image.transform.TransformedImage;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageTransformationStep {
    /**
     */
    public TransformedImage render(
            CancellationToken cancelToken,
            TransformationStepInput input,
            BufferedImage offeredBuffer);
}
