/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

import org.jtrim.concurrent.async.DataConverter;

/**
 *
 * @author Kelemen Attila
 */
public interface ImageTransformer
extends
        DataConverter<ImageTransformerData, TransformedImage> {
}
