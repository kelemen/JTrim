package org.jtrim2.testutils.image;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

public interface StaticImageDataMethods {
    public double getStoredPixelSize(ColorModel cm);
    public long getApproxSize(BufferedImage image);
    public int getCompatibleBufferType(ColorModel colorModel);
    public BufferedImage createCompatibleBuffer(BufferedImage image, int width, int height);
    public BufferedImage cloneImage(BufferedImage image);
    public BufferedImage createNewAcceleratedBuffer(BufferedImage image);
    public BufferedImage createAcceleratedBuffer(BufferedImage image);
    public BufferedImage createNewOptimizedBuffer(BufferedImage image);
    public BufferedImage createOptimizedBuffer(BufferedImage image);
}
