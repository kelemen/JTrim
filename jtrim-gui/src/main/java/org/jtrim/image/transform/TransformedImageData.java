/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import org.jtrim.cache.MemoryHeavyObject;
import org.jtrim.image.ImageReceiveException;

/**
 *
 * @author Kelemen Attila
 */
public final class TransformedImageData implements MemoryHeavyObject {
    private final TransformedImage transformedImage;
    private final ImageReceiveException exception;

    public TransformedImageData(TransformedImage transformedImage, ImageReceiveException exception) {
        this.transformedImage = transformedImage;
        this.exception = exception;
    }

    public ImageReceiveException getException() {
        return exception;
    }

    public TransformedImage getTransformedImage() {
        return transformedImage;
    }

    public ImagePointTransformer getPointTransformer() {
        return transformedImage != null ? transformedImage.getPointTransformer() : null;
    }

    public BufferedImage getImage() {
        return transformedImage != null ? transformedImage.getImage() : null;
    }

    @Override
    public long getApproxMemorySize() {
        if (transformedImage != null) {
            return transformedImage.getApproxMemorySize();
        }

        return 0;
    }
}
