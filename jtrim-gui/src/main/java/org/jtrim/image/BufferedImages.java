package org.jtrim.image;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains static utility methods for create new {@link BufferedImage}s and
 * retrieving some properties of them.
 *
 * @author Kelemen Attila
 */
public final class BufferedImages {
    private static final double BITS_IN_BYTE = 8.0;
    private static final double ALLOWED_SIZE_DIFFERENCE_FOR_GROWTH = 0.75;

    /**
     * Checks if the given two {@code BufferedImage} instances have the same
     * dimension (width and height) and are of the same type
     * ({@code BufferedImage.getType()}). This method considers images with type
     * {@code BufferedImage.TYPE_CUSTOM} incompatible with other images with
     * type {@code BufferedImage.TYPE_CUSTOM}.
     *
     * @param image1 the image whose type and dimension must match
     *   {@code image2}. This argument cannot be {@code null}.
     * @param image2 the image whose type and dimension must match
     *   {@code image1}. This argument cannot be {@code null}.
     * @return {@code true} if the given two {@code BufferedImage} instances
     *   have the same dimension and are of the same type, {@code false}
     *   otherwise
     *
     * @throws NullPointerException thrown if any of the specified arguments is
     *   {@code null}
     */
    public static boolean areCompatibleBuffers(BufferedImage image1, BufferedImage image2) {
        ExceptionHelper.checkNotNullArgument(image1, "image1");
        ExceptionHelper.checkNotNullArgument(image2, "image2");

        if (image1.getWidth() != image2.getWidth()) {
            return false;
        }

        if (image1.getHeight() != image2.getHeight()) {
            return false;
        }

        int type1 = image1.getType();
        int type2 = image2.getType();

        if (type1 != type2) {
            return false;
        }

        if (type1 == BufferedImage.TYPE_CUSTOM) {
            return false;
        }

        // Notice that type2 cannot be BufferedImage.TYPE_CUSTOM because
        // if type1 was custom as well, the previous if would be true, if type1
        // is not custom, then type1 != type2.

        return true;
    }

    private static double getStoredPixelSizeInBits(ColorModel cm, SampleModel sm) {
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

    private static double getStoredPixelSizeInBits(ColorModel cm) {
        try {
            return getStoredPixelSizeInBits(cm, cm.createCompatibleSampleModel(1, 1));
        } catch (UnsupportedOperationException ex) {
            return cm.getPixelSize();
        }
    }

    /**
     * Returns the average size in bytes of a single pixel in the given
     * {@code ColorModel}. The returned value might not be an integer because
     * some {@code ColorModel} encodes pixels in a fraction of a byte (or just
     * not in a multiple of eight bits).
     * <P>
     * Note that for some obscure {@code ColorModel} this method may return an
     * incorrect value but tries to return a good approximate in this cases as
     * well.
     *
     * @param cm the {@code ColorModel} of which pixel size in bytes is to be
     *   returned. This argument cannot be {@code null}.
     * @return the average size in bytes of a single pixel in the given
     *   {@code ColorModel}. This method always return a value greater than or
     *   equal to zero. A return value is zero is usually only theoretical since
     *   there is no such practical color model.
     *
     * @throws NullPointerException thrown if the specified color model is
     *   {@code null}
     */
    public static double getStoredPixelSize(ColorModel cm) {
        return getStoredPixelSizeInBits(cm) / BITS_IN_BYTE;
    }

    /**
     * Returns the approximate memory in bytes the specified
     * {@code BufferedImage} retains. The size is approximate by the size of all
     * the pixels in the {@code BufferedImage}.
     *
     * @param image the {@code BufferedImage} whose size is to be approximated.
     *   This argument can be {@code null}, in which case zero is returned.
     * @return the approximate memory in bytes the specified
     *   {@code BufferedImage} retains. This method always returns a value
     *   greater than or equal to zero.
     */
    public static long getApproxSize(BufferedImage image) {
        if (image != null) {
            double bitsPerPixel = getStoredPixelSizeInBits(
                    image.getColorModel(), image.getSampleModel());

            long pixelCount = (long)image.getWidth() * (long)image.getHeight();
            return (long)((1.0 / BITS_IN_BYTE) * bitsPerPixel * (double)pixelCount);
        }
        else {
            return 0;
        }
    }

    /**
     * Returns the type of the {@code BufferedImage} which most closely
     * approximates the specified {@code ColorModel}.
     * <P>
     * The returned integer can be used as an image type for a
     * {@code BufferedImage}. That is, this integer can be specified for the
     * {@code BufferedImage} at construction time and will be returned by the
     * {@code BufferedImage.getType()} method. Note however, that this method
     * may return {@code BufferedImage.TYPE_CUSTOM} for which the constructor
     * of {@code BufferedImage} will throw an exception but any other return
     * value will be accepted by {@code BufferedImage}.
     *
     * @param colorModel the {@code ColorModel} which is to be approximated with
     *   a {@code BufferedImage} image type. This argument cannot be
     *   {@code null}.
     * @return the {@code BufferedImage} image type which most closely
     *   approximates the specified {@code ColorModel}.
     */
    public static int getCompatibleBufferType(ColorModel colorModel) {
        WritableRaster raster = colorModel.createCompatibleWritableRaster(1, 1);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        return image.getType();
    }

    /**
     * Creates a returns a new {@code BufferedImage} which is compatible with
     * the specified {@code BufferedImage} with the given dimensions. The
     * returned {@code BufferedImage} will have the same {@code ColorModel} as
     * the one specified. The returned {@code BufferedImage} is compatible in
     * the sense that its pixels can be copied using simple array copies and
     * does not require any other transformations.
     *
     * @param image the image to which a compatible {@code BufferedImage} is to
     *   be returned. This argument can be {@code null}, in which case
     *   {@code null} is returned.
     * @param width the width of the returned {@code BufferedImage}. This
     *   argument must be greater than zero.
     * @param height the height of the returned {@code BufferedImage}. This
     *   argument must be greater than zero.
     * @return a new {@code BufferedImage} which is compatible with the
     *   specified {@code BufferedImage} with the given dimensions. This method
     *   returns {@code null} only if the specified image was also {@code null}.
     */
    public static BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height) {
        if (image == null) return null;

        int type = image.getType();

        if (type == BufferedImage.TYPE_CUSTOM
                || type == BufferedImage.TYPE_BYTE_INDEXED) {
            ColorModel cm = image.getColorModel();
            WritableRaster wr;
            wr = cm.createCompatibleWritableRaster(width, height);
            return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        }
        else {
            return new BufferedImage(width, height, type);
        }
    }

    /**
     * Returns a new {@code BufferedImage} which is the copy of the specified
     * image. That is, returns an image which when modified does not affect the
     * specified image. The returned and the specified {@code BufferedImage}
     * have the same color model, dimensions and are otherwise compatible.
     *
     * @param image the image which is to be copied. This argument can be
     *   {@code null}, in which case {@code null} is returned.
     * @return a new {@code BufferedImage} which is the copy of the specified
     *   image. This method returns {@code null} only if the specified image was
     *   also {@code null}.
     */
    public static BufferedImage cloneImage(BufferedImage image) {
        if (image == null) {
            return null;
        }

        BufferedImage result;

        result = createCompatibleBuffer(image, image.getWidth(), image.getHeight());

        Graphics2D g = result.createGraphics();
        g.drawImage(image, null, 0, 0);
        g.dispose();

        return result;
    }

    /**
     * Returns a new image which have the same pixels as the specified image but
     * is likely to be accelerated by the currently present graphics card (if
     * there is any).
     * <P>
     * This methods differs from {@link #createAcceleratedBuffer(BufferedImage) createAcceleratedBuffer}
     * by always returning a new image which is independent from the argument.
     * That is, modifications of the returned image have no effect on the
     * image passed as an argument.
     *
     * @param image the base image to which an image is to be returned which
     *   is more likely to be accelerated by the currently present graphics
     *   card. If this method cannot do better, it will return a copy of this
     *   image. This argument can be {@code null}, in which case {@code null}
     *   is returned.
     * @return the image having the same pixels as the specified image but
     *   is likely to be accelerated by the currently present graphics card.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}. Modifying the returned image has no effect on the image
     *   passed in the argument.
     *
     * @see #createAcceleratedBuffer(BufferedImage)
     * @see #createNewOptimizedBuffer(BufferedImage)
     */
    public static BufferedImage createNewAcceleratedBuffer(BufferedImage image) {
        BufferedImage result = createAcceleratedBuffer(image);

        return result == image ? cloneImage(image) : result;
    }

    /**
     * Returns an image which have the same pixels as the specified image but
     * is likely to be accelerated by the currently present graphics card (if
     * there is any).
     * <P>
     * This method uses several undefined heuristics to determine the required
     * image type and may also return an image which requires somewhat more
     * memory to be stored.
     * <P>
     * This method may return the same {@code BufferedImage} image as the one
     * specified in the argument. If this is undesired, use the
     * {@link #createNewAcceleratedBuffer(BufferedImage) createNewAcceleratedBuffer}
     * method instead.
     *
     * @param image the base image to which an image is to be returned which
     *   is more likely to be accelerated by the currently present graphics
     *   card. This method may return this image if it cannot do better. This
     *   argument can be {@code null}, in which case {@code null} is returned.
     * @return the image having the same pixels as the specified image but
     *   is likely to be accelerated by the currently present graphics card.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createNewAcceleratedBuffer(BufferedImage)
     */
    public static BufferedImage createAcceleratedBuffer(BufferedImage image) {
        if (image == null) return null;
        if (GraphicsEnvironment.isHeadless()) {
            return createOptimizedBuffer(image);
        }

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

            double srcSize = getStoredPixelSizeInBits(srcColorModel,
                    image.getSampleModel());
            double destSize = getStoredPixelSizeInBits(destColorModel);

            // We should allow a limit growth in size because
            // TYPE_3BYTE_BGR images are *very* slow.
            // So this check allows TYPE_3BYTE_BGR to be converted
            // to TYPE_INT_RGB or TYPE_INT_ARGB or TYPE_INT_ARGB_PRE.
            if (srcSize < ALLOWED_SIZE_DIFFERENCE_FOR_GROWTH * destSize) {
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
        } catch (IllegalArgumentException | NullPointerException ex) {
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

    /**
     * Returns a new image which have the same pixels as the specified image but
     * is likely to be more efficiently handled than the one specified.
     * <P>
     * This methods differs from {@link #createOptimizedBuffer(BufferedImage) createOptimizedBuffer}
     * by always returning a new image which is independent from the argument.
     * That is, modifications of the returned image have no effect on the
     * image passed as an argument.
     *
     * @param image the base image to which an image is to be returned which
     *   is likely to be more efficiently handled in general. If this method
     *   cannot do better, it will return a copy of this image. This argument
     *   can be {@code null}, in which case {@code null} is returned.
     * @return the image which have the same pixels as the specified image but
     *   is likely to be more efficiently handled than the one specified.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createNewAcceleratedBuffer(BufferedImage)
     * @see #createOptimizedBuffer(BufferedImage)
     */
    public static BufferedImage createNewOptimizedBuffer(BufferedImage image) {
        BufferedImage result = createOptimizedBuffer(image);

        return result == image ? cloneImage(image) : result;
    }

    /**
     * Returns an image which have the same pixels as the specified image but
     * is likely to be more efficiently handled than the one specified.
     * <P>
     * Unlike {@link #createAcceleratedBuffer(BufferedImage) createAcceleratedBuffer},
     * this method does not consider graphic card acceleration. That is, this is
     * a general purpose method and only employs heuristics for converting the
     * image which are very likely to increase performance in general or at
     * least does not downgrade performance. This method may also return an
     * image which requires somewhat more memory to be stored.
     * <P>
     * This method may return the same {@code BufferedImage} image as the one
     * specified in the argument. If this is undesired, use the
     * {@link #createNewOptimizedBuffer(BufferedImage) createNewOptimizedBuffer}
     * method instead.
     *
     * @param image the base image to which an image is to be returned which
     *   is likely to be more efficiently handled in general. This method may
     *   return this image if it cannot do better. This argument can be
     *   {@code null}, in which case {@code null} is returned.
     * @return the image which have the same pixels as the specified image but
     *   is likely to be more efficiently handled than the one specified.
     *   This method returns {@code null} only if the specified image was also
     *   {@code null}.
     *
     * @see #createAcceleratedBuffer(BufferedImage)
     * @see #createNewOptimizedBuffer(BufferedImage)
     */
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

    private BufferedImages() {
        throw new AssertionError();
    }
}
