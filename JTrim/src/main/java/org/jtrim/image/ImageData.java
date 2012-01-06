/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.*;
import org.jtrim.cache.*;
import org.jtrim.collections.*;

/**
 *
 * @author Kelemen Attila
 */
public final class ImageData implements MemoryHeavyObject {
    private final BufferedImage image;
    private final long approxSize;
    private final ImageMetaData metaData;
    private final ImageReceiveException exception;

    private static double getStoredPixelSize(ColorModel cm, SampleModel sm) {
        int dataType = sm.getDataType();
        int dataTypeSize = DataBuffer.getDataTypeSize(dataType);

        if (sm instanceof ComponentSampleModel) {
            sm.getNumDataElements();
            return sm.getNumDataElements() * dataTypeSize;
        }
        else if (sm instanceof SinglePixelPackedSampleModel) {
            return dataTypeSize;
        }
        else if (sm instanceof MultiPixelPackedSampleModel) {
            int pixelSize = cm.getPixelSize();
            // pixelSize must not be larger than dataTypeSize
            // according to the documentation.
            int pixelPerData = dataTypeSize / pixelSize;
            return (double)dataTypeSize / (double)pixelPerData;
        }
        else {
            // Though it may not be true, this is out best guess.
            return cm.getPixelSize();
        }
    }

    public static double getStoredPixelSize(ColorModel cm) {
        try {
            return getStoredPixelSize(cm, cm.createCompatibleSampleModel(1, 1));
        } catch (UnsupportedOperationException ex) {
            return cm.getPixelSize();
        }
    }

    public static long getApproxSize(BufferedImage image) {
        if (image != null) {
            double bitsPerPixel = getStoredPixelSize(
                    image.getColorModel(), image.getSampleModel());

            long pixelCount = (long)image.getWidth() * (long)image.getHeight();
            return (long)((1.0 / 8.0) * bitsPerPixel * (double)pixelCount);
        }
        else {
            return 0;
        }
    }

    public static int getCompatibleBufferType(ColorModel colorModel) {
        WritableRaster raster = colorModel.createCompatibleWritableRaster(1, 1);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        return image.getType();
    }

    public static BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height) {
        if (image == null) return null;

        ColorModel cm = image.getColorModel();
        WritableRaster wr;
        wr = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    public static BufferedImage cloneImage(BufferedImage image) {
        BufferedImage result;

        result = createCompatibleBuffer(image, image.getWidth(), image.getHeight());

        Graphics2D g = result.createGraphics();
        g.drawImage(image, null, 0, 0);
        g.dispose();

        return result;
    }

    public static BufferedImage createNewAcceleratedBuffer(BufferedImage image) {
        BufferedImage result = createAcceleratedBuffer(image);

        return result == image ? cloneImage(image) : result;
    }

    public static BufferedImage createAcceleratedBuffer(BufferedImage image) {
        if (image == null) return null;

        int width = image.getWidth();
        int height = image.getHeight();

        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();

        BufferedImage result;
        try {
            ColorModel srcColorModel = image.getColorModel();
            if (srcColorModel.getTransparency() != Transparency.OPAQUE
                    && !config.isTranslucencyCapable()) {
                return image;
            }

            // Note: For some reason it seems that translucent images
            //       are faster.
            //       Maybe because this way the renderer don't have to
            //       ignore the alpha byte?
            int transparency = java.awt.Transparency.TRANSLUCENT;
            ColorModel destColorModel = config.getColorModel(transparency);

            if (srcColorModel.equals(destColorModel)) {
                return image;
            }

            double srcSize = getStoredPixelSize(srcColorModel,
                    image.getSampleModel());
            double destSize = getStoredPixelSize(destColorModel);

            // We should allow a limit growth in size because
            // TYPE_3BYTE_BGR images are *very* slow.
            // So this check allows TYPE_3BYTE_BGR to be converted
            // to TYPE_INT_RGB or TYPE_INT_ARGB or TYPE_INT_ARGB_PRE.
            if (srcSize < 0.75 * destSize) {
                return image;
            }

            int srcMaxSize = ArraysEx.findMax(srcColorModel.getComponentSize());
            int destMaxSize = ArraysEx.findMax(destColorModel.getComponentSize());

            // In this case we would surely lose some precision
            // so to be safe we will return the same image.
            if (destMaxSize < srcMaxSize) {
                return image;
            }

            // The most likely cause is that the source image is a grayscale
            // image and the destination is an RGB buffer. In this case
            // we want to avoid converting the image.
            if (srcColorModel.getNumColorComponents()
                    != destColorModel.getNumColorComponents()) {

                return image;
            }

            result = config.createCompatibleImage(width, height, transparency);
        } catch (IllegalArgumentException|NullPointerException ex) {
            // IllegalArgumentException:
            // This exception may only happen if getComponentSize() returns
            // an empty array or createCompatibleImage cannot create an image
            // with the given transparency. These cases should not happen
            // in my oppinion.

            // NullPointerException:
            // May happen if getComponentSize() returns null but I do not
            // think that a well behaved implementation of ColorModel can
            // return null.

            result = null;
        }

        if (result == null) {
            return image;
        }

        Graphics2D g = result.createGraphics();
        g.drawImage(image, null, 0, 0);
        g.dispose();

        return result;
    }

    public static BufferedImage createNewOptimizedBuffer(BufferedImage image) {
        BufferedImage result = createOptimizedBuffer(image);

        return result == image ? cloneImage(image) : result;
    }

    public static BufferedImage createOptimizedBuffer(
            BufferedImage image) {

        if (image == null) return null;

        int width = image.getWidth();
        int height = image.getHeight();

        int newType;

        switch (image.getType()) {
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            {
                newType = BufferedImage.TYPE_INT_ARGB_PRE;
                break;
            }
            case BufferedImage.TYPE_4BYTE_ABGR:
            {
                newType = BufferedImage.TYPE_INT_ARGB;
                break;
            }
            case BufferedImage.TYPE_3BYTE_BGR:
            {
                newType = BufferedImage.TYPE_INT_RGB;
                break;
            }
            default:
                newType = image.getType();
                break;
        }

        if (image.getType() != newType) {
            BufferedImage result;
            result = new BufferedImage(width, height, newType);

            Graphics2D g = result.createGraphics();
            g.drawImage(image, null, 0, 0);
            g.dispose();

            return result;
        }
        else {
            return image;
        }
    }

    public ImageData(BufferedImage image, ImageMetaData metaData,
            ImageReceiveException exception) {

        this.image = image;
        this.approxSize = getApproxSize(image);
        this.metaData = metaData;
        this.exception = exception;
    }

    public ImageReceiveException getException() {
        return exception;
    }

    public BufferedImage getImage() {
        return image;
    }

    public ImageMetaData getMetaData() {
        return metaData;
    }

    public int getWidth() {
        return metaData != null ? metaData.getWidth() : -1;
    }

    public int getHeight() {
        return  metaData != null ? metaData.getHeight() : -1;
    }

    @Override
    public long getApproxMemorySize() {
        return approxSize;
    }
}
