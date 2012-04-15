/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image;

import org.jtrim.concurrent.async.DataTransferException;

/**
 *
 * @author Kelemen Attila
 */
public class ImageReceiveException extends DataTransferException {
    private static final long serialVersionUID = -8807903975010525439L;

    /**
     * Creates a new instance of <code>ImageReceiveException</code> without detail message.
     */
    public ImageReceiveException() {
    }


    /**
     * Constructs an instance of <code>ImageReceiveException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ImageReceiveException(String msg) {
        super(msg);
    }

    public ImageReceiveException(Throwable cause) {
        super(cause);
    }

    public ImageReceiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
