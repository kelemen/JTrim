/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.event;

import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.image.*;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageListener {
    public void onChangeImage(AsyncDataLink<ImageData> imageLink);
    public void onReceiveMetaData(ImageMetaData metaData);
}
