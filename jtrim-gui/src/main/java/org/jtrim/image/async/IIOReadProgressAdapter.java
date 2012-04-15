/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.async;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;

/**
 *
 * @author Kelemen Attila
 */
public class IIOReadProgressAdapter implements IIOReadProgressListener {

    @Override
    public void sequenceStarted(ImageReader source, int minIndex) {
    }

    @Override
    public void sequenceComplete(ImageReader source) {
    }

    @Override
    public void imageStarted(ImageReader source, int imageIndex) {
    }

    @Override
    public void imageProgress(ImageReader source, float percentageDone) {
    }

    @Override
    public void imageComplete(ImageReader source) {
    }

    @Override
    public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
    }

    @Override
    public void thumbnailProgress(ImageReader source, float percentageDone) {
    }

    @Override
    public void thumbnailComplete(ImageReader source) {
    }

    @Override
    public void readAborted(ImageReader source) {
    }

}
