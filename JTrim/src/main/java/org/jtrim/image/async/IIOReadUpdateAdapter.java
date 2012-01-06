/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.async;

import java.awt.image.BufferedImage;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadUpdateListener;

/**
 *
 * @author Kelemen Attila
 */
public class IIOReadUpdateAdapter implements IIOReadUpdateListener {

    @Override
    public void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
    }

    @Override
    public void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
    }

    @Override
    public void passComplete(ImageReader source, BufferedImage theImage) {
    }

    @Override
    public void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
    }

    @Override
    public void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
    }

    @Override
    public void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail) {
    }

}
