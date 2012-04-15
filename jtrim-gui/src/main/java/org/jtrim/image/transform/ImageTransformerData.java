/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import java.awt.image.BufferedImage;
import org.jtrim.image.ImageMetaData;

/**
 *
 * @author Kelemen Attila
 */
public final class ImageTransformerData {

    private final BufferedImage sourceImage;
    private final int destWidth;
    private final int destHeight;
    private final ImageMetaData metaData;

    public ImageTransformerData(BufferedImage sourceImage, int destWidth, int destHeight, ImageMetaData metaData) {
        this.sourceImage = sourceImage;
        this.destWidth = destWidth;
        this.destHeight = destHeight;
        this.metaData = metaData;
    }

    public int getImageWidth() {
        int result = 0;

        if (metaData != null) {
            result = metaData.getWidth();
        }

        if (result <= 0) {
            return sourceImage != null ? sourceImage.getWidth() : 0;
        }

        return result;
    }

    public int getImageHeight() {
        int result = 0;

        if (metaData != null) {
            result = metaData.getHeight();
        }

        if (result <= 0) {
            return sourceImage != null ? sourceImage.getHeight() : 0;
        }

        return result;
    }

    public int getSrcWidth() {
        return sourceImage != null ? sourceImage.getWidth() : 0;
    }

    public int getSrcHeight() {
        return sourceImage != null ? sourceImage.getHeight() : 0;
    }

    public int getDestHeight() {
        return destHeight;
    }

    public int getDestWidth() {
        return destWidth;
    }

    public ImageMetaData getMetaData() {
        return metaData;
    }

    public BufferedImage getSourceImage() {
        return sourceImage;
    }
}
