/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

/**
 *
 * @author Kelemen Attila
 */
public class DataTransferException extends Exception {
    private static final long serialVersionUID = 6330457357379838967L;

    public DataTransferException(Throwable cause) {
        super(cause);
    }

    public DataTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataTransferException(String message) {
        super(message);
    }

    public DataTransferException() {
    }
}
