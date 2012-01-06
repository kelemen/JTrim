/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.image.transform;

/**
 *
 * @author Kelemen Attila
 */
public class ImageProcessingException extends RuntimeException {
    private static final long serialVersionUID = 6999620545186048398L;

    /**
     * Creates a new instance of <code>ImageProcessingException</code> without detail message.
     */
    public ImageProcessingException() {
    }


    /**
     * Constructs an instance of <code>ImageProcessingException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ImageProcessingException(String msg) {
        super(msg);
    }

    public ImageProcessingException(Throwable cause) {
        super(cause);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
