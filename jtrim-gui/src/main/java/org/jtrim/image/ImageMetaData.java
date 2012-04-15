/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image;

/**
 *
 * @author Kelemen Attila
 */
public class ImageMetaData {

    private final int width;
    private final int height;
    private final boolean complete;

    public ImageMetaData(int width, int height, boolean complete) {
        this.width = width;
        this.height = height;
        this.complete = complete;
    }

    public final boolean isComplete() {
        return complete;
    }

    public final int getHeight() {
        return height;
    }

    public final int getWidth() {
        return width;
    }
}
