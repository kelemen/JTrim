/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image;

import javax.imageio.metadata.IIOMetadata;

/**
 *
 * @author Kelemen Attila
 */
public class JavaIIOMetaData extends ImageMetaData {
    private final IIOMetadata iioMetaData;

    public JavaIIOMetaData(int width, int height, IIOMetadata iioMetaData, boolean complete) {
        super(width, height, complete);
        this.iioMetaData = iioMetaData;
    }

    public IIOMetadata getIioMetaData() {
        return iioMetaData;
    }
}
